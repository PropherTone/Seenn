package com.protone.seen.adapter

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.protone.api.context.layoutInflater
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.databinding.GalleyVp2AdapterLayoutBinding

class GalleyViewPager2Adapter(context: Context, private val data: MutableList<GalleyMedia>) :
    BaseAdapter<GalleyVp2AdapterLayoutBinding>(context) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<GalleyVp2AdapterLayoutBinding> =
        Holder(GalleyVp2AdapterLayoutBinding.inflate(context.layoutInflater, parent, false))

    override fun onBindViewHolder(holder: Holder<GalleyVp2AdapterLayoutBinding>, position: Int) {
        Glide.with(context)
            .asDrawable()
            .load(data[position].uri)
            .skipMemoryCache(true)
            .into(holder.binding.root as ImageView)
    }

    override fun getItemCount(): Int = data.size


}
