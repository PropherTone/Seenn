package com.protone.seen.customView

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.cardview.widget.CardView
import com.protone.api.context.layoutInflater
import com.protone.seen.databinding.PhotoCardLayoutBinding

class PhotoCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val binding = PhotoCardLayoutBinding.inflate(context.layoutInflater, this, true)

    var title = ""
        set(value) {
            binding.photoCardTitle.text = value
            field = value
        }

//    var detail = ""
//        set(value) {
//            binding.photoCardDetial.text = value
//            field = value
//        }

    val photo : ImageView
        get() = binding.photoCardPhoto

}