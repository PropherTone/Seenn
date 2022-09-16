package com.protone.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.protone.api.context.SApplication
import com.protone.api.entity.GalleyMedia
import com.protone.ui.databinding.GalleyListAdapterLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleyListAdapter(
    context: Context,
    private val useSelect: Boolean = true,
    private val combine: Boolean = false
) : SelectListAdapter<GalleyListAdapterLayoutBinding, GalleyMedia, GalleyListAdapter.GalleyListEvent>(
    context, true
) {
    private val medias: MutableList<GalleyMedia> = mutableListOf()

    enum class MediaStatus{
        UPDATED,
        INSERTED,
        DELETED
    }

    sealed class GalleyListEvent {
        object SelectAll : GalleyListEvent()
        object QuiteSelectAll : GalleyListEvent()
        data class NoticeDataUpdate(val item: MutableList<GalleyMedia>?) : GalleyListEvent()
        data class NoticeSelectChange(val item: GalleyMedia) : GalleyListEvent()
        data class RemoveMedia(val galleyMedia: GalleyMedia) : GalleyListEvent()
        data class NoticeListItemUpdate(val media: GalleyMedia) : GalleyListEvent()
        data class NoticeListItemDelete(val media: GalleyMedia) : GalleyListEvent()
        data class NoticeListItemInsert(val media: GalleyMedia) : GalleyListEvent()
    }

    fun setMedias(list: MutableList<GalleyMedia>) {
        medias.addAll(list)
    }

    private var itemLength = 0
    private var onSelectMod = false

    @SuppressLint("NotifyDataSetChanged")
    override suspend fun onEventIO(data: GalleyListEvent) {
        when (data) {
            is GalleyListEvent.QuiteSelectAll -> {
                if (!onSelectMod) return
                onSelectMod = false
                withContext(Dispatchers.Main) {
                    clearAllSelected()
                    onSelectListener?.select(selectList)
                }
            }
            is GalleyListEvent.SelectAll -> {
                medias.onEach {
                    if (!selectList.contains(it)) {
                        selectList.add(it)
                        withContext(Dispatchers.Main) { notifyItemChanged(medias.indexOf(it)) }
                    }
                }
            }
            is GalleyListEvent.NoticeDataUpdate -> {
                if (data.item == null) return
                medias.clear()
                medias.addAll(data.item)
                withContext(Dispatchers.Main) {
                    notifyDataSetChanged()
                }
            }
            is GalleyListEvent.NoticeSelectChange -> {
                val indexOf = medias.indexOf(data.item)
                if (indexOf != -1) {
                    onSelectMod = true
                    withContext(Dispatchers.Main) { notifyItemChanged(indexOf) }
                }
            }
            is GalleyListEvent.RemoveMedia -> {
                val index = medias.indexOf(data.galleyMedia)
                if (index != -1) {
                    medias.removeAt(index)
                    if (selectList.contains(data.galleyMedia)) selectList.remove(data.galleyMedia)
                    withContext(Dispatchers.Main) {
                        notifyItemRemoved(index)
                    }
                }
            }
            is GalleyListEvent.NoticeListItemUpdate -> {
                val index = medias.indexOf(data.media)
                if (index != -1) {
                    medias[index] = data.media
                    withContext(Dispatchers.Main) {
                        notifyItemChanged(index)
                    }
                }
            }
            is GalleyListEvent.NoticeListItemDelete -> {
                val index = medias.indexOf(data.media)
                if (index != -1) {
                    medias.removeAt(index)
                    withContext(Dispatchers.Main) {
                        notifyItemRemoved(index)
                    }
                }
            }
            is GalleyListEvent.NoticeListItemInsert -> {
                withContext(Dispatchers.Main) {
                    medias.add(0, data.media)
                    notifyItemInserted(0)
                }
            }
        }
    }

    override val select: (Holder<GalleyListAdapterLayoutBinding>, Boolean) -> Unit =
        { holder, select ->
            holder.binding.apply {
                checkSeen.isVisible = select
                checkCheck.isChecked = select
            }
        }

    override fun itemIndex(path: GalleyMedia): Int {
        return medias.indexOf(path)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        itemLength = SApplication.screenWidth / 4 - recyclerView.paddingEnd
        recyclerView.layoutAnimationListener = object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                recyclerView.suppressLayout(true)
            }

            override fun onAnimationEnd(animation: Animation?) {
                recyclerView.suppressLayout(false)
            }

            override fun onAnimationRepeat(animation: Animation?) = Unit

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<GalleyListAdapterLayoutBinding> {
        return Holder(GalleyListAdapterLayoutBinding.inflate(
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
        })
    }

    override fun onBindViewHolder(holder: Holder<GalleyListAdapterLayoutBinding>, position: Int) {
        setSelect(holder, selectList.contains(medias[position]))
        holder.binding.videoIcon.isGone = !medias[position].isVideo && !combine
        holder.binding.imageView.let { image ->
            Glide.with(context).load(medias[position].thumbnailUri).into(image)
            image.setOnClickListener {
                if (onSelectMod) {
                    checkSelect(holder, medias[position])
                    onSelectListener?.select(medias[position])
                    onSelectListener?.select(selectList)
                } else onSelectListener?.openView(medias[position])
            }
            if (useSelect) {
                image.setOnLongClickListener {
                    onSelectMod = true
                    checkSelect(holder, medias[position])
                    onSelectListener?.select(medias[position])
                    onSelectListener?.select(selectList)
                    true
                }
            }
        }
    }

    fun noticeDataUpdate(item: MutableList<GalleyMedia>?) {
        emit(GalleyListEvent.NoticeDataUpdate(item))
    }

    fun selectAll() {
        emit(GalleyListEvent.SelectAll)
    }

    fun quitSelectMod() {
        emit(GalleyListEvent.QuiteSelectAll)
    }

    fun noticeSelectChange(item: GalleyMedia) {
        emit(GalleyListEvent.NoticeSelectChange(item))
    }

    fun removeMedia(galleyMedia: GalleyMedia) {
        emit(GalleyListEvent.RemoveMedia(galleyMedia))
    }

    fun noticeListItemUpdate(media: GalleyMedia) {
        emit(GalleyListEvent.NoticeListItemUpdate(media))
    }

    fun noticeListItemDelete(media: GalleyMedia) {
        emit(GalleyListEvent.NoticeListItemDelete(media))
    }

    fun noticeListItemInsert(media: GalleyMedia) {
        emit(GalleyListEvent.NoticeListItemInsert(media))
    }

    override fun getItemCount(): Int {
        return medias.size
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