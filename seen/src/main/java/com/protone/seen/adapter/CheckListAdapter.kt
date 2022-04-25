package com.protone.seen.adapter

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.protone.api.context.layoutInflater
import com.protone.seen.databinding.CheckListAdapterLayoutBinding

class CheckListAdapter(context: Context, private val dataList: MutableList<String>) :
    SelectListAdapter<CheckListAdapterLayoutBinding, String>(context) {

    init {
        multiChoose = false
    }

    override val select: (holder: Holder<CheckListAdapterLayoutBinding>, isSelect: Boolean) -> Unit =
        { holder, isSelect ->
            holder.binding.clCheck.isChecked = isSelect
        }

    override fun itemIndex(path: String): Int = dataList.indexOf(path)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<CheckListAdapterLayoutBinding> =
        Holder(CheckListAdapterLayoutBinding.inflate(context.layoutInflater, parent, false))

    override fun onBindViewHolder(holder: Holder<CheckListAdapterLayoutBinding>, position: Int) {
        setSelect(holder, selectList.contains(dataList[position]))
        holder.binding.apply {
            root.setOnClickListener { checkSelect(holder, dataList[position]) }
            clCheck.isClickable = false
            clName.text = dataList[position]
        }
    }

    override fun getItemCount(): Int = dataList.size
}