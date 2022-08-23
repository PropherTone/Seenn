package com.protone.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.protone.api.baseType.toDateString
import com.protone.api.context.newLayoutInflater
import com.protone.api.entity.Note
import com.protone.ui.databinding.NoteListAdapterLayoutBinding

class NoteListAdapter(context: Context) :
    BaseAdapter<NoteListAdapterLayoutBinding, Any>(context) {

    private val noteList = arrayListOf<Note>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.setHasFixedSize(true)
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<NoteListAdapterLayoutBinding> {
        val binding = NoteListAdapterLayoutBinding.inflate(context.newLayoutInflater, parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder<NoteListAdapterLayoutBinding>, position: Int) {
        holder.binding.apply {
            root.setOnClickListener {
                noteListEventListener?.onNote(noteList[holder.layoutPosition].title)
            }
            root.setOnLongClickListener {
                noteListEventListener?.onDelete(noteList[holder.layoutPosition])
                true
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

    fun deleteNote(note: Note) {
        val indexOf = noteList.indexOf(note)
        if (indexOf != -1) {
            noteList.removeAt(indexOf)
            notifyItemRemoved(indexOf)
        }
    }

    var noteListEventListener: NoteListEvent? = null

    interface NoteListEvent {
        fun onNote(title: String)
        fun onDelete(note: Note)
    }
}