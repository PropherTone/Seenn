package com.protone.seenn.service

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
import com.protone.seen.customView.musicPlayer.BaseMusicPlayer.Companion.LOOP_LIST
import com.protone.seen.customView.musicPlayer.BaseMusicPlayer.Companion.LOOP_SINGLE
import com.protone.seen.customView.musicPlayer.BaseMusicPlayer.Companion.NO_LOOP
import com.protone.seen.customView.musicPlayer.BaseMusicPlayer.Companion.PLAY_LIST
import com.protone.seen.customView.musicPlayer.BaseMusicPlayer.Companion.RANDOM
import com.protone.seenn.R
import com.protone.seenn.broadcast.ApplicationBroadCast
import com.protone.seenn.broadcast.MusicReceiver
import com.protone.seenn.broadcast.musicBroadCastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.timerTask

/**
 * MusicService by ProTone 2022/03/23
 */
class MusicService : Service(), CoroutineScope by CoroutineScope(Dispatchers.IO), IMusicService,
    MediaPlayer.OnCompletionListener {

    companion object {
        const val MUSIC_NOTIFICATION_NAME = "MUSIC_NOTIFICATION"
        const val NOTIFICATION_ID = 0x01
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
                }
            }
            return field
        }
    private var progressTimer: Timer? = null

    private var playPosition = 0
    private val playList = mutableListOf<Music>()
    private val progress = MutableLiveData<Long>()
    private val playState = MutableLiveData<Boolean>()
    private val currentMusic = MutableLiveData<Music>()
    private val loopModeLive = MutableLiveData<Int>()

    private val appReceiver = object : ApplicationBroadCast() {
        override fun finish() {
            TODO("Not yet implemented")
        }

        override fun music() {
        }

    }

    private val receiver = object : MusicReceiver() {
        override fun play() {
            this@MusicService.play()
            notificationPlayState(true)
        }

        override fun pause() {
            this@MusicService.pause()
            notificationPlayState(false)
        }

        override fun finish() {
            this@MusicService.finishMusic()
        }

        override fun previous() {
            this@MusicService.previous()
            notificationText()
            notificationPlayState(true)
        }

        override fun next() {
            this@MusicService.next()
            notificationText()
            notificationPlayState(true)
        }

        private fun notificationText() {
            remoteViews?.setTextViewText(
                R.id.notify_music_name,
                playList[playPosition].title
            )
            notificationManager?.notify(NOTIFICATION_ID, notification)
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
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onCreate() {
        super.onCreate()
        musicBroadCastManager.registerReceiver(receiver, musicIntentFilter)
        registerReceiver(receiver, musicIntentFilter)
        registerReceiver(appReceiver, appIntentFilter)
    }

    override fun onBind(intent: Intent?): IBinder {
        if (playList.isNotEmpty())
            currentMusic.postValue(playList[playPosition])
        return MusicBinder()
    }

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
                this@MusicService,
                0,
                Intent(MUSIC_PLAY),
                intentFlags
            ).let { setOnClickPendingIntent(R.id.notify_music_control, it) }

            PendingIntent.getBroadcast(
                this@MusicService,
                0,
                Intent(MUSIC_PREVIOUS),
                intentFlags
            ).let { setOnClickPendingIntent(R.id.notify_music_previous, it) }

            PendingIntent.getBroadcast(
                this@MusicService,
                0,
                Intent(MUSIC_NEXT),
                intentFlags
            ).let { setOnClickPendingIntent(R.id.notify_music_next, it) }

            PendingIntent.getBroadcast(
                this@MusicService,
                0,
                Intent(MUSIC),
                intentFlags
            ).let { setOnClickPendingIntent(R.id.notify_music_parent, it) }

            PendingIntent.getBroadcast(
                this@MusicService,
                0,
                Intent(FINISH),
                intentFlags
            ).let { setOnClickPendingIntent(R.id.notify_music_close, it) }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MUSIC_NOTIFICATION_NAME,
                "SEEN_MUSIC",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(channel)
            Notification.Builder(this, MUSIC_NOTIFICATION_NAME).apply {
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
            notificationManager?.notify(NOTIFICATION_ID, it)
        }
    }

    override fun setLoopMode(mode: Int) {
        loopModeLive.postValue(mode)
    }

    override fun play(music: Music?) {
        if (playList.isEmpty()) {
            currentMusic.postValue(getEmptyMusic())
            return
        }
        launch {
            if (music != null) {
                if (!playList.contains(music)) {
                    playList.add(music)
                }
                val index = playList.indexOf(music)
                playPosition = index
                currentMusic.postValue(playList[playPosition])
            }
            musicPlayer?.apply {
                start()
                playState.postValue(true)
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
                    playState.postValue(false)
                    progressTimer?.cancel()
                    progressTimer = null
                }
        }
    }

    override fun next() {
        launch {
            if (++playPosition > playList.size - 1) playPosition = 0
            finishMusic()
            currentMusic.postValue(playList[playPosition])
            play()
        }
    }

    override fun previous() {
        launch {
            if (--playPosition <= 0) playPosition = playList.size - 1
            finishMusic()
            currentMusic.postValue(playList[playPosition])
            play()
        }
    }

    override fun getPlayList(): MutableList<Music> =
        playList.ifEmpty { mutableListOf(getEmptyMusic()) }

    override fun setPlayList(list: MutableList<Music>) {
        playList.clear()
        playList.addAll(list)
    }

    override fun onProgress(): LiveData<Long> = progress

    override fun onPlayState(): LiveData<Boolean> = playState

    override fun onLoopMode(): LiveData<Int> = loopModeLive

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

    override fun onMusicPlaying(): LiveData<Music> = currentMusic

    override fun init(music: Music, progress: Long) {
        currentMusic.postValue(
            if (playList.isNotEmpty()) {
                playPosition = playList.indexOf(music)
                playList[playPosition]
            } else getEmptyMusic()
        )
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
        override fun setLoopMode(mode: Int) = this@MusicService.setLoopMode(mode)
        override fun play(music: Music?) = this@MusicService.play(music)
        override fun pause() = this@MusicService.pause()
        override fun next() = this@MusicService.next()
        override fun previous() = this@MusicService.previous()
        override fun getPlayList(): MutableList<Music> = this@MusicService.getPlayList()
        override fun setPlayList(list: MutableList<Music>) = this@MusicService.setPlayList(list)
        override fun onProgress(): LiveData<Long> = this@MusicService.onProgress()
        override fun onPlayState(): LiveData<Boolean> = this@MusicService.onPlayState()
        override fun onLoopMode(): LiveData<Int> = this@MusicService.onLoopMode()
        override fun setProgress(progress: Long) = this@MusicService.setProgress(progress)
        override fun onMusicPlaying(): LiveData<Music> = this@MusicService.onMusicPlaying()
        override fun init(music: Music, progress: Long) = this@MusicService.init(music, progress)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        when (loopModeLive.value) {
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
            PLAY_LIST -> {
                if (playPosition <= playList.size - 1) next()
                else pause()
            }
        }
    }

    private fun getEmptyMusic() = Music(
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
    fun onLoopMode(): LiveData<Int>
    fun setProgress(progress: Long)
    fun onMusicPlaying(): LiveData<Music>
    fun init(music: Music, progress: Long)
}