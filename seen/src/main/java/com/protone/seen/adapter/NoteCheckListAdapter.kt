package com.protone.seen.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.protone.seen.databinding.MainLayoutBinding

class NoteCheckListAdapter(context: Context, list: List<String>) : BaseCheckListAdapter<MainLayoutBinding,String>(list) {

    var inflater: LayoutInflater = LayoutInflater.from(context)

    lateinit var binding: MainLayoutBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder<MainLayoutBinding> {
        binding = MainLayoutBinding.inflate(inflater,parent,false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder<MainLayoutBinding>, position: Int) {}

    override val setOnCheck: (Boolean) -> Unit
        get() = {
        }
}