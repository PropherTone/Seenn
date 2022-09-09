package com.protone.ui.adapter

import android.content.Context
import android.view.ViewGroup
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
import com.protone.worker.photoInToday
import com.protone.worker.randomNote
import com.protone.worker.videoInToday

class MainModelListAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        loadDataBelow()
    }

    private fun loadDataBelow() {
        photoInToday()?.toJson()?.let { itemList.add("photo:$it") }
        notifyItemInserted(itemList.size - 1)
        videoInToday()?.toJson()?.let { itemList.add("video:$it") }
        notifyItemInserted(itemList.size - 1)
        randomNote()?.toJson()?.let { itemList.add("tNote:$it") }
        notifyItemInserted(itemList.size - 1)
//            musicInToday().toJson().let { itemList.add("music:$it") }
//            notifyItemInserted(itemList.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            MUSIC -> return MusicPlayerViewHolder(
                MusicCardBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
            )
            NOTE -> return NoteCardViewHolder(
                NoteCardBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
            )
            VIDEO -> return VideoPlayerViewHolder(
                VideoCardBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
            )
            PHOTO -> return PhotoCardViewHolder(
                PhotoCardBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
            )
            TIME -> return DateViewHolder(
                DateLayoutBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
            )
            else -> return DateViewHolder(
                DateLayoutBinding.inflate(
                    context.newLayoutInflater,
                    parent,
                    false
                )
            ).apply { binding.modelTime.text = context.getString(R.string.bruh) }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MusicPlayerViewHolder -> {
                val music = itemList[position].substring(6).toEntity(Music::class.java)
            }
            is NoteCardViewHolder -> {
                val note = itemList[position].substring(6).toEntity(Note::class.java)
                Glide.with(context).asDrawable().load(note.imagePath)
                    .into(holder.binding.modelNoteIcon)
                holder.binding.modelNoteTitle.text = note.title
            }
            is PhotoCardViewHolder -> {
                val media = itemList[position].substring(6).toEntity(GalleyMedia::class.java)
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
            is VideoPlayerViewHolder -> {
                val media = itemList[position].substring(6).toEntity(GalleyMedia::class.java)
                holder.binding.videoPlayer.apply {
                    setVideoPath(media.uri)
                    setFullScreen {
                        modelClkListener?.onVideo(media.toJson())
                    }
                }
                holder.binding.videoCardTitle.text =
                    media.date.toDateString("yyyy/MM/dd E").toString()
            }
            is DateViewHolder -> holder.binding.modelTime.text = itemList[position].substring(6)
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

    class MusicPlayerViewHolder(val binding: MusicCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    class NoteCardViewHolder(val binding: NoteCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    class PhotoCardViewHolder(val binding: PhotoCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    class VideoPlayerViewHolder(val binding: VideoCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    class DateViewHolder(val binding: DateLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)
}