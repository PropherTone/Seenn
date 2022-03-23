package com.protone.seen.customView.video

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.protone.seen.Progress
import java.io.IOException

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

    private val videoController: MyVideoController by lazy { MyVideoController(context) }
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

    init {

        videoController.apply {
            playVideo = { play() }
            pauseVideo = { pause() }
            setProgressListener(object : Progress {
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
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer()
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
        initPlayer()
        mediaPlayer?.setDataSource(context, path)
    }

//    fun setVideoPath(path: String) {
//        initPlayer()
//        mediaPlayer?.setDataSource(context, Uri.parse(path))
//    }

    private fun play() {
        try {
            if (isPrepared) {
                mediaPlayer?.start()
            }
        } catch (e: IOException) {

        }
    }

    private fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: IOException) {

        }

    }

    private fun release() {
        mediaPlayer?.apply {
            stop()
            reset()
            release()
        }
    }

    private fun videoSeekTo(position: Long) {
        mediaPlayer?.duration?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaPlayer?.seekTo(position * it / 100, MediaPlayer.SEEK_CLOSEST)
            } else {
                mediaPlayer?.seekTo((position * it).toInt())
            }
        }
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
        if (surfaceTexture == null) {
            surfaceTexture = p0
            mediaPlayer?.apply {
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
        videoController.complete()
    }

    override fun onPrepared(p0: MediaPlayer?) {
        isPrepared = true
        p0?.duration?.let { videoController.setVideoDuration(it.toLong()) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pause()
    }
}
