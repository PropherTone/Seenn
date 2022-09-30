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
import kotlinx.coroutines.launch

class NoteListAdapter(context: Context) :
    BaseAdapter<NoteListAdapterLayoutBinding, NoteListAdapter.NoteEvent>(context, true) {

    private val noteList = arrayListOf<Note>()

    sealed class NoteEvent {
        data class NoteDelete(val note: Note) : NoteEvent()
        data class NoteInsert(val note: Note) : NoteEvent()
        data class NoteUpdate(val note: Note) : NoteEvent()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.setHasFixedSize(true)
        super.onAttachedToRecyclerView(recyclerView)
    }

    override suspend fun onEventIO(data: NoteEvent) {
        when (data) {
            is NoteEvent.NoteDelete -> {
                val indexOf = noteList.indexOf(data.note)
                if (indexOf != -1) {
                    noteList.removeAt(indexOf)
                    notifyItemRemovedCO(indexOf)
                }
            }
            is NoteEvent.NoteInsert -> {
                noteList.add(0, data.note)
                notifyItemInsertedCO(0)
            }
            is NoteEvent.NoteUpdate -> {
                val index = noteList.indexOf(data.note)
                if (index != -1) {
                    noteList[index] = data.note
                    notifyItemChangedCO(index)
                }
            }
        }
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
        launch {
            emit(NoteEvent.NoteDelete(note))
        }
    }

    fun insertNote(note: Note) {
        launch {
            emit(NoteEvent.NoteInsert(note))
        }
    }

    fun updateNote(note: Note) {
        launch {
            emit(NoteEvent.NoteUpdate(note))
        }
    }

    var noteListEventListener: NoteListEvent? = null

    interface NoteListEvent {
        fun onNote(title: String)
        fun onDelete(note: Note)
    }
}