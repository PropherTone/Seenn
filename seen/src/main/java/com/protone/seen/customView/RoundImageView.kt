package com.protone.seen.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.protone.api.TAG

class RoundImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private val path = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val radius = w / 15.toFloat()
        path.addRoundRect(
            RectF(radius, radius, w.toFloat() - radius, h.toFloat() - radius),
            radius,
            radius,
            Path.Direction.CW
        )
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.save()
        canvas?.clipPath(path)
        super.onDraw(canvas)
        canvas?.restore()
    }
}