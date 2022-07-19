package com.protone.seen.customView.video

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.protone.api.context.newLayoutInflater
import com.protone.api.baseType.getDrawable
import com.protone.seen.R
import com.protone.seen.customView.ColorfulProgressBar
import com.protone.seen.databinding.VideoControllerBinding

class MyVideoController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding = VideoControllerBinding.inflate(context.newLayoutInflater, this, true)

    var playVideo: () -> Unit = {}
    var pauseVideo: () -> Unit = {}

    private var isPlaying = false
        set(value) {
            binding.vStart.setImageDrawable( if (!value) R.drawable.ic_baseline_play_arrow_24_white.getDrawable()
            else R.drawable.ic_baseline_pause_24_white.getDrawable())
            binding.vControl.setImageDrawable(if (!value) R.drawable.ic_baseline_play_arrow_24_white.getDrawable()
            else R.drawable.ic_baseline_pause_24_white.getDrawable())
            field = value
        }

    var fullScreen: (() -> Unit)? = null

    init {
        binding.vFull.setOnClickListener {
            fullScreen?.invoke()
        }
        binding.vStart.setOnClickListener {
            if (!isPlaying) {
                playVideo()
                binding.vSeekBar.start()
            }
            it.isVisible = false
            binding.vContainer.isVisible = true
            isPlaying = true
        }
        binding.vControl.setOnClickListener {
            if (!isPlaying) {
                playVideo()
                binding.vSeekBar.start()
            } else {
                pauseVideo()
                binding.vSeekBar.stop()
            }
            isPlaying = !isPlaying
        }
    }

    fun complete() {
        isPlaying = false
        isVisible = true
        binding.vStart.isVisible = true
        binding.vContainer.isVisible = false
        binding.vSeekBar.stop()
    }

    fun setVideoDuration(duration: Long) {
        binding.vSeekBar.barDuration = duration
    }

    fun seekTo(duration: Long) = binding.vSeekBar.barSeekTo(duration)

    fun setProgressListener(listener: ColorfulProgressBar.Progress) {
        binding.vSeekBar.progressListener = listener
    }
}

