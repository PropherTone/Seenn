package com.protone.seen.customView.video

import android.content.Context
import android.graphics.SurfaceTexture
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


//    private val binding =
//        VideoPlayerLayoutBinding.inflate(context.layoutInflater, this, true)

    private var isPrepared: Boolean = false
    private lateinit var path: Uri

    private val videoController: MyVideoController by lazy { MyVideoController(context) }

    fun setFullScreen(listener: () -> Unit) {
        videoController.fullScreen = listener
    }
//        get() {
//            return binding.videoController
//        }

    private var mediaPlayer: MediaPlayer? = null
    private val textureView: MyTextureView? by lazy { MyTextureView(context) }

    //        get() {
//            return binding.videoView
//        }
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
            playVideo = { play() }
            pauseVideo = { pause() }
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
            e.printStackTrace()
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
                startProgress()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                progressHandler.removeCallbacksAndMessages(null)
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }

    }

    fun release() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        progressHandler.removeCallbacksAndMessages(null)
    }

    private fun videoSeekTo(position: Long) {
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
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
        if (surfaceTexture == null) {
            surfaceTexture = p0
            mediaPlayer?.apply {
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
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        return textureView == null
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {}

    override fun onVideoSizeChanged(p0: MediaPlayer?, p1: Int, p2: Int) {
        textureView?.adaptVideoSize(p1, p2)
        textureView?.measuredHeight?.let {
            textureView?.measuredWidth?.let { it1 ->
                measure(
                    it1,
                    it
                )
            }
        }
    }

    override fun onCompletion(p0: MediaPlayer?) {
        isPrepared = false
        videoController.complete()
    }

    override fun onPrepared(p0: MediaPlayer?) {
        isPrepared = true
        videoController.seekTo(0)
        p0?.let { videoController.setVideoDuration(it.duration.toLong()) }

    }
}
