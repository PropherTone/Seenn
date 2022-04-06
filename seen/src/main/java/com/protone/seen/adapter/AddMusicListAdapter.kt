package com.protone.seen.adapter

import android.content.Context
import android.view.ViewGroup
import com.protone.database.room.entity.Music
import com.protone.seen.databinding.AddMusicAdapterLayoutBinding

class AddMusicListAdapter(context: Context) :
    SelectListAdapter<AddMusicAdapterLayoutBinding, Music>(context) {

    var musicList = mutableListOf<Music>()
        set(value) {
            field.clear()
            field.addAll(value)
        }

    override val select: (holder: Holder<AddMusicAdapterLayoutBinding>, isSelect: Boolean) -> Unit
        get() = TODO("Not yet implemented")

    override fun itemIndex(path: Music): Int = musicList.indexOf(path)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<AddMusicAdapterLayoutBinding> {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: Holder<AddMusicAdapterLayoutBinding>, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }
}