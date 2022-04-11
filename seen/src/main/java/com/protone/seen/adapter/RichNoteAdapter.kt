package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.protone.api.context.layoutInflater
import com.protone.mediamodle.note.entity.*
import com.protone.mediamodle.note.spans.ColorSpan
import com.protone.mediamodle.note.spans.ISpan
import com.protone.seen.customView.InRecyclerView
import com.protone.seen.databinding.*
import java.lang.Exception

class RichNoteAdapter(
    val context: Context,
    private val isEditable: Boolean = true,
    dataList: ArrayList<Any>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ISpan {

    private val dataList by lazy { arrayListOf<Any>() }

    private var curPosition: Int? = null

    private var parent: InRecyclerView? = null

    companion object {
        const val TEXT = 0X01
        const val PHOTO = 0X02
        const val MUSIC = 0x03
        const val VIDEO = 0x04
    }

    init {
        this.dataList.addAll(dataList)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        parent = recyclerView as InRecyclerView
        parent?.setOnInterceptTouchEvent(object : InRecyclerView.OnTouch {
            override fun onTouch(x: Float, y: Float) {
                curPosition = parent?.findChildViewUnder(x, y)?.let {
                    parent?.getChildViewHolder(it)?.adapterPosition
                }
            }
        })
    }

    override fun getItemViewType(position: Int): Int {
        when (dataList[position]) {
            is RichNoteStates -> return TEXT
            is RichVideoStates -> return VIDEO
            is RichPhotoStates -> return PHOTO
            is RichMusicStates -> return MUSIC
        }
        return TEXT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            PHOTO -> RichPhotoHolder(
                RichPhotoLayoutBinding.inflate(
                    context.layoutInflater,
                    parent,
                    false
                )
            )
            MUSIC -> RichMusicHolder(
                RichMusicLayoutBinding.inflate(
                    (context.layoutInflater),
                    parent,
                    false
                )
            )
            VIDEO -> RichVideoHolder(
                RichVideoLayoutBinding.inflate(
                    context.layoutInflater,
                    parent,
                    false
                )
            )
            else -> if (isEditable) RichEditTextHolder(
                RichEditTextLayoutBinding.inflate(
                    context.layoutInflater,
                    parent,
                    false
                )
            ) else RichTextHolder(
                RichTextLayoutBinding.inflate(
                    context.layoutInflater,
                    parent,
                    false
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is RichTextHolder -> {
                holder.binding.text.text = (dataList[holder.layoutPosition] as RichNoteStates).text
            }
            is RichEditTextHolder -> {
                holder.binding.edit.apply {
                    setText((dataList[holder.layoutPosition] as RichNoteStates).text)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                        }

                        override fun afterTextChanged(s: Editable?) {
                            val layoutPosition = holder.layoutPosition
                            if (s?.length == 0 && layoutPosition != 0) {
                                dataList.removeAt(layoutPosition)
                                if (getItemViewType(layoutPosition - 1) != TEXT) {
                                    dataList.removeAt(layoutPosition - 1)
                                }
                                notifyItemRangeRemoved(layoutPosition - 1, layoutPosition)
                            }
                        }

                    })
                }
            }
            is RichPhotoHolder -> {
                holder.binding.apply {
                    (dataList[holder.layoutPosition]
                            as RichPhotoStates).let {
                        richPhotoTitle.text = it.name
                        it.date?.let { d -> richPhotoDetail.text = d }
                        richPhotoTvContainer.setOnClickListener { c ->
                            c.visibility = if (c.isVisible) View.INVISIBLE else View.VISIBLE
                        }
                        glideIv(richPhotoIv, it.uri)
                    }
                }
            }
            is RichMusicHolder -> {

            }
            is RichVideoHolder -> {

            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    private fun glideIv(iv: ImageView, uri: Uri) {
        iv.layoutParams
    }

    class RichTextHolder(val binding: RichTextLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    class RichEditTextHolder(val binding: RichEditTextLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    class RichPhotoHolder(val binding: RichPhotoLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    class RichMusicHolder(val binding: RichMusicLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    class RichVideoHolder(val binding: RichVideoLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun setBold() {
        setEditTextSpan(StyleSpan(Typeface.BOLD))
    }

    override fun setItalic() {
        setEditTextSpan(StyleSpan(Typeface.ITALIC))
    }

    override fun setSize(size: Int) {
        setEditTextSpan(AbsoluteSizeSpan(size))
    }

    override fun setUnderlined() {
        setEditTextSpan(UnderlineSpan())
    }

    override fun setStrikethrough() {
        setEditTextSpan(StrikethroughSpan())
    }

    override fun setColor(color: String) {
        setEditTextSpan(ColorSpan(color))
    }

    override fun setImage(uri: Uri, link: String?, name: String, date: String?) {
        dataList.add(RichPhotoStates(uri, link, name, date))
        notifyItemInserted(dataList.size - 1)
        dataList.add(RichNoteStates("", null))
        getCurEditText(dataList.size - 1)?.requestFocus()
        notifyItemInserted(dataList.size - 1)
    }

    private fun setEditTextSpan(span: Any) {
        getCurEditText(curPosition)?.apply {
            text.setSpan(
                span,
                selectionStart,
                selectionEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun getCurEditText(position: Int?): EditText? {
        return try {
            (position?.let { parent?.layoutManager?.findViewByPosition(it) } as EditText)
        } catch (e: Exception) {
            null
        }
    }
}