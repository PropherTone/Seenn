package com.protone.seen.customView

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.LruCache
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.protone.api.TAG
import kotlinx.coroutines.*
import java.io.*
import kotlin.math.sqrt

val sBitmapCache = LruCache<String, Bitmap>(16)

class ScalableRegionLoadingImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), DecodeListener {

    private var decoder: SBitmapDecoder? = null

    private val gestureDetector = GestureDetector(context, GestureListener())

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

    fun setImageResource(assetsRes: String) {
        decoder = SBitmapDecoder(context, this)
        decoder?.setImageResource(assetsRes)
    }

    fun setImageResource(uri: Uri) {
        decoder = SBitmapDecoder(context, this)
        decoder?.setImageResource(uri)
    }

    fun setImageResource(file: File) {
        decoder?.setImageResource(file)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (decoder == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        decoder?.apply {
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
            sampleRect.set(0, 0, bitW, bitH)
            displayRect.set(0, 0, width, height)
            setOriginDisplayRect(0, 0, width, height)
            setMeasuredDimension(width, height)
        }
    }

    private var onDraw = false

    override fun onDraw(canvas: Canvas?) {
        if (decoder == null) {
            super.onDraw(canvas)
        } else {
            decoder?.apply {
                originalBitmap?.let { canvas?.drawBitmap(it, null, originalRect, null) }
                if (!onDraw) {
                    mBitmap?.let { canvas?.drawBitmap(it, null, displayRect, null) }
                    mBitmap?.recycle()
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
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val fingerCounts = ev.pointerCount
        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_UP -> {
                clkX = ev.x
                clkY = ev.y
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (fingerCounts == 2) {
                    mFinger1DownX = 0f
                    mFinger1DownY = 0f
                    mFinger2DownX = 0f
                    mFinger2DownY = 0f
                    onScale(scaleX)
                    return true
                }
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
                    var scale = (scaleX + space / width).toFloat()
                    if (scale < 1f) {
                        scale = 1f
                    }
                    setScale(scale.coerceAtMost(SCALE_MAX))
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
            MotionEvent.ACTION_DOWN -> {
                if (scaleX > 1f) parent.requestDisallowInterceptTouchEvent(true)
            }
            else -> return gestureDetector.onTouchEvent(ev)
        }
        return gestureDetector.onTouchEvent(ev)
    }

    override suspend fun onResource() {
        requestLayout()
    }

    override suspend fun onDecode() {
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        decoder?.clear()
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
        elevation = if (scale > 1f) 10f else 0f
        animate().scaleX(scale).scaleY(scale).setDuration(100)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    onScale(scale)
                }
            }).start()
    }

    private fun onScale(scale: Float) {
        onDraw = scale == 1f
        val globalRect = Rect()
        getGlobalVisibleRect(globalRect)
        val localRect = Rect()
        getLocalVisibleRect(localRect)
        decoder?.calculateDisplayZone(scale, globalRect, localRect)
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
        elevation = if (scale > 1f) 10f else 0f
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

        private val rect = Rect()

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (scaleX <= 1f) return false
            parent.requestDisallowInterceptTouchEvent(true)
//            val scaledWidth = width * scaleX
//            val scaledHeight = height * scaleX
            /*
            * GlobalVisibleRect
            * Left : left - 父View与屏幕左边距离 = 与父View左边的距离
            *        值和getLocationOnScreen、getLocationInWindow的array[0]相同
            *        view未超出显示区域为0
            * Top : top - 通知栏高度 - 父View与屏幕顶端距离 = 与父View顶端的距离
            *
            * */
//            getGlobalVisibleRect(rect)
//            Log.d(TAG, "getGlobalVisibleRect: $rect")
//            getLocalVisibleRect(rect)
//            Log.d(TAG, "getLocalVisibleRect: $rect")
//            val location = intArrayOf(0, 0)
//            getLocationOnScreen(location)
//            Log.d(TAG, "getLocationOnScreen: ${location[0]},${location[1]}")
//            getLocationInWindow(location)
            //intArrayOf[0] - parent.marginTop = 触顶高度
//            Log.d(TAG, "getLocationInWindow: ${location[0]},${location[1]}")
            //distanceY>0上滑,distanceX>0左滑
            y -= distanceY
            x -= distanceX
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
            Log.d(TAG, "onFling: ")
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

class SBitmapDecoder(val context: Context, private val onDecodeListener: DecodeListener?) :
    CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private var bitmapRegionDecoder: BitmapRegionDecoder? = null
    private var inputStream: InputStream? = null

    private var resName: String? = null

    private var options: BitmapFactory.Options = BitmapFactory.Options()
    val sampleRect = Rect()
    var originalRect = Rect()
        private set
    var displayRect: Rect = Rect()

    var originalBitmap: Bitmap? = null
        private set
    var mBitmap: Bitmap? = null
        private set

    var bitH = 0
    var bitW = 0

    //资源宽高比
    var srcWHScaled = 0f

    //资源和View大小比例
    var srcScaledW = 0f
    var srcScaledH = 0f

    fun setImageResource(assetsRes: String) {
        resName = assetsRes
        launch(Dispatchers.IO) {
            try {
                inputStream = context.assets?.open(assetsRes)
                context.assets?.open(assetsRes)?.let {
                    initWidthAndHeight(it)
                    generateDecoder()
                }
                withContext(Dispatchers.Main) {
                    onDecodeListener?.onResource()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    fun setImageResource(uri: Uri) {
        resName = uri.toString()
        launch(Dispatchers.IO) {
            try {
                inputStream = context.contentResolver?.openInputStream(uri)
                context.contentResolver?.openInputStream(uri)?.let {
                    initWidthAndHeight(it)
                    generateDecoder()
                }
                withContext(Dispatchers.Main) {
                    onDecodeListener?.onResource()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    fun setImageResource(file: File) {
        resName = file.path
        launch(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    inputStream = FileInputStream(file)
                    FileInputStream(file).let {
                        initWidthAndHeight(it)
                        generateDecoder()
                    }
                }
                withContext(Dispatchers.Main) {
                    onDecodeListener?.onResource()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    private fun initWidthAndHeight(input: InputStream) {
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(input, null, options)
        bitH = options.outHeight
        bitW = options.outWidth
        options.inJustDecodeBounds = false
        input.close()
        srcWHScaled = bitW.toFloat() / bitH
    }

    private fun generateDecoder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bitmapRegionDecoder =
                inputStream?.let { BitmapRegionDecoder.newInstance(it) }
        } else {
            @Suppress("DEPRECATION")
            bitmapRegionDecoder =
                inputStream?.let { BitmapRegionDecoder.newInstance(it, false) }
        }
    }

    fun setOriginDisplayRect(left: Int, top: Int, right: Int, bottom: Int) {
        if (resName == null) return
        launch(Dispatchers.IO) {
            if (originalBitmap != null) return@launch
            originalRect.set(left, top, right, bottom)
            options.inSampleSize = calculateInSampleSize(bitW, bitH, right, bottom)
            options.inJustDecodeBounds = true
            bitmapRegionDecoder?.decodeRegion(sampleRect, options)
            val outWidth = options.outWidth
            val outHeight = options.outHeight
            options.inJustDecodeBounds = false
            synchronized(sBitmapCache) {
                sBitmapCache.get(resName).let {
                    if (it != null) {
                        if (it.width == outWidth && it.height == outHeight) {
                            originalBitmap = it
                        } else {
                            originalBitmap =
                                bitmapRegionDecoder?.decodeRegion(sampleRect, options)
                            sBitmapCache.put(resName, originalBitmap)
                        }
                    } else {
                        originalBitmap = bitmapRegionDecoder?.decodeRegion(sampleRect, options)
                        sBitmapCache.put(resName, originalBitmap)
                    }
                }
            }
            withContext(Dispatchers.Main) { onDecodeListener?.onDecode() }
        }
    }

    fun calculateDisplayZone(scale: Float, globalRect: Rect, localRect: Rect) {
        launch {
            if (scale <= 1f) {
                mBitmap = bitmapRegionDecoder?.decodeRegion(sampleRect, options)
                withContext(Dispatchers.Main) {
                    onDecodeListener?.onDecode()
                }
                return@launch
            }
            //1
            val scaledW = (displayRect.right * scale).toInt()
            val scaledH = (displayRect.bottom * scale).toInt()
            options.inSampleSize = calculateInSampleSize(bitW, bitH, scaledW, scaledH)
            //2
            val l = localRect.left / scale
            val t = localRect.top / scale

            val displayW = globalRect.right / scale
            val displayH = (localRect.bottom - localRect.top) / scale
            val r = l + displayW
            val b = t + displayH
            //3
            displayRect.set(l.toInt(), t.toInt(), r.toInt(), b.toInt())
            //4
            sampleRect.set(
                (l * srcScaledW).toInt(),
                (t * srcScaledH).toInt(),
                (r * srcScaledW).toInt(),
                (b * srcScaledH).toInt()
            )
            //5
            mBitmap = bitmapRegionDecoder?.decodeRegion(sampleRect, options)
            withContext(Dispatchers.Main) {
                onDecodeListener?.onDecode()
            }
        }
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

    fun clear() {
        inputStream?.close()
        bitmapRegionDecoder?.recycle()
        originalBitmap = null
        mBitmap?.recycle()
        mBitmap = null
        resName = null
        bitmapRegionDecoder = null
        cancel()
    }
}

interface DecodeListener {
    suspend fun onResource()
    suspend fun onDecode()
}