package com.protone.ui.customView.blurView

import android.graphics.*
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.ColorInt

abstract class BaseBlurFactory(protected val blurEngine: BlurEngine) : IBlurTool, IBlurConfig {
    protected val scaleFactory = ScaleFactory()
    protected val decorCanvas = BlurCanvas()
    protected var decorBitmap: Bitmap? = null

    protected var canMove = false

    protected var maskColor: Int = Color.TRANSPARENT
        private set
    protected var xfMode: PorterDuff.Mode = PorterDuff.Mode.ADD
        private set

    protected fun transformCanvas() {
        scaleFactory.apply {
            decorCanvas.save()
            decorCanvas.translate(leftScaled, rightScaled)
            decorCanvas.scale(1 / wScaled, 1 / hScaled)
        }
    }

    protected fun makeCanvas(w: Int, h: Int, config: Bitmap.Config) {
        decorBitmap = Bitmap.createBitmap(w, h, config)
        decorCanvas.setBitmap(decorBitmap)
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

    override fun setWillMove(willMove: Boolean) {
        if (canMove != willMove) {
            if (willMove) decorCanvas.restore()
            else transformCanvas()
        }
        this.canMove = willMove
    }

    inner class BlurCanvas : Canvas()
}

class DefaultBlurController(private val root: View, blurEngine: BlurEngine) :
    BaseBlurFactory(blurEngine) {

    private var blurView: SBlurView? = null

    private val bitmapPaint: Paint = Paint().apply {
        flags = Paint.FILTER_BITMAP_FLAG
    }

    private var isResized = false
    private var start = false

    override fun drawDecor() {
        if (!start && !isResized) return
        root.apply {
            if (width <= 0 || height <= 0) return
            decorBitmap?.eraseColor(Color.TRANSPARENT)
            if (canMove) {
                decorCanvas.save()
                calculateSize()
                draw(decorCanvas)
                decorCanvas.restore()
            } else {
                draw(decorCanvas)
            }
        }
    }

    override fun blur(): Boolean {
        if (!start) return false
        drawDecor()
        decorBitmap = decorBitmap?.apply { blurEngine.blur(this) }
        return true
    }

    private fun calculateSize() {
        start = false
        isResized = false
        blurView?.apply {
            scaleFactory.apply {
                wScaled = (width / (decorBitmap?.width ?: width).toFloat())
                hScaled = (height / (decorBitmap?.height ?: height).toFloat())
                val rootLocation = intArrayOf(0, 0)
                val viewLocation = intArrayOf(0, 0)
                root.getLocationOnScreen(rootLocation)
                getLocationOnScreen(viewLocation)
                val opsX = viewLocation[0] - rootLocation[0]
                val opsY = viewLocation[1] - rootLocation[1]
                leftScaled = -opsX / wScaled
                rightScaled = -opsY / hScaled
            }
            transformCanvas()
        }
        isResized = true
        start = true
    }

    override fun resize() {
        start = false
        isResized = false
        blurView?.apply {
            if (width == 0 || height == 0) return@apply
            val scaleFactor = blurEngine.getScaleFactor(width)
            makeCanvas(
                (width / scaleFactor).toInt(),
                (height / scaleFactor).toInt(),
                blurEngine.getBitmapConfig()
            )
            calculateSize()
        }
    }

    override fun drawBlurred(canvas: Canvas?) {
        if (!start) return
        decorBitmap?.also {
            canvas?.apply {
                save()
                scale(scaleFactory.wScaled, scaleFactory.hScaled)
                drawBitmap(it, 0f, 0f, bitmapPaint)
                restore()
            }
            drawMask(canvas)
        }
    }

    override fun drawMask(canvas: Canvas?) {
        if (maskColor != Color.TRANSPARENT) {
            canvas?.drawColor(maskColor, xfMode)
        }
    }

    override fun setBlurView(view: SBlurView) {
        this.blurView = view.apply {
            setWillNotDraw(true)
            onGlobalLayout {
                resize()
                setWillNotDraw(false)
            }
        }
    }

    override fun release() {
        start = false
        blurView?.setWillNotDraw(true)
        decorBitmap?.recycle()
    }

    private inline fun View.onGlobalLayout(crossinline block: View.() -> Unit) {
        val view = this
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                block.invoke(view)
            }
        })
    }
}

class EmptyIBlurTool(blurEngine: BlurEngine = DefaultBlurEngine()) : BaseBlurFactory(blurEngine) {
    override fun drawDecor() = Unit
    override fun blur() = false
    override fun resize() = Unit
    override fun drawBlurred(canvas: Canvas?) = Unit
    override fun drawMask(canvas: Canvas?) = Unit
    override fun setMaskXfMode(mode: PorterDuff.Mode) = Unit
    override fun setBlurView(view: SBlurView) = Unit
    override fun setMaskColor(color: Int) = Unit
    override fun setBlurRadius(radius: Float) = Unit
    override fun release() = Unit
}