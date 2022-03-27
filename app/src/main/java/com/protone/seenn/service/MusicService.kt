package com.protone.seenn.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.lifecycle.MutableLiveData
import com.protone.api.context.*
import com.protone.database.room.entity.Music
import com.protone.mediamodle.Galley
import com.protone.mediamodle.MusicState
import com.protone.mediamodle.media.*
import com.protone.seenn.R
import java.lang.Exception
import java.util.*
import kotlin.concurrent.timerTask

class MusicService() : Service(), MediaPlayer.OnCompletionListener {

    companion object {
        @JvmStatic
        val MUSIC_NOTIFICATION_NAME = "MUSIC_NOTIFICATION"

        private val TAG = this::class.simpleName
    }

    private var remoteViews: RemoteViews? = null

    private var notificationManager: NotificationManager? = null

    private val receiver = object : MusicReceiver() {

        override fun play() {
            this@MusicService.play()
        }

        override fun pause() {
            this@MusicService.pause()
        }

        override fun finish() {
            this@MusicService.musicFinish()
        }

        override fun previous() {
            this@MusicService.previous()
        }

        override fun next() {
            this@MusicService.next()
        }

    }

    private var playPosition = MutableLiveData<Long>()
    private var musicLists: MutableList<Music> = mutableListOf()
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
            Log.d(TAG, "bucket: ${musicLists[musicPosition]}")
            return if (musicLists.size > 0) musicLists[musicPosition].uri else field
        }

    private var musicTimer: Timer? = Timer()


    private var musicPosition = 0
    var musicPlayerMode = LIST_LOOPING

    override fun onCreate() {
        super.onCreate()
        musicBroadCastManager.registerReceiver(receiver, musicIntentFilter)
        registerReceiver(receiver, musicIntentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(0x01, initMusicNotification())
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder {
        return MusicControlLer()
    }

    override fun onDestroy() {
        super.onDestroy()
        remoteViews?.let {
            it.removeAllViews(it.layoutId)
        }
        remoteViews = null
        notificationManager?.cancelAll()
        notificationManager = null
        musicFinish()
        unregisterReceiver(receiver)
        musicBroadCastManager.unregisterReceiver(receiver)
    }

    private fun initMusicNotification(): Notification {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            notificationManager?.notify(0x01, it)
        }
    }

    private fun setDate(list: MutableList<Music>) {
        this.musicLists = list
    }

    private fun play() {
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
                } catch (ignored: Exception){}
            }, 0, 100)
        }
    }

    private fun pause() {
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
//        musicTimer?.cancel()
//        musicTimer = null
    }

    private fun next() {
        musicPosition =
            if (musicLists.size > 0) {
                if (musicPosition + 1 >= musicLists.size) 0 else ++musicPosition
            } else musicPosition
        restart()
    }

    private fun previous() {
        musicPosition =
            if (musicLists.size > 0) {
                if (musicPosition - 1 < 0) musicLists.size - 1 else --musicPosition
            } else musicPosition
        restart()
    }

    private fun restart() {
        musicFinish()
        play()
    }

    private fun musicFinish() {
//        musicTimer?.cancel()
//        musicTimer = null
        musicPlayer?.apply {
            stop()
            reset()
            release()
        }
        musicPlayer = null
    }

    private fun seekTo(position: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            musicPlayer?.seekTo(
                position * musicLists[musicPosition].duration / 100,
                MediaPlayer.SEEK_CLOSEST
            )
        } else {
            musicPlayer?.seekTo((position * musicLists[musicPosition].duration).toInt())
        }
    }

    private fun getPosition() = playPosition

    private fun getMusicDetail() = musicLists[musicPosition]

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

    private fun postData(
        displayName: String,
        duration: Long,
        uri: Uri,
        isPlaying: Boolean = false
    ) {
        Galley.musicState.postValue(
            MusicState(
                displayName,
                duration,
                0,
                uri,
                isPlaying
            )
        )
    }

    private fun getData() = if (musicLists.size > 0) MusicState(
        musicLists[musicPosition].title,
        musicLists[musicPosition].duration,
        0,
        musicLists[musicPosition].uri
    ) else MusicState("NO MUSIC", 0, 0, Uri.EMPTY)

    inner class MusicControlLer : Binder() {
        fun setDate(list: MutableList<Music>) = this@MusicService.setDate(list)
        fun play() = this@MusicService.play()
        fun pause() = this@MusicService.pause()
        fun next() = this@MusicService.next()
        fun previous() = this@MusicService.previous()
        fun getPosition() = this@MusicService.getPosition()
        fun getMusicDetail() = this@MusicService.getMusicDetail()
        fun getData() = this@MusicService.getData()
        fun seekTo(position: Long) = this@MusicService.seekTo(position)
    }
}
