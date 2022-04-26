package com.protone.seen.customView

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.protone.api.context.layoutInflater
import com.protone.seen.databinding.DragRectLayoutBinding

@SuppressLint("ClickableViewAccessibility")
class DragRect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), View.OnTouchListener {

    val binding = DragRectLayoutBinding.inflate(context.layoutInflater, this, true)
    private var dragTarget: View? = null

    init {
        binding.apply {
//            topLeft.setOnTouchListener(this@DragRect)
//            topRight.setOnTouchListener(this@DragRect)
//            botLeft.setOnTouchListener(this@DragRect)
            botRight.setOnTouchListener(this@DragRect)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        dragTarget?.apply {
            when (ev?.action) {
                MotionEvent.ACTION_DOWN -> return true
                MotionEvent.ACTION_MOVE -> {
                    x = if (ev.x + width / 2 > this@DragRect.width) {
                        this@DragRect.width.toFloat()
                    } else ev.x
                    y = if (ev.y + height / 2 > this@DragRect.height) {
                        this@DragRect.height.toFloat()
                    } else ev.y
//                when (dragTarget) {
//                    binding.topLeft -> {
//                        binding.topRight.y = y
//                        binding.botLeft.x = x
//                    }
//                    binding.topRight -> {
//                        binding.topLeft.y = y
//                        binding.botRight.x = x
//                    }
//                    binding.botLeft -> {
//                        binding.topLeft.x = x
//                        binding.botRight.y = y
//                    }
//                    binding.botRight -> {
                    binding.topRight.x = x
                    binding.botLeft.y = y
//                    }
//                }
                    rectSizeListener?.onChange((x + width / 2).toInt(), (y + height / 2).toInt())
                }
            }
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> dragTarget = v
            MotionEvent.ACTION_UP -> dragTarget = null
        }
        return false
    }

    var rectSizeListener: RectSize? = null

    interface RectSize {
        fun onChange(x: Int, y: Int)
    }
}