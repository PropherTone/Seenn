package com.protone.seen.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.protone.api.context.SApplication
import com.protone.api.entity.GalleyMedia
import com.protone.seen.databinding.PictureBoxAdapterLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleyListAdapter(
    context: Context,
    private val isVideo: Boolean = false,
    private val useSelect: Boolean = true
) : SelectListAdapter<PictureBoxAdapterLayoutBinding, GalleyMedia, GalleyListAdapter.GalleyListEvent>(
    context,true
) {
    private val medias: MutableList<GalleyMedia> = mutableListOf()

    sealed class GalleyListEvent {
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

    init {
        hasFixedSize = false
    }

    override suspend fun onEventIO(data: GalleyListEvent) {
        when (data) {
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
                medias.add(0, data.media)
                withContext(Dispatchers.Main) {
                    notifyItemInserted(0)
                }
            }
        }
    }

    override val select: (Holder<PictureBoxAdapterLayoutBinding>, Boolean) -> Unit =
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
        setSelect(holder, selectList.contains(medias[position]))
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
        launch(Dispatchers.IO) {
            if (item == null) return@launch
            val size = medias.size
            medias.clear()
            withContext(Dispatchers.Main) { notifyItemRangeRemoved(0, size) }
            medias.addAll(item)
            withContext(Dispatchers.Main) { notifyItemRangeInserted(0, medias.size) }
        }
    }

    fun selectAll() {
        launch(Dispatchers.IO) {
            medias.onEach {
                if (!selectList.contains(it)) {
                    selectList.add(it)
                    withContext(Dispatchers.Main) { notifyItemChanged(medias.indexOf(it)) }
                }
            }
        }
    }

    fun quitSelectMod() {
        if (!onSelectMod) return
        onSelectMod = false
        clearAllSelected()
        onSelectListener?.select(selectList)
    }

    fun noticeSelectChange(item: GalleyMedia) {
        launch {
            adapterFlow.emit(GalleyListEvent.NoticeSelectChange(item))
        }
    }

    fun removeMedia(galleyMedia: GalleyMedia) {
        launch {
            adapterFlow.emit(GalleyListEvent.RemoveMedia(galleyMedia))
        }
    }

    fun noticeListItemUpdate(media: GalleyMedia) {
        launch {
            adapterFlow.emit(GalleyListEvent.NoticeListItemUpdate(media))
        }
    }

    fun noticeListItemDelete(media: GalleyMedia) {
        launch {
            adapterFlow.emit(GalleyListEvent.NoticeListItemDelete(media))
        }
    }

    fun noticeListItemInsert(media: GalleyMedia) {
        launch {
            adapterFlow.emit(GalleyListEvent.NoticeListItemInsert(media))
        }
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