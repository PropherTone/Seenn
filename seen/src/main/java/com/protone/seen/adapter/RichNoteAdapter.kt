package com.protone.seen.adapter

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.protone.api.context.layoutInflater
import com.protone.mediamodle.note.entity.*
import com.protone.seen.databinding.*

class RichNoteAdapter(
    val context: Context,
    private val isEditable: Boolean = true,
    dataList: ArrayList<Any>
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val dataList by lazy { arrayListOf<Any>() }

    companion object {
        const val TEXT = 0X01
        const val PHOTO = 0X02
        const val MUSIC = 0x03
        const val VIDEO = 0x04
    }

    init {
        this.dataList.addAll(dataList)
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
                holder.binding.edit.setText((dataList[holder.layoutPosition] as RichNoteStates).text)
            }
            is RichPhotoHolder -> {
                holder.binding.apply {
                    (dataList[holder.layoutPosition]
                            as RichPhotoStates).let {
                        richPhotoTitle.text = it.name
                        it.date?.let { d-> richPhotoDetail.text = d }
                        richPhotoTvContainer.setOnClickListener { c ->
                            c.visibility = if (c.isVisible) View.INVISIBLE else View.VISIBLE
                        }
                        glideIv(richPhotoIv,it.uri)
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
}