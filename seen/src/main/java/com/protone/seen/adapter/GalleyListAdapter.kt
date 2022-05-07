package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.protone.api.Config
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.databinding.PictureBoxAdapterLayoutBinding

class GalleyListAdapter(
    context: Context,
    private val media: MutableList<GalleyMedia>,
    private val isVideo: Boolean = false,
    private val onSelect : (Boolean)->Unit = {}
) : SelectListAdapter<PictureBoxAdapterLayoutBinding, GalleyMedia>(context) {

    private var itemLength = 0
    private var onSelectMod = false

    override val select: (Holder<PictureBoxAdapterLayoutBinding>, Boolean) -> Unit =
        { holder, select ->
            holder.binding.apply {
                checkSeen.isVisible = select
                checkCheck.isChecked = select
            }
        }

    override fun itemIndex(path: GalleyMedia): Int {
        return media.indexOf(path)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        itemLength = Config.screenWidth / 4 - recyclerView.paddingEnd
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<PictureBoxAdapterLayoutBinding> {
        return Holder(PictureBoxAdapterLayoutBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        ).apply {
            imageView.updateLayoutParams {
                width = this@GalleyListAdapter.itemLength
                height = width
            }
            checkSeen.updateLayoutParams {
                width = this@GalleyListAdapter.itemLength
                height = width
            }
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            videoIcon.isGone = !isVideo
        })
    }


    override fun onBindViewHolder(holder: Holder<PictureBoxAdapterLayoutBinding>, position: Int) {
        setSelect(holder, selectList.contains(media[position]))
        holder.binding.imageView.let { image ->
            Glide.with(context).load(media[position].thumbnailUri).into(image)
            image.setOnClickListener {
                if (onSelectMod) {
                    checkSelect(holder, media[position])
                } else onSelectListener?.openView(media[position])
            }
            image.setOnLongClickListener {
                onSelectMod = true
                onSelect(onSelectMod)
                checkSelect(holder, media[position])
                true
            }
        }
    }

    override fun checkSelect(holder: Holder<PictureBoxAdapterLayoutBinding>, item: GalleyMedia) {
        onSelectListener?.select(selectList)
        super.checkSelect(holder, item)
    }

    fun quitSelectMod(){
        onSelectMod = false
        onSelect(onSelectMod)
        clearAllSelected()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun noticeDataUpdate(item: MutableList<GalleyMedia>) {
        media.clear()
        media.addAll(item)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll(){
        selectList.addAll(media)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return media.size
    }

    private var onSelectListener: OnSelect? = null

    interface OnSelect{
        fun select(galleyMedia: MutableList<GalleyMedia>)
        fun openView(galleyMedia: GalleyMedia)
    }

    fun setOnSelectListener(listener: OnSelect?) {
        this.onSelectListener = listener
    }
}