package com.protone.seen.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.R
import com.protone.seen.databinding.RenameDialogLayoutBinding

class RenameDialog(val context: Context, val name: String, val callBack: (String) -> Unit) {

    private val binding by lazy {
        RenameDialogLayoutBinding.inflate(
            context.layoutInflater,
            context.root,
            false
        ).apply { renameInput.text = Editable.Factory.getInstance().newEditable(name) }
    }

    init {
        AlertDialog.Builder(context).also {
            it.setView(binding.root)
            it.setPositiveButton(
                R.string.confirm
            ) { p0, _ ->
                callBack(binding.renameInput.text.toString())
                p0?.dismiss()
            }
            it.setNegativeButton(R.string.cancel, null)
        }.create().show()
    }
}