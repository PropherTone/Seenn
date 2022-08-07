package com.protone.testapp.view

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.protone.api.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.io.IOException
import java.io.InputStream
import kotlin.math.sqrt


class SImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), CoroutineScope by MainScope() {

    private var bitmapRegionDecoder: BitmapRegionDecoder? = null
    private var options: BitmapFactory.Options = BitmapFactory.Options()
    private val sampleRect = Rect()
    private var originalRect = Rect()
    private var displayRect: Rect = Rect()

    private var originalBitmap: Bitmap? = null
    private var mBitmap: Bitmap? = null

    private val gestureDetector = GestureDetector(context, GestureListener())

    private var bitH = 0
    private var bitW = 0

    //资源宽高比
    private var srcWHScaled = 0f

    //资源和View大小比例
    private var srcScaledW = 0f
    private var srcScaledH = 0f

    //缩放相关
    private var mFinger1DownX = 0f
    private var mFinger1DownY = 0f
    private var mFinger2DownX = 0f
    private var mFinger2DownY = 0f
    private var oldDistance = 0.0

    companion object {
        const val SCALE_MAX = 5.0f
        const val SCALE_MID = 2.5f
        const val SCALE_MIN = 1.0f
    }

    private var clkX = 0f
    private var clkY = 0f
    private var zoomIn = 0

    init {
        gestureDetector.setOnDoubleTapListener(DoubleTapListener())
    }

    fun setImageResource(input: InputStream, width: Int, height: Int) {
        try {
            bitH = height
            bitW = width
            srcWHScaled = bitW.toFloat() / bitH
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bitmapRegionDecoder =
                    BitmapRegionDecoder.newInstance(input)
            } else {
                @Suppress("DEPRECATION")
                bitmapRegionDecoder = BitmapRegionDecoder.newInstance(input, false)
            }
            requestLayout()
            invalidate()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var width = getDefaultSize(bitW, widthMeasureSpec)
        var height = getDefaultSize(bitH, heightMeasureSpec)
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize
            height = (width / srcWHScaled).toInt()
        } else if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize
            width = (height * srcWHScaled).toInt()
        } else if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED) {
            width = rootView.width
            height = (width / srcWHScaled).toInt()
        } else if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED) {
            height = rootView.height
            width = (height * srcWHScaled).toInt()
        }

        srcScaledW = bitW / width.toFloat()
        srcScaledH = bitH / height.toFloat()
        options.inSampleSize = calculateInSampleSize(bitW, bitH, width, height)
        sampleRect.set(0, 0, bitW, bitH)
        displayRect.set(0, 0, width, height)
        originalRect.set(0, 0, width, height)
        setMeasuredDimension(width, height)
    }

    //Google 采样率算法
    private fun calculateInSampleSize(
        outWidth: Int, outHeight: Int, reqWidth: Int, reqHeight: Int
    ): Int {
        // Raw height and width of image
        var inSampleSize = 1
        if (outHeight > reqHeight || outWidth > reqWidth) {
            val halfHeight = outHeight / 2
            val halfWidth = outWidth / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight
                && halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private var onDraw = false

    override fun onDraw(canvas: Canvas?) {
        mBitmap = bitmapRegionDecoder?.decodeRegion(sampleRect, options)
        if (originalBitmap == null) {
            originalBitmap = mBitmap
        } else {
            canvas?.drawBitmap(originalBitmap!!, null, originalRect, null)
        }
        if (!onDraw) {
            mBitmap?.let { canvas?.drawBitmap(it, null, displayRect, null) }
            //测试画框用
//            if (scaleX > 1f) {
//                canvas?.drawRect(displayRect, Paint().apply {
//                    color = Color.RED
//                    strokeWidth = 5f
//                    style = Paint.Style.STROKE
//                    isAntiAlias = true
//                    isDither = true
//                    strokeJoin = Paint.Join.ROUND
//                    strokeCap = Paint.Cap.ROUND
//                })
//            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val fingerCounts = ev.pointerCount
        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_UP -> {
                calculateDisplayZone(scaleX)
                if (fingerCounts == 2) {
                    mFinger1DownX = 0f
                    mFinger1DownY = 0f
                    mFinger2DownX = 0f
                    mFinger2DownY = 0f
                    val moveX1 = ev.getX(0)
                    val moveY1 = ev.getY(0)
                    val moveX2 = ev.getX(1)
                    val moveY2 = ev.getY(1)
                    val changeX1: Double = (moveX1 - mFinger1DownX).toDouble()
                    val changeY1: Double = (moveY1 - mFinger1DownY).toDouble()
                    val changeX2: Double = (moveX2 - mFinger2DownX).toDouble()
                    val changeY2: Double = (moveY2 - mFinger2DownY).toDouble()
                    if (scaleX > 1) { //滑动
                        val lessX = (changeX1 / 2 + changeX2 / 2).toFloat()
                        val lessY = (changeY1 / 2 + changeY2 / 2).toFloat()
                        setPivot(-lessX, -lessY)
                    }
                    val newDistance = spacing(ev)
                    val space: Double = newDistance - oldDistance
                    val scale = (scaleX + space / width).toFloat()
                    scale.coerceAtMost(SCALE_MAX).let {
                        setScale(it)
                        calculateDisplayZone(it)
                    }
                    mFinger1DownX = ev.getX(0)
                    mFinger1DownY = ev.getY(0)
                    mFinger2DownX = ev.getX(1)
                    mFinger2DownY = ev.getY(1)
                    oldDistance = spacing(ev)
                }
                clkX = ev.x
                clkY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (fingerCounts == 2) {
                    val moveX1 = ev.getX(0)
                    val moveY1 = ev.getY(0)
                    val moveX2 = ev.getX(1)
                    val moveY2 = ev.getY(1)
                    val changeX1: Double = (moveX1 - mFinger1DownX).toDouble()
                    val changeY1: Double = (moveY1 - mFinger1DownY).toDouble()
                    val changeX2: Double = (moveX2 - mFinger2DownX).toDouble()
                    val changeY2: Double = (moveY2 - mFinger2DownY).toDouble()
                    if (scaleX > 1) {
                        val lessX = (changeX1 / 2 + changeX2 / 2).toFloat()
                        val lessY = (changeY1 / 2 + changeY2 / 2).toFloat()
                        setPivot(-lessX, -lessY)
                    }
                    val newDistance = spacing(ev)
                    val space: Double = newDistance - oldDistance
                    val scale = (scaleX + space / width).toFloat()
                    setScale(scale.coerceAtMost(SCALE_MAX))
                    return true
                }
                clkX = ev.x
                clkY = ev.y
                if (fingerCounts == 2) {
                    mFinger1DownX = ev.getX(0)
                    mFinger1DownY = ev.getY(0)
                    mFinger2DownX = ev.getX(1)
                    mFinger2DownY = ev.getY(1)
                    oldDistance = spacing(ev)
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                clkX = ev.x
                clkY = ev.y
                if (fingerCounts == 2) {
                    mFinger1DownX = ev.getX(0)
                    mFinger1DownY = ev.getY(0)
                    mFinger2DownX = ev.getX(1)
                    mFinger2DownY = ev.getY(1)
                    oldDistance = spacing(ev)
                    return true
                }
            }
            else -> {}
        }
        return gestureDetector.onTouchEvent(ev)
    }

    private fun performZoom() {
        when (zoomIn) {
            0 -> {
                setPivotXYNoCalculate(clkX, clkY)
                animateScale(SCALE_MID)
                zoomIn++
            }
            1 -> {
                setPivotXYNoCalculate(clkX, clkY)
                animateScale(SCALE_MAX)
                zoomIn++
            }
            2 -> {
                animateScale(SCALE_MIN)
                zoomIn = 0
            }
        }
    }

    private fun setPivotXYNoCalculate(x: Float, y: Float) {
        pivotX = x
        pivotY = y
    }

    private fun animateScale(scale: Float) {
        onDraw = scale == 1f
        animate().scaleX(scale).scaleY(scale).setDuration(100)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    calculateDisplayZone(scale)
                }
            }).start()
    }

    private fun setPivot(x: Float, y: Float) {
        var mPivotX: Float
        var mPivotY: Float
        mPivotX = pivotX + x
        mPivotY = pivotY + y
        if (mPivotX < 0 && mPivotY < 0) {
            mPivotX = 0f
            mPivotY = 0f
        } else if (mPivotX > 0 && mPivotY < 0) {
            mPivotY = 0f
            if (mPivotX > width) {
                mPivotX = width.toFloat()
            }
        } else if (mPivotX < 0 && mPivotY > 0) {
            mPivotX = 0f
            if (mPivotY > height) {
                mPivotY = height.toFloat()
            }
        } else {
            if (mPivotX > width) {
                mPivotX = width.toFloat()
            }
            if (mPivotY > height) {
                mPivotY = height.toFloat()
            }
        }
        pivotX = mPivotX
        pivotY = mPivotY
    }

    private fun setScale(scale: Float) {
        scaleX = scale
        scaleY = scale
    }

    private fun spacing(event: MotionEvent): Double {
        return if (event.pointerCount == 2) {
            val x = event.getX(0) - event.getX(1)
            val y = event.getY(0) - event.getY(1)
            sqrt((x * x + y * y).toDouble())
        } else {
            0.0
        }
    }

    private fun calculateDisplayZone(scale: Float) {
        /* 1.计算缩放后view大小，根据缩放后view大小计算采样率
         * 2.计算缩放后所取图片资源的位置
         * 3.设置解析区域为计算后的图片资源位置
         * 4.设置绘图区域为缩放后的显示区域
         * 5.重绘试图
         * */
        if (scale <= 1f) {
            invalidate()
            return
        }
        //1
        val scaledW = (displayRect.right * scale).toInt()
        val scaledH = (displayRect.bottom * scale).toInt()
        options.inSampleSize = calculateInSampleSize(bitW, bitH, scaledW, scaledH)
        Log.d(TAG, "calculateDisplayZone: $scaledW,$scaledH")
        //2
        val globalRect = Rect()
        getGlobalVisibleRect(globalRect)
        Log.d(TAG, "getGlobalVisibleRect: $globalRect")
        val localRect = Rect()
        getLocalVisibleRect(localRect)
        Log.d(TAG, "getLocalVisibleRect: $localRect")

        val l = localRect.left / scale
        val t = localRect.top / scale

        val displayW = globalRect.right / scale
        val displayH = (localRect.bottom - localRect.top) / scale
        val r = l + displayW
        val b = t + displayH

        //3
        displayRect.set(l.toInt(), t.toInt(), r.toInt(), b.toInt())
        Log.d(TAG, "displayRect: $displayRect")
        //4
        sampleRect.set(
            (l * srcScaledW).toInt(),
            (t * srcScaledH).toInt(),
            (r * srcScaledW).toInt(),
            (b * srcScaledH).toInt()
        )
        //5
        invalidate()
    }

    inner class GestureListener : GestureDetector.OnGestureListener {

        override fun onDown(e: MotionEvent?): Boolean {
//            Log.d(TAG, "onDown: ")
            return true
        }

        override fun onShowPress(e: MotionEvent?) {
//            Log.d(TAG, "onShowPress: ")
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
//            Log.d(TAG, "onSingleTapUp: ")
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
//            Log.d(TAG, "onScroll: ")
            return true
        }

        override fun onLongPress(e: MotionEvent?) {
//            Log.d(TAG, "onLongPress: ")
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
//            Log.d(TAG, "onFling: ")
            return true
        }

    }

    inner class DoubleTapListener : GestureDetector.OnDoubleTapListener {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
//            Log.d(TAG, "onSingleTapConfirmed: ")
            Log.d(TAG, "animateScale:x $clkX,y $clkY")
            return true
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
//            Log.d(TAG, "onDoubleTap: ")
            performZoom()
            return true
        }

        override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
//            Log.d(TAG, "onDoubleTapEvent: ")
            return true
        }
    }


}