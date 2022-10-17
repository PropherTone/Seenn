package com.protone.ui.customView.musicPlayer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.protone.api.baseType.toBitmap
import com.protone.api.img.Blur
import com.protone.api.isInDebug
import com.protone.ui.R
import com.protone.ui.customView.ColorfulProgressBar
import com.protone.ui.customView.SwitchImageView
import kotlinx.coroutines.*

abstract class BaseMusicPlayer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), CoroutineScope by CoroutineScope(Dispatchers.Main) {

    abstract val next: ImageView?
    abstract val control: ImageView
    abstract val previous: ImageView?
    abstract val progress: ColorfulProgressBar?
    abstract var duration: Long?
    abstract val switcher: SwitchImageView
    abstract val coverSwitcher: SwitchImageView
    abstract var looper: ImageView?
    abstract val root: View

    var cover: Uri = Uri.EMPTY
        set(value) {
            loadAlbum(value)
            field = value
        }

    var isPlay = false
        set(value) {
            if (value) onPlay() else onPause()
            field = value
        }

    var interceptAlbumCover = false

    private var onBlurAlbumCover: ((Bitmap) -> Unit)? = null
    fun onBlurAlbumCover(block: (Bitmap) -> Unit) {
        this.onBlurAlbumCover = block
    }

    abstract fun onPlay()
    abstract fun onPause()
    abstract fun setName(name: String)
    abstract fun setDetail(detail: String)

    private fun loadAlbum(albumUri: Uri?) {
        launch {
            val albumBitmap = albumUri?.toBitmap()
            if (albumBitmap == null) {
                coverSwitcher.setImageResource(R.drawable.ic_baseline_music_note_24)
            } else {
                try {
                    loadBlurCover(albumBitmap)
                    coverSwitcher.setImageBitmap(albumBitmap)
                } catch (e: Exception) {
                    if (isInDebug()) e.printStackTrace()
                    coverSwitcher.setImageResource(R.drawable.ic_baseline_music_note_24)
                }
            }
        }
    }

    private fun loadBlurCover(albumBitmap: Bitmap) {
        launch(Dispatchers.Default) {
            try {
                val blur = Blur(context).blur(albumBitmap, radius = 12, sampling = 10)
                if (interceptAlbumCover && blur != null) {
                    withContext(Dispatchers.Main) {
                        onBlurAlbumCover?.invoke(blur)
                        switcher.setImageBitmap(null)
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    switcher.setImageBitmap(blur)
                }
            } catch (e: Exception) {
                if (isInDebug()) e.printStackTrace()
                withContext(Dispatchers.Main) {
                    switcher.setImageBitmap(null)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancel()
    }
}