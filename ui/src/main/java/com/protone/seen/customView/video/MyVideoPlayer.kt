package com.protone.seen.customView.video

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.protone.api.isInDebug
import com.protone.seen.customView.ColorfulProgressBar

class MyVideoPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr),
    TextureView.SurfaceTextureListener,
    MediaPlayer.OnVideoSizeChangedListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener {

    private var isPrepared: Boolean = false
    private lateinit var path: Uri

    private val videoController: MyVideoController by lazy { MyVideoController(context) }

    var title : String = ""
        set(value) {
            videoController.title = value
            field = value
        }

    fun setFullScreen(listener: () -> Unit) {
        videoController.fullScreen(listener)
    }

    private var mediaPlayer: MediaPlayer? = null
    private val textureView: MyTextureView? by lazy { MyTextureView(context) }

    private var surface: Surface? = null
    private var surfaceTexture: SurfaceTexture? = null
        set(value) {
            field = value
            surface = Surface(value)
        }

    private val progressHandler = Handler(context.mainLooper)

    private val progressRunnable = ProgressRunnable()

    inner class ProgressRunnable : Runnable {
        override fun run() {
            try {
                mediaPlayer?.let { videoController.seekTo(it.currentPosition.toLong()) }
                progressHandler.postDelayed(progressRunnable, 1000)
            } catch (ignored: Exception) {
            }
        }
    }

    init {
        videoController.apply {
            playVideo { play() }
            pauseVideo { pause() }
            setProgressListener(object : ColorfulProgressBar.Progress {
                override fun getProgress(position: Long) {
                    videoSeekTo(position)
                }
            })
        }
        removeView(videoController)
        addView(
            videoController,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        setOnClickListener {
            videoController.isVisible = !videoController.isVisible
        }
    }

    private fun initPlayer() {
        initVideoPlayer()
        initTextureView()
    }

    private fun initVideoPlayer() {
        try {
            release()
            mediaPlayer = MediaPlayer()
        } catch (e: Exception) {
            if (isInDebug()) e.printStackTrace()
        }
    }

    private fun initTextureView() {
        textureView?.surfaceTextureListener = this
        removeView(textureView)
        addView(
            textureView, 0, LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
    }

    fun setVideoPath(path: Uri) {
        this.path = path
        initPlayer()
    }

    private fun startProgress() {
        progressHandler.post(progressRunnable)
    }

    fun play() {
        try {
            if (mediaPlayer?.isPlaying == false && isPrepared) {
                mediaPlayer?.start()
                videoController.onStart()
                startProgress()
            }
        } catch (e: IllegalStateException) {
            if (isInDebug()) e.printStackTrace()
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                progressHandler.removeCallbacksAndMessages(null)
            }
        } catch (e: IllegalStateException) {
            if (isInDebug()) e.printStackTrace()
        }

    }

    fun release() {
        try {
            mediaPlayer?.apply {
                stop()
                release()
                isPrepared = false
            }
            mediaPlayer = null
            progressHandler.removeCallbacksAndMessages(null)
        } catch (e: IllegalStateException) {
            if (isInDebug()) e.printStackTrace()
        }
    }

    private fun videoSeekTo(position: Long) {
        try {
            mediaPlayer?.duration?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer?.seekTo(
                        (position.toFloat() / 100 * it).toLong(),
                        MediaPlayer.SEEK_CLOSEST
                    )
                } else {
                    mediaPlayer?.seekTo((position.toFloat() / 100 * it).toInt())
                }
            }
        } catch (e: IllegalStateException) {
            if (isInDebug()) e.printStackTrace()
        }
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
        try {
            if (surfaceTexture == null) {
                surfaceTexture = p0
                mediaPlayer?.apply {
                    setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .build()
                    )
                    setDataSource(context, path)
                    setSurface(surface)
                    prepareAsync()
                    setOnVideoSizeChangedListener(this@MyVideoPlayer)
                    setOnCompletionListener(this@MyVideoPlayer)
                    setOnPreparedListener(this@MyVideoPlayer)
                }
            } else {
                surfaceTexture?.apply {
                    textureView?.setSurfaceTexture(this)
                }
            }
        } catch (e: Exception) {
            if (isInDebug()) e.printStackTrace()
        }
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        return textureView == null
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {}

    override fun onVideoSizeChanged(p0: MediaPlayer?, p1: Int, p2: Int) {
        textureView?.adaptVideoSize(p1, p2)
        textureView?.measuredHeight?.let {
            textureView?.measuredWidth?.let { it1 -> measure(it1, it) }
        }
    }

    override fun onCompletion(p0: MediaPlayer?) {
        videoController.complete()
    }

    override fun onPrepared(p0: MediaPlayer?) {
        isPrepared = true
        videoController.seekTo(0)
        p0?.let { videoController.setVideoDuration(it.duration.toLong()) }
    }
}
