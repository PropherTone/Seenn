package com.protone.ui.customView.musicPlayer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import com.protone.api.context.newLayoutInflater
import com.protone.ui.R
import com.protone.ui.customView.ColorfulProgressBar
import com.protone.ui.customView.SwitchImageView
import com.protone.ui.databinding.AutoMusicPlayerLayoutMidBinding

class MusicPlayerViewMid @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BaseMusicPlayer(context, attrs) {

    private val binding =
        AutoMusicPlayerLayoutMidBinding.inflate(context.newLayoutInflater, this, true)

    override val next: ImageView = binding.musicNext
    override val control: ImageView = binding.musicControl
    override val previous: ImageView = binding.musicPrevious
    override val progress: ColorfulProgressBar = binding.musicProgress
    override val switcher: SwitchImageView = binding.musicBack
    override val coverSwitcher: SwitchImageView = binding.musicCover
    override var looper: ImageView? = binding.loopMode
    override val root: View = binding.musicBack

    override var duration: Long? = 0L
        set(value) {
            binding.musicProgress.barDuration = value ?: 0L
            field = value
        }

    override fun onPlay() {
        binding.musicProgress.start()
        binding.musicControl.setImageResource(R.drawable.ic_round_paused_white_24)
    }

    override fun onPause() {
        binding.musicProgress.stop()
        binding.musicControl.setImageResource(R.drawable.ic_round_on_white_24)
    }

    override fun setName(name: String) {
        binding.musicName.text = name
    }

    override fun setDetail(detail: String) {}

    init {
        coverSwitcher.enableShapeAppearance = true
    }

}