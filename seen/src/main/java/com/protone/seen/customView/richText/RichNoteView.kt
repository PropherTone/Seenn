package com.protone.seen.customView.richText

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.bumptech.glide.Glide
import com.protone.api.context.layoutInflater
import com.protone.mediamodle.note.entity.RichNoteStates
import com.protone.mediamodle.note.entity.RichPhotoStates
import com.protone.seen.adapter.RichNoteAdapter
import com.protone.seen.databinding.NoteViewLayoutBinding
import com.protone.seen.databinding.RichPhotoLayoutBinding

class RichNoteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0,
    var isEditable: Boolean = false
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val richList = arrayListOf<Any>()

    private val richMap = mutableMapOf<Int, View>()

    init {
        orientation = VERTICAL
    }

    fun setRichList(list: List<Any>) {
        richList.clear()
        richList.addAll(list)
    }

    fun insertText(note: RichNoteStates) {
        richList.add(note)
        this.addView(when (isEditable) {
            true ->
                EditText(context).apply {
                    layoutParams = LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setText(note.text)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) = Unit

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) = Unit

                        override fun afterTextChanged(s: Editable?) {

                        }

                    })
                }
            else -> TextView(context).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = note.text
            }
        }.also { richMap[if (richList.size > 0) richList.size - 1 else 0] = it })
    }

    fun insertImage(note: RichPhotoStates) {
        addView(
            RichPhotoLayoutBinding
                .inflate(context.layoutInflater, this, false)
                .apply {
                    Glide.with(this.richPhotoIv.context).load(note.uri)
                        .into(this.richPhotoIv)
                    richPhotoTitle.text = note.name
                    richPhotoDetail.text = note.date
                }.root
        )
    }
}