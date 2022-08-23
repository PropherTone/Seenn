package com.protone.ui.adapter

import android.content.Context
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.protone.api.context.newLayoutInflater
import com.protone.api.entity.GalleyMedia
import com.protone.ui.databinding.GalleyVp2AdapterLayoutBinding
import kotlinx.coroutines.launch

class GalleyViewPager2Adapter(context: Context, private val data: MutableList<GalleyMedia>) :
    BaseAdapter<GalleyVp2AdapterLayoutBinding, Any>(context) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<GalleyVp2AdapterLayoutBinding> =
        Holder(GalleyVp2AdapterLayoutBinding.inflate(context.newLayoutInflater, parent, false))

    override fun onBindViewHolder(holder: Holder<GalleyVp2AdapterLayoutBinding>, position: Int) {
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