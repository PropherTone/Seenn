package com.protone.ui.customView.musicPlayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ViewSwitcher
import com.protone.api.img.Blur
import com.protone.api.isInDebug
import com.protone.ui.R
import com.protone.ui.customView.ColorfulProgressBar
import kotlinx.coroutines.*

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

    private var albumBitmap: Bitmap? = null

    fun loadAlbum(embeddedPicture: ByteArray?) {
        if (embeddedPicture == null) {
            (coverSwitcher.nextView as ImageView).setImageResource(R.drawable.ic_baseline_music_note_24)
            coverSwitcher.showNext()
            albumBitmap = null
        } else launch {
            try {
                albumBitmap =
                    BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                loadBlurCover()
                withContext(Dispatchers.Main) {
                    (coverSwitcher.nextView as ImageView).setImageBitmap(albumBitmap)
                    coverSwitcher.showNext()
                }
            } catch (e: Exception) {
                if (isInDebug()) e.printStackTrace()
                (coverSwitcher.nextView as ImageView).setImageResource(R.drawable.ic_baseline_music_note_24)
                coverSwitcher.showNext()
            }
        }
    }

    private fun loadBlurCover() {
        if (albumBitmap != null) {
            launch(Dispatchers.IO) {
                try {
                    val blur =
                        Blur(context).blur(
                            albumBitmap!!,
                            radius = 30,
                            sampling = 10
                        )
                    withContext(Dispatchers.Main) {
                        (switcher.nextView as ImageView).setImageBitmap(blur)
                        switcher.showNext()
                    }
                } catch (e: Exception) {
                    if (isInDebug()) e.printStackTrace()
                    (switcher.nextView as ImageView).setImageResource(R.drawable.main_background)
                    switcher.showNext()
                }
            }
        } else {
            (switcher.nextView as ImageView).setImageResource(R.drawable.main_background)
            switcher.showNext()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancel()
    }
}