package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.protone.database.room.entity.NoteType
import com.protone.seen.R
import com.protone.seen.databinding.NoteTpyeListAdapterBinding

class NoteTypeListAdapter(
    context: Context,
) : BaseAdapter<NoteTpyeListAdapterBinding>(context) {

    private val noteTypeList = arrayListOf<NoteType>()

    init {
        noteTypeList.add(NoteType(context.getString(R.string.all), ""))
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<NoteTpyeListAdapterBinding> {
        return Holder(
            NoteTpyeListAdapterBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    var addNote: ((String?) -> Unit)? = null
    var onTypeSelected: ((String?) -> Unit)? = null

    override fun onBindViewHolder(holder: Holder<NoteTpyeListAdapterBinding>, position: Int) {
        holder.binding.apply {
            root.setOnClickListener {
                onTypeSelected?.invoke(noteTypeName.text.toString())
            }
            noteTypeName.text = noteTypeList[holder.layoutPosition].type
            noteTypeAddNote.setOnClickListener {
                addNote?.invoke(noteTypeName.text.toString())
            }
        }
    }

    override fun getItemCount(): Int = noteTypeList.size

    @SuppressLint("NotifyDataSetChanged")
    fun setNoteTypeList(list: List<NoteType>) {
        noteTypeList.clear()
        noteTypeList.addAll(list)
        notifyDataSetChanged()
    }

}