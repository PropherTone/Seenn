package com.protone.seen.customView

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.TypedValue

class AutoSizeTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatTextView(context, attrs) {

    companion object {
        const val TAG = "AutoSizeTextView_TAG"
    }

    private var mW = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mW = MeasureSpec.getSize(widthMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
//        measureText(mW)
    }

    private fun measureText(target: Int) {
        val cacheSize = textSize
        textSize = target.toFloat()
        val measureText = paint.measureText(text.toString().trim())
        if (measureText > target) {
            val scale = measureText / textSize
            val newSize = target / scale
            setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize)
        } else {
            textSize = cacheSize
        }
    }
}