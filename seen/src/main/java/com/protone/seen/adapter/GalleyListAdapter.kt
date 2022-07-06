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
import com.protone.api.context.APP
import com.protone.api.context.onUiThread
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.databinding.PictureBoxAdapterLayoutBinding

class GalleyListAdapter(
    context: Context,
    private val media: MutableList<GalleyMedia>,
    private val isVideo: Boolean = false,
    private val useSelect: Boolean = true
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
        itemLength = APP.screenWidth / 4 - recyclerView.paddingEnd
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
                    onSelectListener?.select(media[position])
                    onSelectListener?.select(selectList)
                } else onSelectListener?.openView(media[position])
            }
            if (useSelect) {
                image.setOnLongClickListener {
                    onSelectMod = true
                    checkSelect(holder, media[position])
                    onSelectListener?.select(media[position])
                    onSelectListener?.select(selectList)
                    true
                }
            }
        }
    }

    fun quitSelectMod() {
        if (!onSelectMod) return
        onSelectMod = false
        context.onUiThread {
            clearAllSelected()
        }
        onSelectListener?.select(selectList)
    }

    fun noticeSelectChange(item: GalleyMedia) {
        val indexOf = media.indexOf(item)
        if (indexOf != -1) {
            onSelectMod = true
            notifyItemChanged(indexOf)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun noticeDataUpdate(item: MutableList<GalleyMedia>?) {
        if (item == null) return
        media.clear()
        media.addAll(item)
        context.onUiThread {
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll() {
        media.onEach {
            if (!selectList.contains(it)) {
                selectList.add(it)
                context.onUiThread {
                    notifyItemChanged(media.indexOf(it))
                }
            }
        }
    }

    fun removeMedia(galleyMedia: GalleyMedia) {
        val index = media.indexOf(galleyMedia)
        if (index != -1) {
            media.removeAt(index)
            if (selectList.contains(galleyMedia)) selectList.remove(galleyMedia)
            notifyItemRemoved(index)
        }
    }

    override fun getItemCount(): Int {
        return media.size
    }

    private var onSelectListener: OnSelect? = null

    interface OnSelect {
        fun select(galleyMedia: GalleyMedia)
        fun select(galleyMedia: MutableList<GalleyMedia>)
        fun openView(galleyMedia: GalleyMedia)
    }

    fun setNewSelectList(list: MutableList<GalleyMedia>) {
        selectList = list
    }

    fun setOnSelectListener(listener: OnSelect?) {
        this.onSelectListener = listener
    }
}