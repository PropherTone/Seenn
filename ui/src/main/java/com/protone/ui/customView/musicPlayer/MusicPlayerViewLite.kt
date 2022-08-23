package com.protone.ui.customView.musicPlayer

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ViewSwitcher
import com.protone.api.context.newLayoutInflater
import com.protone.api.baseType.toBitmapByteArray
import com.protone.ui.R
import com.protone.ui.customView.ColorfulProgressBar
import com.protone.ui.databinding.AutoMusicPlayerLayoutLiteBinding

class MusicPlayerViewLite @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BaseMusicPlayer(context, attrs) {

    private val binding =
        AutoMusicPlayerLayoutLiteBinding.inflate(context.newLayoutInflater, this, true)

    override val next: ImageView? = null
    override val control: ImageView = binding.musicControl
    override val previous: ImageView? = null
    override val progress: ColorfulProgressBar? = null
    override var background1: ImageView = binding.musicBack1
    override var background2: ImageView = binding.musicBack1
    override var switcher: ViewSwitcher = binding.musicBack
    override var cover1: ImageView = binding.musicCover1
    override var cover2: ImageView = binding.musicCover2
    override var coverSwitcher: ViewSwitcher = binding.musicCover
    override var looper: ImageView? = null
    override val root: View = binding.musicBack

    override var cover: Uri = Uri.EMPTY
        set(value) {
            loadAlbum(value.toBitmapByteArray())
            field = value
        }
    override var duration: Long? = 0L

    override fun onPlay() {
        binding.musicControl.setImageResource(R.drawable.ic_round_paused_white_24)
    }

    override fun onPause() {
        binding.musicControl.setImageResource(R.drawable.ic_round_on_white_24)
    }

    override fun setName(name: String) {
        binding.musicName.text = name
    }

    override fun setDetail(detail: String) {}

}