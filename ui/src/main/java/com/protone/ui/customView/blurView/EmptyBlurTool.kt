package com.protone.ui.customView.blurView

import android.graphics.Canvas
import android.graphics.PorterDuff

class EmptyBlurTool : BlurTool {
    override fun drawDecor() = Unit
    override fun blur() = Unit
    override fun resize() = Unit
    override fun drawBlurred(canvas: Canvas?) = Unit
    override fun drawMask(canvas: Canvas?) = Unit
    override fun setMaskXfMode(mode: PorterDuff.Mode) = Unit
    override fun setBlurView(view: SBlurView) = Unit
    override fun setMaskColor(color: Int) = Unit
    override fun release() = Unit
}