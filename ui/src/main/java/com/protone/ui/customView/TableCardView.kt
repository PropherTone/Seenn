package com.protone.ui.customView

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

    fun show(){
        animate().setInterpolator(interpolator).translationY(0f+topBlock).start()
    }

    fun hide(){
        animate().setInterpolator(interpolator).translationY(measuredHeight.toFloat() - botBlock).start()
    }


}