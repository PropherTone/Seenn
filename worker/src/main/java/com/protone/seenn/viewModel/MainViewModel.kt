package com.protone.seenn.viewModel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.protone.api.context.SApplication
import com.protone.api.img.Blur
import com.protone.seenn.R
import java.io.File

class MainViewModel : BaseViewModel() {
    var btnY = 0f
    val btnH = SApplication.app.resources.getDimensionPixelSize(R.dimen.action_icon_p)

    fun loadBlurIcon(path: String): Bitmap? {
        return try {
            Blur(SApplication.app).blur(
                BitmapFactory.decodeFile(path),
                radius = 10,
                sampling = 10
            )
        } catch (e: Exception) {
            null
        }
    }
}