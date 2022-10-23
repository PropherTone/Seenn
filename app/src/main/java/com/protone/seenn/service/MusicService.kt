package com.protone.seenn.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.protone.api.baseType.toBitmap
import com.protone.api.context.*
import com.protone.api.entity.Music
import com.protone.seenn.R
import com.protone.seenn.broadcast.ApplicationBroadCast
import com.protone.seenn.broadcast.MusicReceiver
import com.protone.seenn.broadcast.musicBroadCastManager
import com.protone.seenn.viewModel.MusicControllerIMP.Companion.LOOP_LIST
import com.protone.seenn.viewModel.MusicControllerIMP.Companion.LOOP_SINGLE
import com.protone.seenn.viewModel.MusicControllerIMP.Companion.NO_LOOP
import com.protone.seenn.viewModel.MusicControllerIMP.Companion.PLAY_LIST
import com.protone.seenn.viewModel.MusicControllerIMP.Companion.RANDOM
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.timerTask
import kotlin.system.exitProcess

/**
 * MusicService by ProTone 2022/03/23
 */
class MusicService : Service(), CoroutineScope by CoroutineScope(Dispatchers.Default),
    IMusicService,
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
            if (playList.isEmpty()) return null
            if (field == null) {

                field = MediaPlayer.create(
                    SApplication.app,
                    playList[playPosition.get()].uri
                ).also {
                    it.setOnCompletionListener(this)
                }
            }
            return field
        }

    private fun initMusicPlayer(): MediaPlayer? {
        if (playList.isEmpty()) return null
        if (musicPlayer == null) {

            musicPlayer = MediaPlayer.create(
                SApplication.app,
                playList[playPosition.get()].uri
            ).also {
                it.setOnCompletionListener(this)
            }
        }
        return musicPlayer
    }

    private var progressTimer: Timer? = null

    private val mutex by lazy { Mutex() }

    private var playPosition = AtomicInteger(0)
    private val playList = mutableListOf<Music>()
    private val progress = MutableLiveData<Long>()
    private val playState = MutableLiveData<Boolean>()
    private val currentMusic = MutableLiveData<Music>()
    private val loopModeLive = MutableLiveData<Int>()

    private val appReceiver = object : ApplicationBroadCast() {
        override fun finish() {
            SApplication.app.stopService(MusicService::class.intent)
            onDestroy()
        }

        override fun music() {
        }

    }

    private val receiver = object : MusicReceiver() {
        override fun play() {
            this@MusicService.play()
        }

        override fun pause() {
            this@MusicService.pause()
        }

        override fun finish() {
            this@MusicService.finishMusic()
        }

        override fun previous() {
            this@MusicService.previous()
        }

        override fun next() {
            this@MusicService.next()
        }

        override fun refresh(b: Boolean, ref: Boolean) {
            notificationPlayState(b, ref)
        }

        private fun notificationPlayState(state: Boolean, ref: Boolean) =
            launch(Dispatchers.Default) {
                mutex.withLock {
                    if (playList.isEmpty()) return@launch
                    playState.postValue(state)
                    initMusicNotification()
                    withContext(Dispatchers.Main) {
                        remoteViews?.setImageViewResource(
                            R.id.notify_music_control,
                            if (state) R.drawable.ic_baseline_pause_24
                            else R.drawable.ic_baseline_play_arrow_24
                        )
                        if (state || ref) {
                            remoteViews?.setTextViewText(
                                R.id.notify_music_name,
                                playList[playPosition.get()].title
                            )
                            playList[playPosition.get()].uri.toBitmap()?.let { ba ->
                                remoteViews?.setImageViewBitmap(R.id.notify_music_icon, ba)
                            }
                        }
                    }
                    notificationManager?.notify(NOTIFICATION_ID, notification)
                }
            }
    }

    override fun onCreate() {
        super.onCreate()
        musicBroadCastManager.registerReceiver(receiver, musicIntentFilter)
        registerReceiver(receiver, musicIntentFilter)
        registerReceiver(appReceiver, appIntentFilter)
    }

    override fun onBind(intent: Intent?): IBinder {
        launch(Dispatchers.Default) {
            synchronized(playList) {
                if (playList.isNotEmpty())
                    currentMusic.postValue(playList[playPosition.get()])
            }
        }
        return MusicBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(0x01, initMusicNotification())
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        activityOperationBroadcast.sendBroadcast(Intent(ACTIVITY_FINISH))
        finishMusic()
        unregisterReceiver(receiver)
        unregisterReceiver(appReceiver)
        musicBroadCastManager.unregisterReceiver(receiver)
        notificationManager?.cancelAll()
        super.onDestroy()
        cancel()
        val activityManager =
            SApplication.app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.killBackgroundProcesses(SApplication.app.packageName)
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
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
        launch {
            mutex.withLock {
                (if (music != null) {
                    finishMusic()
                    if (playList.isEmpty()) {
                        currentMusic.postValue(getEmptyMusic())
                        return@launch
                    }
                    if (music !in playList) {
                        playList.add(music)
                    }
                    val index = playList.indexOf(music)
                    playPosition.set(index)
                    initMusicPlayer()
                } else musicPlayer)?.apply {
                    start()
                    currentMusic.postValue(playList[playPosition.get()])
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
    }

    override fun pause() {
        launch {
            mutex.withLock {
                if (musicPlayer?.isPlaying == true)
                    musicPlayer?.apply {
                        pause()
                        playState.postValue(false)
                        progressTimer?.cancel()
                        progressTimer = null
                    }
            }
        }
    }

    override fun next() {
        if (playList.isEmpty()) return
        launch {
            if (playPosition.incrementAndGet() > playList.size - 1) playPosition.set(0)
            finishMusic()
            play()
        }
    }

    override fun previous() {
        if (playList.isEmpty()) return
        launch {
            if (playPosition.decrementAndGet() <= 0) playPosition.set(playList.size - 1)
            finishMusic()
            play()
        }
    }

    override fun getPlayList(): MutableList<Music> {
        synchronized(playList) {
            return playList.ifEmpty { mutableListOf(getEmptyMusic()) }
        }
    }

    override fun setPlayList(list: MutableList<Music>) {
        launch(Dispatchers.Default) {
            synchronized(playList) {
                playList.clear()
                playList.addAll(list)
            }
        }
    }

    override fun onProgress(): LiveData<Long> = progress

    override fun onPlayState(): LiveData<Boolean> = playState

    override fun onLoopMode(): LiveData<Int> = loopModeLive

    override fun setProgress(progress: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            musicPlayer?.seekTo(
                progress * playList[playPosition.get()].duration / 100,
                MediaPlayer.SEEK_CLOSEST
            )
        } else {
            musicPlayer?.seekTo((progress * playList[playPosition.get()].duration).toInt())
        }
    }

    override fun onMusicPlaying(): LiveData<Music> = currentMusic

    override fun init(music: Music, progress: Long) {
        launch(Dispatchers.Default) {
            synchronized(this@MusicService) {
                currentMusic.postValue(
                    if (playList.isNotEmpty() && music.title != "NO MUSIC") {
                        playPosition.set(playList.indexOf(music))
                        if (playPosition.get() == -1) {
                            playPosition.set(0)
                            getEmptyMusic()
                        } else {
                            playList[playPosition.get()]
                        }
                    } else getEmptyMusic()
                )
            }
        }
    }

    private fun finishMusic() {
        musicPlayer?.apply {
            stop()
            reset()
            release()
            musicPlayer = null
        }
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
        playState.postValue(false)
        when (loopModeLive.value) {
            LOOP_LIST -> musicBroadCastManager.sendBroadcast(Intent(MUSIC_NEXT))
            LOOP_SINGLE -> {
                playPosition.decrementAndGet()
                musicBroadCastManager.sendBroadcast(Intent(MUSIC_NEXT))
            }
            NO_LOOP -> musicBroadCastManager.sendBroadcast(Intent(MUSIC_PAUSE))
            RANDOM -> {
                playPosition.set((0 until playList.size - 1).random())
                repeat(2) {
                    musicBroadCastManager.sendBroadcast(Intent(MUSIC_PLAY))
                }
            }
            PLAY_LIST -> {
                if (playPosition.get() <= playList.size - 1) musicBroadCastManager.sendBroadcast(
                    Intent(
                        MUSIC_NEXT
                    )
                )
                else musicBroadCastManager.sendBroadcast(Intent(MUSIC_PAUSE))
            }
        }
    }
}

fun getEmptyMusic() = Music(
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
    Uri.EMPTY
)

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