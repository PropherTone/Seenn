package com.protone.seen.customView.video

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.protone.api.TAG
import com.protone.api.context.layoutInflater
import com.protone.seen.Progress
import com.protone.seen.R
import com.protone.seen.customView.DragBar.DragBar
import com.protone.seen.databinding.VideoControllerBinding

class MyVideoController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding = VideoControllerBinding.inflate(context.layoutInflater, this, true)

    var playVideo: () -> Unit = {}
    var pauseVideo: () -> Unit = {}

    private var isPlaying = false
        set(value) {
            binding.vStart.background = ResourcesCompat.getDrawable(
                resources,
                if (!value) R.drawable.ic_baseline_play_arrow_24_white
                else R.drawable.ic_baseline_pause_24_white,
                null
            )
            field = value
        }

    init {
        binding.vStart.setOnClickListener {
            if (!isPlaying) {
                playVideo()
            } else pauseVideo()
            this.isVisible = false
            isPlaying = !isPlaying
        }
    }

    fun complete() {
        isPlaying = false
        isVisible = true
    }

    fun setVideoDuration(duration: Long) {
        binding.vSeekBar.barDuration = duration;
    }

    fun seekTo(duration: Long) = binding.vSeekBar.barSeekTo(duration)

    fun setProgressListener(listener: Progress) {
        binding.vSeekBar.progressListener = listener
    }
}

