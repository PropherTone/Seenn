package com.protone.ui.customView

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ViewSwitcher
import androidx.annotation.DrawableRes
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.protone.ui.R

class SwitchImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val switcher: ViewSwitcher = ViewSwitcher(context, attrs)

    init {
        addView(switcher)
        repeat(2) {
            switcher.addView(ShapeableImageView(context).apply {
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
                shapeAppearanceModel = ShapeAppearanceModel.builder(context, R.style.ovalImage,R.style.ovalImage).build()
            })
        }
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        switcher.apply {
            (nextView as ImageView).setImageBitmap(bitmap)
            showNext()
        }
    }

    fun setImageResource(@DrawableRes res: Int) {
        switcher.apply {
            (nextView as ImageView).setImageResource(res)
            showNext()
        }
    }

    fun setImageDrawable(drawable: Drawable?) {
        switcher.apply {
            (nextView as ImageView).setImageDrawable(drawable)
            showNext()
        }
    }

}