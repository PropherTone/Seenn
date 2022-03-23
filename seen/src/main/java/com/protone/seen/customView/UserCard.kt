package com.protone.seen.customView

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.UserCardLayoutBinding

class UserCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding = UserCardLayoutBinding.inflate(context.layoutInflater, this, true)

    var welcomeText: CharSequence?
        set(value) {
            binding.userWelcome.text = value
        }
        get() = binding.userWelcome.text

    var dateText: CharSequence?
        set(value) {
            binding.userDate.text = value
        }
        get() = binding.userDate.text

    val userIcon: ImageView
        get() = binding.userIcon
}

