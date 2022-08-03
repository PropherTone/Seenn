package com.protone.seen.customView

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.AttrRes
import com.protone.api.TAG

class SameSizeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, heightMeasureSpec)
    }
}