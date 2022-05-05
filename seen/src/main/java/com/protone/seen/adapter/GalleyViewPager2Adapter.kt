package com.protone.seen.adapter

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.protone.api.context.layoutInflater
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.databinding.GalleyVp2AdapterLayoutBinding
import com.protone.seen.databinding.RichVideoLayoutBinding

class GalleyViewPager2Adapter(
    val context: Context,
    private val data: MutableList<GalleyMedia>,
    private val isVideo: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = if (isVideo) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == 1) {
            VideoHolder(RichVideoLayoutBinding.inflate(context.layoutInflater, parent, false))
        } else {
            PhotoHolder(
                GalleyVp2AdapterLayoutBinding.inflate(
                    context.layoutInflater,
                    parent,
                    false
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (isVideo && holder is VideoHolder) {
            holder.binding.richVideo.setVideoPath(data[position].uri)
        } else if (holder is PhotoHolder) {
            Glide.with(context)
                .asDrawable()
                .load(data[position].uri)
                .skipMemoryCache(true)
                .into(holder.binding.root as ImageView)
            holder.binding.root.setOnClickListener {
                onClk?.invoke()
            }
        }
    }

    var onClk: (() -> Unit)? = null

    override fun getItemCount(): Int = data.size
    class VideoHolder(val binding: RichVideoLayoutBinding) : RecyclerView.ViewHolder(binding.root)
    class PhotoHolder(val binding: GalleyVp2AdapterLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

}
