package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.protone.api.context.layoutInflater
import com.protone.api.toDateString
import com.protone.database.room.entity.Note
import com.protone.seen.databinding.NoteListAdapterLayoutBinding

class NoteListAdapter(context: Context) : BaseAdapter<NoteListAdapterLayoutBinding>(context) {

    private val noteList = arrayListOf<Note>()

    var noteClk: ((String) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<NoteListAdapterLayoutBinding> {
        val binding = NoteListAdapterLayoutBinding.inflate(context.layoutInflater, parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder<NoteListAdapterLayoutBinding>, position: Int) {
        holder.binding.apply {
            root.setOnClickListener {
                noteClk?.invoke(noteList[holder.layoutPosition].title)
            }
            noteList[holder.layoutPosition].let {
                Glide.with(context)
                    .asDrawable()
                    .load(it.imagePath)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(noteBack)
                noteTitle.text = it.title
                noteDate.text = it.time.toDateString()
            }
        }
    }

    override fun getItemCount(): Int = noteList.size

    @SuppressLint("NotifyDataSetChanged")
    fun setNoteList(list: List<Note>) {
        noteList.clear()
        noteList.addAll(list)
        notifyDataSetChanged()
    }

}