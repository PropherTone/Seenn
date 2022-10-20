package com.protone.ui.customView.blurView

import android.graphics.Canvas
import android.graphics.PorterDuff
import androidx.annotation.ColorInt

interface BlurTool {
    fun drawDecor()
    fun blur()
    fun resize()
    fun drawBlurred(canvas: Canvas?)
    fun drawMask(canvas: Canvas?)
    fun setMaskXfMode(mode: PorterDuff.Mode)
    fun setBlurView(view: SBlurView)
    fun setMaskColor(@ColorInt color: Int)
    fun setBlurRadius(radius: Float)
    fun release()
}