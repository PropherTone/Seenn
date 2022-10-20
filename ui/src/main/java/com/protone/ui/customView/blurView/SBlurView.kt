package com.protone.ui.customView.blurView

import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

class SBlurView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var blurTool: BlurTool = EmptyBlurTool()
        set(value) {
            value.setBlurView(this)
            field = value
        }

    fun initBlurTool(blurTool: BlurTool) {
        this.blurTool = blurTool
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        blurTool.resize()
    }

    override fun onDraw(canvas: Canvas?) {
        blurTool.drawBlurred(canvas)
        super.onDraw(canvas)
    }

    fun renderFrame() {
        blurTool.blur()
    }

    fun setMaskColor(@ColorInt color: Int): BlurTool {
        blurTool.setMaskColor(color)
        return blurTool
    }

    fun release(): BlurTool {
        blurTool.release()
        return blurTool
    }

    fun setXfMode(mode: PorterDuff.Mode): BlurTool {
        blurTool.setMaskXfMode(mode)
        return blurTool
    }

    fun setBlurRadius(radius: Float): BlurTool {
        blurTool.setBlurRadius(radius)
        return blurTool
    }

}