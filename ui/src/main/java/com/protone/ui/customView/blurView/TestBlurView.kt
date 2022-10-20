package com.protone.ui.customView.blurView

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.protone.api.img.Blur
import com.protone.ui.R

class TestBlurView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var blurBitmap: Bitmap? = null

    private val decorCanvas = Canvas()

    private var blurTargetView: View? = null

    private val renderPaint: Paint = Paint().apply {
        flags = Paint.FILTER_BITMAP_FLAG
    }
    private val forePaint: Paint = Paint().apply {
        this.color = ResourcesCompat.getColor(resources, R.color.transparent_black, null)
    }

    private var decorBitmap: Bitmap? = null

    private var scaleFactor: Float = 5f
    private var hScaled: Float = 0f
    private var wScaled: Float = 0f
    private var leftScaled: Float = 0f
    private var rightScaled: Float = 0f

    var blurFrameEmitter: BlurFrame = object : BlurFrame {
        override fun blur() {
            (blurTargetView ?: (parent as View?))?.apply {
                if (width <= 0 || height <= 0) return
                decorBitmap?.eraseColor(Color.TRANSPARENT)
                decorCanvas.save()
                decorCanvas.translate(leftScaled, rightScaled)
                decorCanvas.scale(1 / wScaled, 1 / hScaled)
                draw(decorCanvas)
                decorCanvas.restore()
                decorBitmap = decorBitmap?.apply { Blur.doFastBlur(this, 24f) }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        (blurTargetView ?: (parent as View?))?.let {
            decorBitmap = Bitmap.createBitmap(
                (it.width / scaleFactor).toInt(),
                (it.height / scaleFactor).toInt(), Bitmap.Config.ARGB_8888
            )
            decorCanvas.setBitmap(decorBitmap)
            wScaled = (this.width /
                    (decorBitmap?.width ?: this.width).toFloat())
            hScaled = (this.height /
                    (decorBitmap?.height ?: this.height).toFloat())
            leftScaled = -this@TestBlurView.x / wScaled
            rightScaled = -this@TestBlurView.y / hScaled
        }
    }

    override fun onDrawForeground(canvas: Canvas?) {
        canvas?.drawPaint(forePaint)
        super.onDrawForeground(canvas)
    }

    override fun onDraw(canvas: Canvas?) {
        decorBitmap?.apply {
            canvas?.save()
            canvas?.scale(wScaled, hScaled)
            canvas?.drawBitmap(this, 0f, 0f, renderPaint)
            canvas?.restore()
        }
        super.onDraw(canvas)
    }

    interface BlurFrame {
        fun blur()
    }
}