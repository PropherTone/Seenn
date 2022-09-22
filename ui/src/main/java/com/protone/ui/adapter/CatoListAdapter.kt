package com.protone.ui.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.bumptech.glide.Glide
import com.protone.api.context.newLayoutInflater
import com.protone.api.entity.GalleyMedia
import com.protone.ui.databinding.ImageCateLayoutBinding
import com.protone.ui.databinding.TextCateLayoutBinding

class CatoListAdapter(context: Context, val catoListDataProxy: CatoListDataProxy) :
    BaseAdapter<ViewDataBinding, String>(context) {

    private val catoList = mutableListOf<String>()
    private var itemClick: ((String) -> Unit)? = null

    override fun getItemViewType(position: Int): Int {
        return if (catoList[position].contains("content://")) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder<ViewDataBinding> =
        Holder(
            when (viewType) {
                1 -> ImageCateLayoutBinding.inflate(context.newLayoutInflater, parent, false)
                else -> TextCateLayoutBinding.inflate(context.newLayoutInflater, parent, false)
            }
        )


    override fun onBindViewHolder(holder: Holder<ViewDataBinding>, position: Int) {
        holder.binding.apply {
            when (this) {
                is ImageCateLayoutBinding -> {
                    val media = catoListDataProxy.getMedia()
                    Glide.with(context).asDrawable().load(media.uri).into(catoBack)
                    catoName.text = media.name
                    root.setOnClickListener {
                        itemClick?.invoke(catoList[position])
                    }
                }
                is TextCateLayoutBinding -> {
                    cato.text = catoList[position]
                }
            }
        }
    }

    override fun getItemCount(): Int = catoList.size

    fun setItemClick(itemClick: (String) -> Unit) {
        this.itemClick = itemClick
    }

    interface CatoListDataProxy {
        fun getMedia(): GalleyMedia
    }

}