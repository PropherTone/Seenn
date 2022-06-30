package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.NoteType
import com.protone.seen.R
import com.protone.seen.databinding.NoteTpyeListAdapterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteTypeListAdapter(
    context: Context,
) : BaseAdapter<NoteTpyeListAdapterBinding>(context) {

    private val noteTypeList = arrayListOf<NoteType>()

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
            root.setOnLongClickListener {
                AlertDialog.Builder(context).setPositiveButton(
                    context.getString(R.string.confirm)
                ) { dialog, _ ->
                    val noteType = noteTypeList[position]
                    DataBaseDAOHelper.execute {
                        val re = DataBaseDAOHelper.doDeleteNoteTypeRs(noteType)
                        if (re) {
                            val index = noteTypeList.indexOf(noteType)
                            noteTypeList.removeAt(index)
                            withContext(Dispatchers.Main) {
                                notifyItemRemoved(index)
                            }
                        } else withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.failed_msg),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    dialog.dismiss()
                }.setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.setTitle(context.getString(R.string.delete)).create().show()
                return@setOnLongClickListener false
            }
            noteTypeName.text = noteTypeList[holder.layoutPosition].type
            noteTypeAddNote.setOnClickListener {
                addNote?.invoke(noteTypeName.text.toString())
            }
        }
    }

    override fun getItemCount(): Int = noteTypeList.size

    fun insertNoteType(noteType: NoteType) {
        noteTypeList.add(noteType)
        notifyItemInserted(noteTypeList.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNoteTypeList(list: List<NoteType>) {
        noteTypeList.clear()
        noteTypeList.add(NoteType(context.getString(R.string.all), ""))
        noteTypeList.addAll(list)
        notifyDataSetChanged()
    }

}