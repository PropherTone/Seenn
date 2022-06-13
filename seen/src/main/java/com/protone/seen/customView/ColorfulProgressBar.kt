package com.protone.seen.customView

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import com.protone.api.context.layoutInflater
import com.protone.seen.R
import com.protone.seen.databinding.ColorfulBarChildLayoutBinding

class ColorfulProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding = ColorfulBarChildLayoutBinding.inflate(context.layoutInflater, this, true)
    private var childX: Float
        get() = binding.root.x
        set(value) {
            binding.root.x = value
        }
    private var halfHeight = 0f
    private var halfChildW = 0
    private var rootWidth: Int = 0
    private var scroll = 0
    private var steep = 10
    private var speed = 100
    private var blurRadius = 0
    private var moveLength = 0f
    private var isTouch = false
    private var isStart: Boolean = false
    var barDuration: Long = 0

    private val colors = intArrayOf(
        Color.parseColor("#FFBB86FC"),
        Color.parseColor("#448AFF")
    )

    private var linearGradient: LinearGradient = LinearGradient(
        scroll.toFloat(), 100f,
        ((90 shl 2) + scroll).toFloat(), 200f,
        colors, null, Shader.TileMode.MIRROR
    )

    private val colorRunnable: Runnable = ColorRunnable()
    private val mHandler by lazy { Handler(Looper.getMainLooper()) }
    var progressListener: Progress? = null

    private val foreBarPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        isDither = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.BLACK
    }
    private val backBarPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        isDither = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = ResourcesCompat.getColor(resources, R.color.glass, null)
    }
    private val foreBarPath = Path()
    private val backBarPath = Path()

    init {
        setPadding(0)
        setBackgroundColor(Color.TRANSPARENT)
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.DragBar,
            0, 0
        ).apply {
            blurRadius = getInteger(R.styleable.DragBar_blurRadius, 5)
            recycle()
        }
        foreBarPaint.maskFilter = BlurMaskFilter(blurRadius.toFloat(), BlurMaskFilter.Blur.SOLID)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        when (event?.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                isTouch = true
                moveLength = event.x - halfChildW
                if (event.x > measuredWidth - halfChildW) {
                    moveLength = (measuredWidth - 2 * halfChildW).toFloat()
                } else if (event.x < halfChildW) {
                    moveLength = 0f
                }
                childX = moveLength
                progressListener?.getProgress(
                    (moveLength / (measuredWidth - 2 * halfChildW) * 100).toLong()
                )
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                isTouch = false
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        halfHeight = measuredHeight / 2.toFloat()
        rootWidth = measuredWidth.coerceAtLeast(rootWidth)
        (measuredHeight / 1.5).toFloat().let {
            foreBarPaint.strokeWidth = it
            backBarPaint.strokeWidth = it
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        halfChildW = binding.root.measuredWidth / 2
        backBarPath.moveTo(halfChildW.toFloat(), halfHeight)
        backBarPath.lineTo((width - halfChildW).toFloat(), halfHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        foreBarPath.apply {
            reset()
            moveTo(halfChildW.toFloat(), halfHeight)
            lineTo(moveLength + halfChildW, halfHeight)
        }
        foreBarPaint.shader = linearGradient
        canvas?.drawPath(backBarPath, backBarPaint)
        canvas?.drawPath(foreBarPath, foreBarPaint)
    }

    fun barSeekTo(position: Long) {
        if (isTouch) return
        moveLength = (position.toFloat() / barDuration.toFloat() * (rootWidth - 2 * halfChildW))
        if (moveLength > rootWidth - 2 * halfChildW) {
            moveLength = (rootWidth - 2 * halfChildW).toFloat()
        } else if (moveLength < 0) {
            moveLength = 0f
        }
        childX = moveLength
        invalidate()
    }

    fun startGradient() {
//        if (!isStart) {
//            mHandler.post(colorRunnable)
//            isStart = true
//        }
    }

    fun stopGradient() {
//        mHandler.removeCallbacksAndMessages(null)
    }

    inner class ColorRunnable : Runnable {

        override fun run() {
            scroll += steep

            if (scroll >= (moveLength + halfChildW).toInt() shl 4) {
                scroll = left
            }
            linearGradient = LinearGradient(
                scroll.toFloat(), 100f,
                ((90 shl 2) + scroll).toFloat(), 200f,
                colors, null, Shader.TileMode.MIRROR
            )

            postInvalidateDelayed(10)

            mHandler.postDelayed(colorRunnable, speed.toLong())
        }
    }

    interface Progress {
        fun getProgress(position: Long)
    }
}