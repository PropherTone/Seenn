package com.protone.seen.customView.musicPlayer

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.protone.seen.R
import com.protone.seen.customView.ColorfulProgressBar

abstract class BaseMusicPlayer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    abstract val next: ImageView
    abstract val control: ImageView
    abstract val previous: ImageView
    abstract val progress: ColorfulProgressBar
    abstract var cover: Uri
    abstract var duration: Long
    var isPlay = false
        set(value) {
            if (value) onPlay() else onPause()
            field = value
        }

    abstract fun onPlay()
    abstract fun onPause()

    fun loadAlbum(embeddedPicture: ByteArray?, target: ImageView) {
        Glide.with(context).load(embeddedPicture)
            .error(R.drawable.ic_baseline_music_note_24)
            .circleCrop().transition(DrawableTransitionOptions.withCrossFade())
            .into(target)
    }

}