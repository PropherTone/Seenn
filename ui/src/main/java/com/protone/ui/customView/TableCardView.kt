package com.protone.ui.customView

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout

class TableCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    var topBlock = 0f
    var botBlock = 0f

    var interpolator = AccelerateDecelerateInterpolator()

    private var startAction: Runnable? = null
    private var endAction: Runnable? = null
    private var updateListener: ValueAnimator.AnimatorUpdateListener? = null

    fun withStartAction(start: Runnable): TableCardView {
        this.startAction = start
        return this
    }

    fun withEndAction(end: Runnable): TableCardView {
        this.endAction = end
        return this
    }

    fun setUpdateListener(listener: ValueAnimator.AnimatorUpdateListener): TableCardView {
        this.updateListener = listener
        return this
    }

    fun show() {
        animate().setInterpolator(interpolator)
            .translationY(0f + topBlock)
            .withEndAction(endAction)
            .withStartAction(startAction)
            .setUpdateListener(updateListener)
            .start()
    }

    fun hide() {
        animate().setInterpolator(interpolator)
            .translationY(measuredHeight.toFloat() - botBlock)
            .withEndAction(endAction)
            .setUpdateListener(updateListener)
            .withStartAction(startAction)
            .start()
    }


}