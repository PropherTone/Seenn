package com.protone.seen.customView

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.AttrRes
import androidx.core.widget.NestedScrollView
import com.protone.api.animation.AnimationHelper
import kotlin.math.abs

class DragRefreshView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
) : NestedScrollView(context, attrs, defStyleAttr) {


    private var oldY = 0f
    private var drag = 0f
    private var isTop = false

    private fun getChild(): View? {
        if (childCount > 0) {
            return getChildAt(0)
        }
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        getChild()?.let { child ->
            when (ev?.action) {
                MotionEvent.ACTION_DOWN -> {
                    oldY = ev.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val moveLen = ev.y - oldY
                    val abs = abs(moveLen)

                    if (child.measuredHeight < this.measuredHeight) {
                        if (moveLen >= 0) {
                            isTop = true
                            val fl = abs / 10
                            child.y += fl
                            drag += fl
                            overScrollListener?.onTop(drag)
                        } else {
                            isTop = false
                            val fl = abs / 10
                            child.y -= fl
                            drag -= fl
                            overScrollListener?.onBot(drag)
                        }
                        invalidate()
                    } else {
                        if (scrollY == 0) {
                            isTop = true
                            val fl = abs / 10
                            child.y += fl
                            drag += fl
                            overScrollListener?.onTop(drag)
                        } else if (scrollY == child.height - height) {
                            isTop = false
                            val fl = abs / 10
                            child.y -= fl
                            drag -= fl
                            overScrollListener?.onBot(drag)
                        }
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    oldY = ev.y
                    AnimationHelper.translationY(
                        child,
                        drag,
                        0f,
                        duration = 100,
                        play = true,
                        doOnEnd = {
                            if (abs(drag) > 100) {
                                overScrollListener?.onStop(isTop)
                            }
                        })
                    drag = 0f
                    invalidate()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    var overScrollListener: OverScrolled? = null

    interface OverScrolled {
        fun onTop(drag: Float)
        fun onBot(drag: Float)
        fun onStop(isTop: Boolean)
    }

}