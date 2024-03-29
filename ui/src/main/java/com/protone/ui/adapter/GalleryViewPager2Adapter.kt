package com.protone.ui.adapter

import android.content.Context
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.protone.api.context.newLayoutInflater
import com.protone.api.entity.GalleryMedia
import com.protone.ui.databinding.GalleryVp2AdapterLayoutBinding
import kotlinx.coroutines.launch

class GalleryViewPager2Adapter(context: Context, private val data: MutableList<GalleryMedia>) :
    BaseAdapter<GalleryVp2AdapterLayoutBinding, Any>(context) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<GalleryVp2AdapterLayoutBinding> =
        Holder(GalleryVp2AdapterLayoutBinding.inflate(context.newLayoutInflater, parent, false))

    override fun onBindViewHolder(holder: Holder<GalleryVp2AdapterLayoutBinding>, position: Int) {
        holder.binding.apply {
            if (!data[position].name.contains("gif")) {
                image.setImageResource(data[position].uri)
            } else {
                Glide.with(context)
                    .asDrawable()
                    .load(data[position].uri)
                    .skipMemoryCache(true)
                    .into(image)
            }
            image.onSingleTap = {
                launch {
                    onClk?.invoke()
                }
            }
            root.setOnClickListener {
                launch {
                    onClk?.invoke()
                }
            }
        }
    }

    var onClk: (() -> Unit)? = null

    override fun getItemCount(): Int = data.size
}