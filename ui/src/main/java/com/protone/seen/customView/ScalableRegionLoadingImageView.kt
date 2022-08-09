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

val bitmapCache = object : LruCache<String, Bitmap>(16) {
    override fun entryRemoved(
        evicted: Boolean,
        key: String?,
        oldValue: Bitmap?,
        newValue: Bitmap?
    ) {
        super.entryRemoved(evicted, key, oldValue, newValue)
        oldValue?.recycle()
    }
}

class ScalableRegionLoadingImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), DecodeListener,
    GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {

    private var decoder: BitmapDecoder? = null

    private val gestureDetector = GestureDetector(context, this)

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
        gestureDetector.setOnDoubleTapListener(this)
    }

    fun setImageResource(assetsRes: String) {
        decoder = BitmapDecoder(context, this)
        decoder?.setImageResource(assetsRes)
    }

    fun setImageResource(uri: Uri) {
        decoder = BitmapDecoder(context, this)
        decoder?.setImageResource(uri)
    }

    fun setImageResource(file: File) {
        decoder?.setImageResource(file)
    }

    fun setCalculateSampleSize(func: (Int, Int, Int, Int) -> Int) {
        decoder?.calculateInSampleSize = func
    }

    fun isLongImage(): Boolean {
        return decoder?.isLongImage == true
    }

    fun reZone(){
        val rect = Rect()
        decoder?.calculateDisplayZone(scaleX, getGlobalVisibleRect(rect).let {
            rect
        }, getLocalVisibleRect(rect).let {
            rect
        },true)
    }

    fun clear() {
        decoder?.clear()
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
            setOriginDisplayRect(
                0,
                0,
                width,
                height,
                if (rootView.height > 0) rootView.height else width
            )
            setMeasuredDimension(width, height)
        }
    }

    private var onDraw = false

    override fun onDraw(canvas: Canvas?) {
        if (decoder == null) {
            super.onDraw(canvas)
        } else {
            decoder?.apply {
                originalBitmap?.let {
                    if (!it.isRecycled) {
                        canvas?.drawBitmap(it, null, originalRect, null)
                    }
                }
                if (!onDraw) {
                    mBitmap?.let {
                        if (!it.isRecycled) {
                            canvas?.drawBitmap(it, null, displayRect, null)
                            it.recycle()
                        }
                    }
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
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        val fingerCounts = ev?.pointerCount
        when (ev?.action?.and(MotionEvent.ACTION_MASK)) {
            MotionEvent.ACTION_UP -> {
                clkX = ev.x
                clkY = ev.y
                if (clkX != 0f || clkY != 0f) {
                    onScale(scaleX)
                }
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
        }
        return gestureDetector.onTouchEvent(ev)
    }

    override suspend fun onResource() {
        requestLayout()
    }

    override suspend fun onDecode() {
        invalidate()
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
        animate().translationX(0f).translationY(0f).setDuration(100)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
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

    override fun onDown(e: MotionEvent?): Boolean = true
    override fun onShowPress(e: MotionEvent?) = Unit
    override fun onSingleTapUp(e: MotionEvent?): Boolean = false
    override fun onDoubleTapEvent(e: MotionEvent?): Boolean = false
    override fun onLongPress(e: MotionEvent?) = Unit

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (scaleX <= 1f) return false
        translationX -= distanceX
        translationY -= distanceY
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }

    var onSingleTap: (() -> Unit)? = null
    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        onSingleTap?.invoke()
        return false
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        performZoom()
        return true
    }

}

class BitmapDecoder(val context: Context, private val onDecodeListener: DecodeListener?) :
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

    var isLongImage = false
        private set

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
        isLongImage = bitH / bitW > 2
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

    fun setOriginDisplayRect(left: Int, top: Int, right: Int, bottom: Int, maxHeight: Int) {
        if (resName == null) return
        launch(Dispatchers.IO) {
            if (resName == null) return@launch
            if (originalBitmap != null) return@launch
            originalRect.set(left, top, right, bottom)
            options.inSampleSize = calculateInSampleSize.invoke(
                bitW,
                bitH,
                right,
                if (maxHeight in 1 until bottom) maxHeight else bottom
            )
            synchronized(bitmapCache) {
                bitmapCache.get(resName).also {
                    if (it != null) {
                        if (it.width == bitW && it.height == bitH) {
                            originalBitmap = it
                        } else putCache()
                    } else putCache()
                }
            }
            withContext(Dispatchers.Main) { onDecodeListener?.onDecode() }
        }
    }

    private fun putCache() {
        try {
            originalBitmap = bitmapRegionDecoder?.decodeRegion(sampleRect, options)
            if (originalBitmap != null) {
                bitmapCache.put(resName, originalBitmap)
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "putCache: sampleRect $sampleRect")
        }
    }

    fun calculateDisplayZone(scale: Float, globalRect: Rect, localRect: Rect,onReZone : Boolean = false) {
        launch {
            try {
                if (scale <= 1f && !onReZone) {
                    mBitmap = bitmapRegionDecoder?.decodeRegion(sampleRect, options)
                    withContext(Dispatchers.Main) {
                        onDecodeListener?.onDecode()
                    }
                    return@launch
                }
                //1
                val scaledW = (displayRect.right * scale).toInt()
                val scaledH = (displayRect.bottom * scale).toInt()
                options.inSampleSize = calculateInSampleSize.invoke(bitW, bitH, scaledW,if (onReZone) bitH else scaledH)
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
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "calculateDisplayZone: localRect $localRect")
                Log.e(TAG, "calculateDisplayZone: globalRect $globalRect")
            }
        }
    }

    //采样率算法
    var calculateInSampleSize: (Int, Int, Int, Int) -> Int =
        { outWidth, outHeight, reqWidth, reqHeight ->
            // Raw height and width of image
            var inSampleSize = 1
            if (outHeight > reqHeight || outWidth > reqWidth) {
                val halfHeight = outHeight / 2
                val halfWidth = outWidth / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize >= reqHeight
                    || halfWidth / inSampleSize >= reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            inSampleSize
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