package com.protone.ui.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.protone.api.baseType.toDateString
import com.protone.api.context.newLayoutInflater
import com.protone.api.entity.GalleyMedia
import com.protone.api.entity.Music
import com.protone.api.entity.Note
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.ui.R
import com.protone.ui.databinding.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainModelListAdapter(
    context: Context,
    private val mainModelListAdapterDataProxy: MainModelListAdapterDataProxy
) : BaseAdapter<ViewDataBinding, RecyclerView.ViewHolder>(context, false) {

    private val itemList = mutableListOf<String>()

    companion object {
        const val TIME = 0x1F
        const val MUSIC = 0x2F
        const val PHOTO = 0x3F
        const val VIDEO = 0x4F
        const val NOTE = 0x5F
        const val UNKNOWN = 0x6F
    }

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position].substring(0, 5)) {
            "tTime" -> TIME
            "music" -> MUSIC
            "photo" -> PHOTO
            "video" -> VIDEO
            "tNote" -> NOTE
            else -> UNKNOWN
        }
    }

    suspend fun loadDataBelow() {
        mainModelListAdapterDataProxy.run {
            photoInTodayJson()?.let {
                itemList.add("photo:${it}")
                withContext(Dispatchers.Main) {
                    notifyItemInserted(itemList.size - 1)
                }
            }
            videoInTodayJson()?.let {
                itemList.add("video:${it}")
                withContext(Dispatchers.Main) {
                    notifyItemInserted(itemList.size - 1)
                }
            }
            randomNoteJson()?.let {
                itemList.add("tNote:${it}")
                withContext(Dispatchers.Main) {
                    notifyItemInserted(itemList.size - 1)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder<ViewDataBinding> {
        return Holder(
            when (viewType) {
                MUSIC -> MusicCardBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
                NOTE -> NoteCardBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
                VIDEO -> VideoCardBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
                PHOTO -> PhotoCardBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
                TIME -> DateLayoutBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
                else -> DateLayoutBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                ).apply { modelTime.text = context.getString(R.string.bruh) }
            }
        )
    }

    override fun onBindViewHolder(holder: Holder<ViewDataBinding>, position: Int) {
        when (holder.binding) {
            is MusicCardBinding -> {
//                val music = itemList[position].substring(6).toEntity(Music::class.java)
            }
            is NoteCardBinding -> {
                val note = itemList[position].substring(6).toEntity(Note::class.java)
                Glide.with(context).asDrawable().load(note.imagePath)
                    .into(holder.binding.modelNoteIcon)
                holder.binding.modelNoteTitle.text = note.title
            }
            is PhotoCardBinding -> {
                val media =
                    itemList[position].substring(6).toEntity(GalleyMedia::class.java)
                holder.binding.photoCard.apply {
                    title = media.date.toDateString("yyyy/MM/dd E").toString()
                    photo.let {
                        Glide.with(context).load(media.uri).into(it)
                    }
                    setOnClickListener {
                        modelClkListener?.onPhoto(media.toJson())
                    }
                }
            }
            is VideoCardBinding -> {
                val media =
                    itemList[position].substring(6).toEntity(GalleyMedia::class.java)
                holder.binding.videoPlayer.apply {
                    setVideoPath(media.uri)
                    setFullScreen {
                        modelClkListener?.onVideo(media.toJson())
                    }
                }
                holder.binding.videoCardTitle.text =
                    media.date.toDateString("yyyy/MM/dd E").toString()
            }
            is DateLayoutBinding -> holder.binding.modelTime.text =
                itemList[position].substring(6)
        }
    }

    var modelClkListener: ModelClk? = null

    interface ModelClk {
        fun onPhoto(json: String)
        fun onNote(json: String)
        fun onVideo(json: String)
        fun onMusic(json: String)
    }

    override fun getItemCount(): Int = itemList.size

    interface MainModelListAdapterDataProxy {
        fun photoInTodayJson(): String?
        fun videoInTodayJson(): String?
        fun randomNoteJson(): String?
    }

}