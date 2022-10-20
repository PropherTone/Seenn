package com.protone.ui.customView.blurView

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.ColorInt

class BlurToolController(private val root: View, private val blurEngine: BlurEngine) : BlurTool {

    private var blurView: SBlurView? = null

    private var decorBitmap: Bitmap? = null

    private val decorCanvas = Canvas()

    private val scaleFactor = ScaleFactor()
    private var maskColor: Int = Color.TRANSPARENT
    private var xfMode: PorterDuff.Mode = PorterDuff.Mode.ADD

    private var isResized = false
    private var start = false

    override fun drawDecor() {
        if (!isResized) return
        root.apply {
            if (width <= 0 || height <= 0) return
            decorBitmap?.eraseColor(Color.TRANSPARENT)
            decorCanvas.save()
            scaleFactor.apply {
                decorCanvas.translate(leftScaled, rightScaled)
                decorCanvas.scale(1 / wScaled, 1 / hScaled)
            }
            draw(decorCanvas)
            decorCanvas.restore()
        }
    }

    override fun blur() {
        if (!start && !isResized) return
        drawDecor()
        decorBitmap = decorBitmap?.apply { blurEngine.blur(this) }
    }

    override fun resize() {
        isResized = false
        blurView?.let {
            scaleFactor.apply {
                wScaled = (it.width / (decorBitmap?.width ?: it.width).toFloat())
                hScaled = (it.height / (decorBitmap?.height ?: it.height).toFloat())
                leftScaled = -it.x / wScaled
                rightScaled = -it.y / hScaled
            }
            isResized = true
        }
    }

    override fun drawBlurred(canvas: Canvas?) {
        if (!start) return
        decorBitmap?.apply {
            blurEngine.draw(canvas, this, scaleFactor)
            drawMask(canvas)
        }
    }

    override fun drawMask(canvas: Canvas?) {
        if (maskColor != Color.TRANSPARENT) {
            canvas?.drawColor(maskColor, xfMode)
        }
    }

    override fun setBlurView(view: SBlurView) {
        this.blurView = view
        blurView?.apply {
            setWillNotDraw(true)
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    setWillNotDraw(false)
                    val scaleFactor = blurEngine.getScaleFactor(width)
                    decorBitmap = Bitmap.createBitmap(
                        (width / scaleFactor).toInt(),
                        (height / scaleFactor).toInt(),
                        blurEngine.getBitmapConfig()
                    )
                    decorCanvas.setBitmap(decorBitmap)
                    start = true
                    resize()
                }
            })
        }
    }

    override fun setMaskXfMode(mode: PorterDuff.Mode) {
        this.xfMode = mode
    }

    override fun setMaskColor(@ColorInt color: Int) {
        this.maskColor = color
    }

    override fun setBlurRadius(radius: Float) {
        blurEngine.setRadius(radius)
    }

    override fun release() {
        start = false
        blurView?.setWillNotDraw(true)
        decorBitmap?.recycle()
    }
}