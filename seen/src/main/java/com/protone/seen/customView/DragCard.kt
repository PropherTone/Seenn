package com.protone.seen.customView

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.protone.api.context.layoutInflater
import com.protone.seen.R
import com.protone.seen.databinding.DragCardLayoutBinding
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.roundToInt

class DragCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val scaleAnimator = ValueAnimator().apply {
        duration = 100
    }

    private val listener = ValueAnimator.AnimatorUpdateListener {
        (it.animatedValue as Float).apply {
            scaleX = this
            scaleY = this
        }
    }

    private val mGPaint = Paint()
    private var scroll = 0

    var colors = intArrayOf(
        resources.getColor(R.color.Blue, null),
        resources.getColor(R.color.purple, null)
    )

    private var linearGradient = LinearGradient(
        0f, 0f, 0f, 0f,
        colors, null, Shader.TileMode.MIRROR
    )
    var gradientDuration = 100L

    private var animation = GlobalScope.launch(Dispatchers.IO) {
        while (true) {
            scroll += 20

            if (scroll >= measuredWidth shl 4) {
                scroll = 0
            }

            linearGradient = LinearGradient(
                scroll.toFloat(), 100f,
                ((measuredWidth shl 2) + scroll).toFloat(), 400f,
                colors, null, Shader.TileMode.MIRROR
            )
            mGPaint.shader = linearGradient
            withContext(Dispatchers.Main) {
                postInvalidateDelayed(10)
            }
            delay(gradientDuration)
        }
    }

    private var oldX: Float = 0f

    private val binding =
        DragCardLayoutBinding.inflate(
            context.layoutInflater,
            this, true
        )

    var icon: Drawable?
        get() = binding.mainIcon.drawable
        set(value) {
            binding.mainIcon.background = value
        }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.DragCard,
            defStyleAttr,
            defStyleRes
        )
            .apply {
                try {
                    icon = getDrawable(R.styleable.DragCard_icon)
                    colors[0] = getColor(R.styleable.DragCard_GradientColor1, colors[0])
                    colors[1] = getColor(R.styleable.DragCard_GradientColor2, colors[1])
                    gradientDuration = getInteger(R.styleable.DragCard_duration, 100).toLong()
                } finally {
                    recycle()
                }
            }

        scaleAnimator.addUpdateListener(listener)
        setBackgroundColor(Color.TRANSPARENT)
    }


    override fun onDraw(canvas: Canvas?) {
        if (animation.isActive) {
            canvas?.drawPaint(mGPaint)
            super.onDraw(canvas)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimator()
        scaleAnimator.addUpdateListener(listener)
    }

    override fun onDetachedFromWindow() {
        stopAnimator()
        scaleAnimator.removeAllListeners()
        super.onDetachedFromWindow()
    }

    fun startAnimator() {
        if (!animation.isActive) {
            animation.start()
        }
    }

    fun stopAnimator() {
        animation.cancel()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val currentWight = (parent as ViewGroup).width
        val child = binding.mainIcon
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                oldX = event.x
                bringToFront()
                scaleAnimator(1f, 0.98f)
            }
            MotionEvent.ACTION_MOVE -> {
                val fl = event.x - oldX
                if (oldX != event.x && abs(fl) > 5) {
                    val parentX = x.roundToInt()
                    val childX = child.x.roundToInt()

                    val moveView = {
                        x += fl
                    }

                    val location = IntArray(2)
                    child.getLocationInWindow(location)
                    val rawX = location[0]

                    //左右滑动
                    if (fl < 0) {
                        val width = width - child.width
                        if (child.x <= width) {
                            moveView()

                            //判断子控件贴边
                            if (parentX <= 0 - childX) {
                                child.x -= fl
                                //防止拖拽过快子控件出屏幕
                                if (rawX < -25) {
                                    child.x -= rawX
                                }
                                //防止拖拽过快子控件出界
                                if (child.x >= width) {
                                    child.x = width.toFloat()
                                }
                                //防止拖拽过快控件出屏幕
                                if (x + width < child.width) {
                                    x = 0 - width.toFloat()
                                }
                            }

                        }
                    } else {
                        if (child.x >= 0) {
                            moveView()
                            //判断子控件贴边
                            if (parentX + child.x + child.width >= currentWight
                            ) {
                                child.x -= fl
                                //防止拖拽过快子控件出屏幕
                                if (rawX + child.width - 25 > currentWight) {
                                    child.x -= rawX - currentWight + child.width
                                }
                                //防止拖拽过快子控件出界
                                if (child.x <= 0) {
                                    child.x = 0f
                                }
                                //防止拖拽过快控件出屏幕
                                if (x + child.width > currentWight) {
                                    x = currentWight - child.width.toFloat()
                                }
                            }

                        }
                    }

                    event.action = MotionEvent.ACTION_CANCEL
                }
            }
            MotionEvent.ACTION_UP -> {
                oldX = event.x
                scaleAnimator(0.98f, 1f)
                val location = IntArray(2)
                child.getLocationInWindow(location)
                val rawX = location[0]
                if (rawX < 0) {
                    child.x -= rawX
                }
                if (rawX + child.width > currentWight) {
                    child.x -= rawX - currentWight + child.width
                }
            }
        }
        return true
    }

    private fun scaleAnimator(f1: Float, f2: Float) {
        scaleAnimator.setFloatValues(f1, f2)
        scaleAnimator.start()
    }
}