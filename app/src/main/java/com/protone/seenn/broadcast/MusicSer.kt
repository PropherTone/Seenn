package com.protone.seenn.broadcast

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.protone.api.context.*
import com.protone.api.toBitmapByteArray
import com.protone.database.room.entity.Music
import com.protone.seenn.R
import com.protone.seenn.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.timerTask

/**
 * MusicService by ProTone 2022/03/23
 */
class MusicSer : Service(), CoroutineScope by CoroutineScope(Dispatchers.IO), IMusicService,
    MediaPlayer.OnCompletionListener {

    companion object {
        const val LOOP_SINGLE = 0
        const val LOOP_LIST = 1
        const val NO_LOOP = 3
        const val RANDOM = 4
    }

    private var notificationManager: NotificationManager? = null
    private var notification: Notification? = null
    private var remoteViews: RemoteViews? = null

    private var musicPlayer: MediaPlayer? = null
        get() {
            if (field == null) {
                field = MediaPlayer.create(
                    Global.application,
                    playList[playPosition].uri
                ).also {
                    it.setOnCompletionListener(this)
                    it.setOnPreparedListener {
                        currentMusic.postValue(playList[playPosition])
                    }
                }
            }
            return field
        }
    private var progressTimer: Timer? = null

    private var loopMode = LOOP_LIST
    private var playPosition = 0
    private val playList = mutableListOf<Music>()
    private val progress = MutableLiveData<Long>()
    private val playState = MutableLiveData<Boolean>()
    private val currentMusic = MutableLiveData<Music>()

    private val appReceiver = object : ApplicationBroadCast() {
        override fun finish() {
            TODO("Not yet implemented")
        }

        override fun music() {
            TODO("Not yet implemented")
        }

    }

    private val receiver = object : MusicReceiver() {
        override fun play() {
            this@MusicSer.play()
            notificationPlayState(true)
        }

        override fun pause() {
            this@MusicSer.pause()
            notificationPlayState(false)
        }

        override fun finish() {
            this@MusicSer.finishMusic()
        }

        override fun previous() {
            this@MusicSer.previous()
            notificationText()
            notificationPlayState(true)
        }

        override fun next() {
            this@MusicSer.next()
            notificationText()
            notificationPlayState(true)
        }

        private fun notificationText() {
            remoteViews?.setTextViewText(
                R.id.notify_music_name,
                playList[playPosition].title
            )
            notificationManager?.notify(MusicService.NOTIFICATION_ID, notification)
        }

        private fun notificationPlayState(state: Boolean) {
            playState.postValue(state)
            initMusicNotification()
            remoteViews?.setImageViewResource(
                R.id.notify_music_control,
                if (state) R.drawable.ic_baseline_pause_24
                else R.drawable.ic_baseline_play_arrow_24
            )
            if (state) {
                remoteViews?.setTextViewText(
                    R.id.notify_music_name,
                    playList[playPosition].title
                )
                playList[playPosition].uri.toBitmapByteArray()?.let { ba ->
                    remoteViews?.setImageViewBitmap(
                        R.id.notify_music_icon,
                        BitmapFactory.decodeByteArray(
                            ba,
                            0,
                            ba.size
                        )
                    )
                }
            }
            notificationManager?.notify(MusicService.NOTIFICATION_ID, notification)
        }
    }

    override fun onCreate() {
        super.onCreate()
        musicBroadCastManager.registerReceiver(receiver, musicIntentFilter)
        registerReceiver(receiver, musicIntentFilter)
        registerReceiver(appReceiver, appIntentFilter)
    }

    override fun onBind(intent: Intent?): IBinder = MusicBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(0x01, initMusicNotification())
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        finishMusic()
        unregisterReceiver(receiver)
        unregisterReceiver(appReceiver)
        musicBroadCastManager.unregisterReceiver(receiver)
        cancel()
    }

    @Suppress("DEPRECATION")
    private fun initMusicNotification(): Notification {
        remoteViews = RemoteViews(packageName, R.layout.music_notification_layout).apply {
            val intentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

            PendingIntent.getBroadcast(
                this@MusicSer,
                0,
                Intent(MUSIC_PLAY),
                intentFlags
            ).let { setOnClickPendingIntent(R.id.notify_music_control, it) }

            PendingIntent.getBroadcast(
                this@MusicSer,
                0,
                Intent(MUSIC_PREVIOUS),
                intentFlags
            ).let { setOnClickPendingIntent(R.id.notify_music_previous, it) }

            PendingIntent.getBroadcast(
                this@MusicSer,
                0,
                Intent(MUSIC_NEXT),
                intentFlags
            ).let { setOnClickPendingIntent(R.id.notify_music_next, it) }

            PendingIntent.getBroadcast(
                this@MusicSer,
                0,
                Intent(MUSIC),
                intentFlags
            ).let { setOnClickPendingIntent(R.id.notify_music_parent, it) }

            PendingIntent.getBroadcast(
                this@MusicSer,
                0,
                Intent(FINISH),
                intentFlags
            ).let { setOnClickPendingIntent(R.id.notify_music_close, it) }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MusicService.MUSIC_NOTIFICATION_NAME,
                "SEEN_MUSIC",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(channel)
            Notification.Builder(this, MusicService.MUSIC_NOTIFICATION_NAME).apply {
                setCustomContentView(remoteViews)
                setSmallIcon(R.drawable.ic_baseline_music_note_24)
            }.build()
        } else {
            Notification().apply {
                contentView = remoteViews
                icon = R.drawable.ic_baseline_music_note_24
            }
        }.also {
            it.flags = Notification.FLAG_NO_CLEAR
            notification = it
            notificationManager?.notify(MusicService.NOTIFICATION_ID, it)
        }
    }

    override fun setLoopMode(mode: Int) {
        loopMode = mode
    }

    override fun play(music: Music?) {
        if (playList.isEmpty()) {
            currentMusic.postValue(
                Music(
                    0,
                    "NO MUSIC",
                    0,
                    null,
                    null,
                    null,
                    "",
                    null,
                    null,
                    0L,
                    0L,
                    Uri.EMPTY,
                    mutableListOf()
                )
            )
            return
        }
        launch {
            val index = playList.indexOf(music)
            playPosition = index
            musicPlayer?.apply {
                start()
                if (progressTimer == null) progressTimer = Timer()
                progressTimer?.schedule(timerTask {
                    try {
                        if (isPlaying) progress.postValue(currentPosition.toLong())
                    } catch (ignored: Exception) {
                    }
                }, 0, 100)
            }
        }
    }

    override fun pause() {
        launch {
            if (musicPlayer?.isPlaying == true)
                musicPlayer?.apply {
                    pause()
                    progressTimer?.cancel()
                    progressTimer = null
                }
        }
    }

    override fun next() {
        launch {
            if (++playPosition > playList.size - 1) playPosition = 0
            finishMusic()
            play()
        }
    }

    override fun previous() {
        launch {
            if (--playPosition <= 0) playPosition = playList.size - 1
            finishMusic()
            play()
        }
    }

    override fun getPlayList(): MutableList<Music> = playList

    override fun setPlayList(list: MutableList<Music>) {
        playList.clear()
        playList.addAll(list)
    }

    override fun onProgress(): LiveData<Long> = progress

    override fun onPlayState(): LiveData<Boolean> = playState

    override fun setProgress(progress: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            musicPlayer?.seekTo(
                progress * playList[playPosition].duration / 100,
                MediaPlayer.SEEK_CLOSEST
            )
        } else {
            musicPlayer?.seekTo((progress * playList[playPosition].duration).toInt())
        }
    }

    private fun finishMusic() {
        musicPlayer?.apply {
            stop()
            reset()
            release()
        }
        musicPlayer = null
    }

    inner class MusicBinder : Binder(), IMusicService {
        override fun setLoopMode(mode: Int) = this@MusicSer.setLoopMode(mode)
        override fun play(music: Music?) = this@MusicSer.play(music)
        override fun pause() = this@MusicSer.pause()
        override fun next() = this@MusicSer.next()
        override fun previous() = this@MusicSer.previous()
        override fun getPlayList(): MutableList<Music> = this@MusicSer.getPlayList()
        override fun setPlayList(list: MutableList<Music>) = this@MusicSer.setPlayList(list)
        override fun onProgress(): LiveData<Long> = this@MusicSer.onProgress()
        override fun onPlayState(): LiveData<Boolean> = this@MusicSer.onPlayState()
        override fun setProgress(progress: Long) = this@MusicSer.setProgress(progress)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        when (loopMode) {
            LOOP_LIST -> next()
            LOOP_SINGLE -> {
                --playPosition
                next()
            }
            NO_LOOP -> pause()
            RANDOM -> {
                playPosition = (0 until playList.size - 1).random()
                finishMusic()
                play()
            }
        }
    }
}

interface IMusicService {
    fun setLoopMode(mode: Int)
    fun play(music: Music? = null)
    fun pause()
    fun next()
    fun previous()
    fun getPlayList(): MutableList<Music>
    fun setPlayList(list: MutableList<Music>)
    fun onProgress(): LiveData<Long>
    fun onPlayState(): LiveData<Boolean>
    fun setProgress(progress: Long)
}