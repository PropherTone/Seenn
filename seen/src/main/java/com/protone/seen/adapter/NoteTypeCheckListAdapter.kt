package com.protone.seen.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.protone.seen.databinding.MainLayoutBinding

class NoteTypeCheckListAdapter(
    val context: Context,
    list: List<String>
) : BaseCheckListAdapter<MainLayoutBinding,String>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder<MainLayoutBinding> {
        return Holder(MainLayoutBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: Holder<MainLayoutBinding>, position: Int) {
//        holder.binding.asdasd
    }

    override val setOnCheck: (Boolean) -> Unit
        get() = TODO("Not yet implemented")
}