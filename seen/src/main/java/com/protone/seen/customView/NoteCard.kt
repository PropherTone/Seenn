package com.protone.seen.customView

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.cardview.widget.CardView
import com.protone.api.context.layoutInflater
import com.protone.seen.databinding.NoteCardLayoutBinding


class NoteCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val binding = NoteCardLayoutBinding.inflate(context.layoutInflater, this, true)

    var title = ""
        set(value) {
            binding.noteCardTitle.text = value
            field = value
        }

    var date = ""
        set(value) {
            binding.noteCardDate.text = value
            field = value
        }
}