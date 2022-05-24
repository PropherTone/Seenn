package com.protone.seen.dialog

import android.app.AlertDialog
import android.content.Context
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.CateDialogLayoutBinding

class CateDialog(
    val context: Context,
    private val addCate: () -> Unit,
    private val addCon: () -> Unit
) {

    private val binding by lazy {
        CateDialogLayoutBinding.inflate(
            context.layoutInflater,
            context.root,
            false
        )
    }

    init {
        AlertDialog.Builder(context).setView(binding.root).create().also { dialog ->
            binding.btnAddCate.setOnClickListener {
                dialog.dismiss()
                addCate.invoke()
            }
            binding.btnAddCon.setOnClickListener {
                dialog.dismiss()
                addCon.invoke()
            }
        }.show()
    }
}