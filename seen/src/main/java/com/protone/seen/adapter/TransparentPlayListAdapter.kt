package com.protone.seen.adapter

import android.content.Context
import android.view.ViewGroup
import com.protone.api.context.layoutInflater
import com.protone.database.room.entity.Music
import com.protone.seen.databinding.TpPlaylistAdapterLayoutBinding

class TransparentPlayListAdapter(context: Context, private val playList: MutableList<Music>) :
    BaseAdapter<TpPlaylistAdapterLayoutBinding>(context) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<TpPlaylistAdapterLayoutBinding> {
        val binding = TpPlaylistAdapterLayoutBinding.inflate(context.layoutInflater, parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder<TpPlaylistAdapterLayoutBinding>, position: Int) {

    }

    override fun getItemCount(): Int = playList.size
}