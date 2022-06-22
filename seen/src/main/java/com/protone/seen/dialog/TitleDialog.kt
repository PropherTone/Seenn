package com.protone.seen.dialog

import android.app.Activity
import android.content.Context
import android.text.Editable
import androidx.appcompat.app.AlertDialog
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.api.context.setSoftInputStatuesListener
import com.protone.seen.R
import com.protone.seen.databinding.RenameDialogLayoutBinding

class TitleDialog(val context: Context, val title : String, val name: String, val callBack: (String) -> Unit) {

    private val binding by lazy {
        RenameDialogLayoutBinding.inflate(
            context.layoutInflater,
            context.root,
            false
        ).apply {
            this.renameTitle.text = title
            renameInput.text = Editable.Factory.getInstance().newEditable(name)
        }
    }

    init {
        var create: AlertDialog?
        AlertDialog.Builder(context).also {
            it.setView(binding.root)
            it.setPositiveButton(
                R.string.confirm
            ) { p0, _ ->
                callBack(binding.renameInput.text.toString())
                p0?.dismiss()
            }
            it.setNegativeButton(R.string.cancel, null)
        }.create().also {
            create = it
        }.show()
        val attributes = create?.window?.attributes
        val oldY = attributes?.y
        if (context is Activity) {
            context.setSoftInputStatuesListener { i, b ->
                if (b) {
                    attributes?.y = attributes?.y?.minus(binding.root.measuredHeight / 2)
                    create?.onWindowAttributesChanged(attributes)
                } else {
                    attributes?.y = oldY
                    create?.onWindowAttributesChanged(attributes)
                }
            }
        }
    }
}