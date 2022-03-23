package com.protone.seen.customView

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes


abstract class RoundFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr,defStyleRes) {

    private val mRect: RectF = RectF()
    private val rect: RectF = RectF()
    private val mPath = Path()
    private val path by lazy { Path() }
    private val paint = Paint().also {
        it.isAntiAlias = true
        it.style = Paint.Style.FILL
        it.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val mPaint = Paint()

    init {
        setBackgroundColor(Color.parseColor("#00000000"))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val radius = w / 15 .toFloat()
        rect.set(20f, 20f, 100-20.toFloat(), 100-20.toFloat())
        mRect.set(0f, 0f, 100.toFloat(), 100.toFloat())
//        path.addRoundRect(rect ,25f , 25f , Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas?){
        canvas?.saveLayer(rect, null)
        super.onDraw(canvas)
        paint.reset()
        path.reset()
        path.addRoundRect(rect, 20f,20f, Path.Direction.CCW)
        mPath.reset()
        mPath.addRect(mRect, Path.Direction.CCW)
        mPath.op(path, Path.Op.DIFFERENCE)
        canvas?.drawPath(mPath, mPaint)
        mPaint.xfermode = null
        canvas?.restore()
        mPaint.xfermode = null
    }
}

//class RoundHelperImpl : RoundHelper {
//    private var mContext: Context? = null
//    private var mView: View? = null
//    private var mPaint: Paint? = null
//    private var mRectF: RectF? = null
//    private var mStrokeRectF: RectF? = null
//    private var mOriginRectF: RectF? = null
//    private var mPath: Path? = null
//    private var mTempPath: Path? = null
//    private var mXfermode: Xfermode? = null
//    private var isCircle = false
//    private var mRadii: FloatArray
//    private var mStrokeRadii: FloatArray
//    private var mWidth = 0
//    private var mHeight = 0
//    fun init(context: Context, attrs: AttributeSet?, view: View) {
//        if (view.background == null) {
//            view.setBackgroundColor(Color.parseColor("#00000000"))
//        }
//        view.setLayerType(View.LAYER_TYPE_NONE, null)
//        mContext = context
//        mView = view
//        mRadii = FloatArray(8)
//        mStrokeRadii = FloatArray(8)
//        mPaint = Paint()
//        mRectF = RectF()
//        mStrokeRectF = RectF()
//        mOriginRectF = RectF()
//        mPath = Path()
//        mTempPath = Path()
//        mXfermode =
//            PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
//    }
//
//    fun onSizeChanged(width: Int, height: Int) {
//        mWidth = width
//        mHeight = height
//        if (isCircle) {
//            val radius = Math.min(height, width) * 1f / 2
//            mRadiusTopLeft = radius
//            mRadiusTopRight = radius
//            mRadiusBottomRight = radius
//            mRadiusBottomLeft = radius
//        }
//        setRadius()
//        if (mRectF != null) {
//            mRectF!![mStrokeWidth, mStrokeWidth, width - mStrokeWidth] = height - mStrokeWidth
//        }
//        if (mStrokeRectF != null) {
//            mStrokeRectF!![mStrokeWidth / 2, mStrokeWidth / 2, width - mStrokeWidth / 2] =
//                height - mStrokeWidth / 2
//        }
//        if (mOriginRectF != null) {
//            mOriginRectF!![0f, 0f, width.toFloat()] = height.toFloat()
//        }
//    }
//
//    fun preDraw(canvas: Canvas) {
//        canvas.saveLayer(mRectF, null)
//    }
//
//    fun drawPath(canvas: Canvas) {
//        mPaint!!.reset()
//        mPath!!.reset()
//        mPaint!!.isAntiAlias = true
//        mPaint!!.style = Paint.Style.FILL
//        mPaint!!.xfermode = mXfermode
//        mPath!!.addRoundRect(mRectF!!, mRadii, Path.Direction.CCW)
//        mTempPath!!.reset()
//        mTempPath!!.addRect(mOriginRectF!!, Path.Direction.CCW)
//        mTempPath!!.op(mPath!!, Path.Op.DIFFERENCE)
//        canvas.drawPath(mTempPath!!, mPaint!!)
//        mPaint!!.xfermode = null
//        canvas.restore()
//        mPaint!!.xfermode = null
//    }
//
//    private fun setRadius() {
//        mRadii[1] = mRadiusTopLeft - mStrokeWidth
//        mRadii[0] = mRadii[1]
//        mRadii[3] = mRadiusTopRight - mStrokeWidth
//        mRadii[2] = mRadii[3]
//        mRadii[5] = mRadiusBottomRight - mStrokeWidth
//        mRadii[4] = mRadii[5]
//        mRadii[7] = mRadiusBottomLeft - mStrokeWidth
//        mRadii[6] = mRadii[7]
//        mStrokeRadii[1] = mRadiusTopLeft - mStrokeWidth / 2
//        mStrokeRadii[0] = mStrokeRadii[1]
//        mStrokeRadii[3] = mRadiusTopRight - mStrokeWidth / 2
//        mStrokeRadii[2] = mStrokeRadii[3]
//        mStrokeRadii[5] = mRadiusBottomRight - mStrokeWidth / 2
//        mStrokeRadii[4] = mStrokeRadii[5]
//        mStrokeRadii[7] = mRadiusBottomLeft - mStrokeWidth / 2
//        mStrokeRadii[6] = mStrokeRadii[7]
//    }
//}