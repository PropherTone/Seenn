package com.protone.seen.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import com.protone.api.context.APP
import com.protone.seen.R

class RefreshCoverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var ovalIndex = 360f / context.resources.getDimensionPixelSize(R.dimen.list_icon)

    private val rect = RectF()

    private val paint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = Color.WHITE
            isAntiAlias = true
        }
    }

    private var drag = 0f

    init {
        setBackgroundColor(Color.TRANSPARENT)
        context.theme.obtainStyledAttributes(attrs, R.styleable.RefreshCover, 0, 0).let {
            val dimension = it.getDimension(R.styleable.RefreshCover_refresh_icon_size, 20f)
            val top = it.getDimension(R.styleable.RefreshCover_refresh_margin_top, 0f)
            val width = APP.screenWidth / 2
            rect.set(width - dimension, top, width + dimension, top + dimension * 2)
            it.recycle()
        }
    }

    fun setData(data: Float) {
        drag = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawArc(
            rect,
            0f,
            if (drag * ovalIndex > 360) 360f else drag * ovalIndex,
            true,
            paint
        )
    }
}