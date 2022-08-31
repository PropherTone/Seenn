package com.protone.ui.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.protone.api.baseType.getString
import com.protone.api.entity.NoteDir
import com.protone.ui.R
import com.protone.ui.databinding.NoteTpyeListAdapterBinding
import com.protone.worker.database.DatabaseHelper

class NoteTypeListAdapter(
    context: Context,
) : BaseAdapter<NoteTpyeListAdapterBinding, Any>(context) {

    private val noteDirList = arrayListOf<NoteDir>()

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
                    val noteType = noteDirList[position]
                    DatabaseHelper.instance.execute {
                        DatabaseHelper.instance.noteDirDAOBridge.doDeleteNoteDirRs(noteType)
                    }
                    notifyItemRemoved(position)
                    dialog.dismiss()
                }.setNegativeButton(R.string.cancel.getString()) { dialog, _ ->
                    dialog.dismiss()
                }.setTitle(R.string.delete.getString()).create().show()
                return@setOnLongClickListener false
            }
            noteTypeName.text = noteDirList[holder.layoutPosition].name
            noteTypeAddNote.setOnClickListener {
                addNote?.invoke(noteTypeName.text.toString())
            }
        }
    }

    override fun getItemCount(): Int = noteDirList.size

    fun insertNoteDir(noteDir: NoteDir) {
        noteDirList.add(noteDir)
        notifyItemInserted(noteDirList.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNoteTypeList(list: List<NoteDir>) {
        noteDirList.clear()
        noteDirList.add(
            NoteDir(
                context.getString(
                    R.string.all
                ), ""
            )
        )
        noteDirList.addAll(list)
        notifyDataSetChanged()
    }

}