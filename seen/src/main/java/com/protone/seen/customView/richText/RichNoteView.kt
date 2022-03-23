package com.protone.seen.customView.richText

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.protone.api.context.layoutInflater
import com.protone.seen.databinding.NoteViewLayoutBinding

class RichNoteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    val binding = NoteViewLayoutBinding.inflate(context.layoutInflater, this, true)

    init {
        insertText()
    }

    fun insertText() {
        binding.richChild.addView(RichTextView(context))
    }

    fun insertMusic() {
        binding.richChild.addView(RichMusicView(context))
    }

    fun insertVideo() {
        binding.richChild.addView(RichVideoView(context))
    }

    fun insertPhoto() {
        binding.richChild.addView(RichPhotoView(context))
    }
}