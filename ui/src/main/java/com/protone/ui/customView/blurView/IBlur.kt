package com.protone.ui.customView.blurView

import android.graphics.Canvas
import android.graphics.PorterDuff
import androidx.annotation.ColorInt

interface IBlurTool {
    fun setBlurView(view: SBlurView)
    fun drawDecor()
    fun blur() : Boolean
    fun resize()
    fun drawBlurred(canvas: Canvas?)
    fun drawMask(canvas: Canvas?)
    fun release()
}

interface IBlurConfig{
    fun setMaskXfMode(mode: PorterDuff.Mode)
    fun setMaskColor(@ColorInt color: Int)
    fun setBlurRadius(radius: Float)
    fun setWillMove(willMove : Boolean)
}