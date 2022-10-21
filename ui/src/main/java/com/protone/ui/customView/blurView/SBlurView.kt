package com.protone.ui.customView.blurView

import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

class SBlurView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), IBlurConfig {

    private var blurTool: BaseBlurFactory = EmptyIBlurTool()
        set(value) {
            value.setBlurView(this)
            field = value
        }

    fun initBlurTool(blurTool: BaseBlurFactory): BaseBlurFactory {
        this.blurTool = blurTool
        return this.blurTool
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

    fun release(): BaseBlurFactory {
        blurTool.release()
        return blurTool
    }

    override fun setMaskXfMode(mode: PorterDuff.Mode) {
        blurTool.setMaskXfMode(mode)
    }

    override fun setMaskColor(@ColorInt color: Int) {
        blurTool.setMaskColor(color)
    }

    override fun setBlurRadius(radius: Float) {
        blurTool.setBlurRadius(radius)
    }

}