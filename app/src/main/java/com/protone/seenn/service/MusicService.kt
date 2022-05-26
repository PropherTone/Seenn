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
import androidx.lifecycle.MutableLiveData
import com.protone.api.context.*
import com.protone.api.toBitmapByteArray
import com.protone.database.room.entity.Music
import com.protone.database.sp.config.UserConfig
import com.protone.mediamodle.Medias
import com.protone.mediamodle.MusicState
import com.protone.mediamodle.media.IMusicPlayer
import com.protone.seenn.R
import com.protone.seenn.broadcast.*
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.system.exitProcess

class MusicService : Service(), MediaPlayer.OnCompletionListener, IMusicPlayer {

    companion object {
        @JvmStatic
        val MUSIC_NOTIFICATION_NAME = "MUSIC_NOTIFICATION"

        const val NOTIFICATION_ID = 0x01
    }

    private var remoteViews: RemoteViews? = null

    private var notificationManager: NotificationManager? = null

    private var notification: Notification? = null

    private val appReceiver = object : ApplicationBroadCast() {
        override fun finish() {
            notificationManager?.cancelAll()
            this@MusicService.stopSelf()
            exitProcess(0)
        }

        override fun music() {
//            Global.application.startActivity(SplashActivity::class.intent)
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
            this@MusicService.musicFinish()
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
                musicLists[musicPosition].title
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
                    musicLists[musicPosition].title
                )
                musicLists[musicPosition].uri.toBitmapByteArray()?.let { ba ->
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

    private var playPosition = MutableLiveData<Long>()
    private var playState = MutableLiveData<Boolean>()
    private val musicLists: MutableList<Music>
        get() = Medias.musicBucket[userConfig.playedMusicBucket] ?: Medias.music

    private var musicPlayer: MediaPlayer? = null
        get() {
            if (field == null) {
                field = MediaPlayer.create(
                    Global.application,
                    uri
                ).also {
                    it.setOnCompletionListener(this@MusicService)
                    it.setOnPreparedListener { mp ->
                        postData(
                            musicLists[musicPosition].title,
                            musicLists[musicPosition].duration,
                            uri,
                            mp.isPlaying
                        )
                    }
                }
            }
            return field
        }

    private var uri: Uri = Uri.EMPTY
        get() {
            if (musicLists.size > 0) musicLists[musicPosition].uri
            return if (musicLists.size > 0) musicLists[musicPosition].uri else field
        }

    private var musicTimer: Timer? = Timer()

    private val userConfig by lazy { UserConfig(this) }

    private var musicPosition = 0
        set(value) {
            userConfig.playedMusicPosition = value
            field = value
        }

    var musicPlayerMode = LIST_LOOPING

    override fun onCreate() {
        super.onCreate()
        musicBroadCastManager.registerReceiver(receiver, musicIntentFilter)
        registerReceiver(receiver, musicIntentFilter)
        registerReceiver(appReceiver, appIntentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(0x01, initMusicNotification())
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder {
        return MusicControlLer()
    }

    override fun onDestroy() {
        super.onDestroy()
        remoteViews = null
        notificationManager?.cancelAll()
        notificationManager = null
        musicFinish()
        unregisterReceiver(receiver)
        unregisterReceiver(appReceiver)
        musicBroadCastManager.unregisterReceiver(receiver)
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

    private fun restart() {
        musicFinish()
        play()
    }

    private fun musicFinish() {
        musicPlayer?.apply {
            stop()
            reset()
            release()
        }
        musicPlayer = null
    }

    override fun play() {
        if (musicLists.size <= 0) {
            postData(
                "NO MUSIC",
                0,
                Uri.EMPTY
            )
            return
        }
        if (musicTimer == null) musicTimer = Timer()
        musicPlayer?.apply {
            start()
            musicTimer?.schedule(timerTask {
                try {
                    if (isPlaying) playPosition.postValue(currentPosition.toLong())
                } catch (ignored: Exception) {
                }
            }, 0, 100)
        }
    }

    override fun play(music: Music, progress: Long) {
        if (musicTimer == null) musicTimer = Timer()
        musicPlayer = MediaPlayer.create(
            Global.application,
            music.uri
        ).apply {
            setOnCompletionListener(this@MusicService)
            setOnPreparedListener { mp ->
                onBackground {
                    postData(
                        music.title,
                        music.duration,
                        music.uri,
                        mp.isPlaying
                    )
                }
            }
            start()
            seekTo(progress.toInt())
            musicTimer?.schedule(timerTask {
                try {
                    if (isPlaying) playPosition.postValue(currentPosition.toLong())
                } catch (ignored: Exception) {
                }
            }, 0, 100)

        }
    }

    override fun pause() {
        if (musicPlayer?.isPlaying == false) return
        musicPlayer?.let {
            it.pause()
            postData(
                musicLists[musicPosition].title,
                musicLists[musicPosition].duration,
                uri,
                it.isPlaying
            )
        }
    }

    override fun next() {
        musicPosition =
            if (musicLists.size > 0) {
                if (musicPosition + 1 >= musicLists.size) 0 else ++musicPosition
            } else musicPosition
        restart()
    }

    override fun previous() {
        musicPosition =
            if (musicLists.size > 0) {
                if (musicPosition - 1 < 0) musicLists.size - 1 else --musicPosition
            } else musicPosition
        restart()
    }

    override fun seekTo(position: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            musicPlayer?.seekTo(
                position * musicLists[musicPosition].duration / 100,
                MediaPlayer.SEEK_CLOSEST
            )
        } else {
            musicPlayer?.seekTo((position * musicLists[musicPosition].duration).toInt())
        }
    }

    override fun getPlayState(): MutableLiveData<Boolean> = playState

    override fun getPlayPosition(): Int = musicPosition

    override fun getPosition() = playPosition

    override fun getMusicDetail() = musicLists[musicPosition]

    override fun onCompletion(p0: MediaPlayer?) {
        when (musicPlayerMode) {
            LIST_LOOPING -> {
                next()
            }
            LOOPING -> {
                restart()
            }
            RANDOM -> {
                musicPosition = (0 until musicLists.size).random()
                restart()
            }
        }
    }

    override fun getData() = if (musicLists.size > 0) MusicState(
        musicLists[musicPosition].title,
        musicLists[musicPosition].duration,
        0,
        musicLists[musicPosition].uri
    ) else MusicState("NO MUSIC", 0, 0, Uri.EMPTY)

    override fun setPlayMusicPosition(position: Int) {
        musicPosition = position - 1
        musicBroadCastManager.sendBroadcast(Intent(MUSIC_NEXT))
    }

    private fun postData(
        displayName: String,
        duration: Long,
        uri: Uri,
        isPlaying: Boolean = false
    ) {
        Medias.musicState.postValue(
            MusicState(
                displayName,
                duration,
                0,
                uri,
                isPlaying
            )
        )
    }

    inner class MusicControlLer : Binder(), IMusicPlayer {
        override fun setDate(list: MutableList<Music>) = this@MusicService.setDate(list)
        override fun play() = this@MusicService.play()
        override fun play(music: Music, progress: Long) = this@MusicService.play(music, progress)
        override fun pause() = this@MusicService.pause()
        override fun next() = this@MusicService.next()
        override fun previous() = this@MusicService.previous()
        override fun getPosition() = this@MusicService.getPosition()
        override fun getMusicDetail() = this@MusicService.getMusicDetail()
        override fun getData() = this@MusicService.getData()
        override fun setPlayMusicPosition(position: Int) =
            this@MusicService.setPlayMusicPosition(position)

        override fun seekTo(position: Long) = this@MusicService.seekTo(position)
        override fun getPlayState(): MutableLiveData<Boolean> = this@MusicService.getPlayState()
        override fun getPlayPosition(): Int = this@MusicService.getPlayPosition()
    }
}
