package com.protone.seen.customView.musicPlayer

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import android.widget.ViewSwitcher
import com.protone.api.img.Blur
import com.protone.seen.R
import com.protone.seen.customView.ColorfulProgressBar
import kotlinx.coroutines.*

abstract class BaseMusicPlayer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), CoroutineScope by CoroutineScope(Dispatchers.IO) {

    companion object {
        const val LOOP_SINGLE = 0
        const val LOOP_LIST = 1
        const val PLAY_LIST = 2
        const val NO_LOOP = 3
        const val RANDOM = 4
    }

    abstract val next: ImageView?
    abstract val control: ImageView
    abstract val previous: ImageView?
    abstract val progress: ColorfulProgressBar?
    abstract var cover: Uri
    abstract var duration: Long?
    abstract var background1: ImageView
    abstract var background2: ImageView
    abstract var switcher: ViewSwitcher
    abstract var cover1: ImageView
    abstract var cover2: ImageView
    abstract var coverSwitcher: ViewSwitcher
    abstract var looper: ImageView?
    abstract val root: View
    var isPlay = false
        set(value) {
            if (value) onPlay() else onPause()
            field = value
        }

    abstract fun onPlay()
    abstract fun onPause()
    abstract fun setName(name: String)
    abstract fun setDetail(detail: String)

    fun loadAlbum(embeddedPicture: ByteArray?) {
        loadBlurCover(embeddedPicture)
        if (embeddedPicture == null) {
            (coverSwitcher.nextView as ImageView).setImageResource(R.drawable.ic_baseline_music_note_24)
        } else launch(Dispatchers.IO) {
            try {
                val decodeByteArray =
                    BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                withContext(Dispatchers.Main) {
                    (coverSwitcher.nextView as ImageView).setImageBitmap(decodeByteArray)
                    coverSwitcher.showNext()
                }
            } catch (e: Exception) {
            }
        }
//
//        Glide.with(context).asDrawable().load(embeddedPicture)
//            .placeholder(R.drawable.ic_baseline_music_note_24)
//            .error(R.drawable.ic_baseline_music_note_24)
//            .circleCrop().transition(DrawableTransitionOptions.withCrossFade(500))
//            .into(target)
    }

    private fun loadBlurCover(embeddedPicture: ByteArray?) {
        embeddedPicture?.let {
            launch(Dispatchers.IO) {
                try {
                    val blur =
                        Blur(context).blur(
                            BitmapFactory.decodeByteArray(it, 0, it.size, null),
                            radius = 30,
                            sampling = 10
                        )
                    withContext(Dispatchers.Main) {
                        (switcher.nextView as ImageView).setImageBitmap(blur)
                        switcher.showNext()
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    fun setLoopMode(mode: Int) {
        when (mode) {
            LOOP_LIST -> {
                looper?.setImageResource(R.drawable.ic_round_repeat_24_white)
                showToast(context.getString(R.string.loop_list))
            }
            LOOP_SINGLE -> {
                looper?.setImageResource(R.drawable.ic_round_repeat_one_24_white)
                showToast(context.getString(R.string.loop_single))
            }
            PLAY_LIST -> {
                looper?.setImageResource(R.drawable.ic_round_playlist_play_24_white)
                showToast(context.getString(R.string.play_list))
            }
            NO_LOOP -> {
                looper?.setImageResource(R.drawable.ic_round_block_24_white)
                showToast(context.getString(R.string.no_loop))
            }
            RANDOM -> {
                looper?.setImageResource(R.drawable.ic_round_loop_24_white)
                showToast(context.getString(R.string.random))
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPlay) progress?.startGradient()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progress?.stopGradient()
        cancel()
    }

    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}