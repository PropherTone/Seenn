package com.protone.seen.customView.musicPlayer

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ViewSwitcher
import com.protone.api.img.Blur
import com.protone.seen.R
import com.protone.seen.customView.ColorfulProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseMusicPlayer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), CoroutineScope by CoroutineScope(Dispatchers.IO) {

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
        } else launch {
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
    }

    private fun loadBlurCover(embeddedPicture: ByteArray?) {
        embeddedPicture?.let {
            launch {
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
}