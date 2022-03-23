package com.protone.seen.customView

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import com.protone.api.TAG
import com.protone.seen.R

class StateImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    private var positive = false
    private var stateListener: StateListener? = null

    private var activeDrawable = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.ic_baseline_arrow_drop_down_24,
        null
    )
    private var negativeDrawable = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.ic_baseline_arrow_drop_up_24,
        null
    )

    init {
        setImageDrawable(activeDrawable)
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.StateImageView, defStyleAttr, defStyleRes
        ).let {
            activeDrawable = it.getDrawable(R.styleable.StateImageView_ActiveDrawable)
            negativeDrawable = it.getDrawable(R.styleable.StateImageView_NegativeDrawable)
        }
        this.setOnClickListener {
            if (positive) {
                active()
            } else {
                negative()
            }
        }
    }

    fun setOnStateListener(l: StateListener) {
        this.stateListener = l
    }

    fun active() {
        setImageDrawable(negativeDrawable)
        positive = false
        stateListener?.onActive()
    }

    fun negative() {
        setImageDrawable(activeDrawable)
        positive = true
        stateListener?.onNegative()
    }

    interface StateListener {
        fun onActive()
        fun onNegative()
    }
}