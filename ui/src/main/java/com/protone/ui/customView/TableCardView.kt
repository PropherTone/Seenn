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

    fun show(
        onStart: Runnable? = null,
        onEnd: Runnable? = null,
        update: ValueAnimator.AnimatorUpdateListener? = null
    ) {
        animate().setInterpolator(interpolator)
            .translationY(0f + topBlock)
            .withEndAction(onEnd)
            .withStartAction(onStart)
            .setUpdateListener(update)
            .start()
    }

    fun hide(
        onStart: Runnable? = null,
        onEnd: Runnable? = null,
        update: ValueAnimator.AnimatorUpdateListener? = null
    ) {
        animate().setInterpolator(interpolator)
            .translationY(measuredHeight.toFloat() - botBlock)
            .withEndAction(onEnd)
            .withStartAction(onStart)
            .setUpdateListener(update)
            .start()
    }


}