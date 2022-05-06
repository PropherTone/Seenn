package com.protone.seen.dialog

import android.app.AlertDialog
import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.adapter.CheckListAdapter
import com.protone.seen.databinding.ListPopWindowsLayoutBinding

class CheckListDialog(
    val context: Context,
    private val dataList: MutableList<String>,
    private val callBack: (String?) -> Unit
) {

    init {
        val binding = ListPopWindowsLayoutBinding.inflate(
            context.layoutInflater,
            context.root,
            false
        )
        val create = AlertDialog.Builder(context).setView(binding.root).create()
        binding.apply {
            listList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = CheckListAdapter(context, dataList)
            }
            listConfirm.setOnClickListener {
                listList.adapter.let {
                    if (it is CheckListAdapter)
                        callBack.invoke(if (it.selectList.size > 0) it.selectList[0] else null)
                }
                create.dismiss()
            }
        }
        create.show()
    }

}