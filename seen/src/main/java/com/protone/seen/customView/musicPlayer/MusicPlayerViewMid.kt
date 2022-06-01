package com.protone.seen.customView.musicPlayer

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView
import com.protone.api.context.layoutInflater
import com.protone.api.toBitmapByteArray
import com.protone.seen.R
import com.protone.seen.customView.ColorfulProgressBar
import com.protone.seen.databinding.AutoMusicPlayerLayoutMidBinding

class MusicPlayerViewMid @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BaseMusicPlayer(context, attrs) {

    private val binding =
        AutoMusicPlayerLayoutMidBinding.inflate(context.layoutInflater, this, true)

    override val next: ImageView
        get() = binding.musicNext
    override val control: ImageView
        get() = binding.musicControl
    override val previous: ImageView
        get() = binding.musicPrevious
    override val progress: ColorfulProgressBar
        get() = binding.musicProgress

    override var cover: Uri = Uri.EMPTY
        set(value) {
            loadAlbum(value.toBitmapByteArray(), binding.musicCover)
            field = value
        }

    override var duration: Long? = 0L
        set(value) {
            binding.musicProgress.barDuration = value?:0L
            field = value
        }

    override fun onPlay() {
        binding.musicControl.setImageResource(R.drawable.ic_round_on_white_24)
    }

    override fun onPause() {
        binding.musicControl.setImageResource(R.drawable.ic_round_paused_white_24)
    }

    override fun setName(name: String) {
        binding.musicName.text = name
    }

    override fun setDetail(detail: String) {}

}