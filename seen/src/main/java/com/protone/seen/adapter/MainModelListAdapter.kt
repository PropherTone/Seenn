package com.protone.seen.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.protone.api.context.layoutInflater
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.todayTime
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Music
import com.protone.database.room.entity.Note
import com.protone.mediamodle.Galley
import com.protone.seen.R
import com.protone.seen.databinding.*

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

    fun loadDataBelow() {
        itemList.add("tTime:$todayTime")
        notifyItemInserted(itemList.size)
        Galley.apply {
            musicInToday?.toJson()?.let { itemList.add("music:$it") }
            notifyItemInserted(itemList.size)
            photoInToday?.toJson()?.let { itemList.add("photo:$it") }
            notifyItemInserted(itemList.size)
            videoInToday?.toJson()?.let { itemList.add("video:$it") }
            notifyItemInserted(itemList.size)
        }
    }

    fun loadDataUpside(){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            MUSIC -> return MusicPlayerViewHolder(
                MusicCardBinding.inflate(
                    context.layoutInflater,
                    parent,
                    false
                )
            )
            NOTE -> return NoteCardViewHolder(
                NoteCardBinding.inflate(
                    context.layoutInflater,
                    parent,
                    false
                )
            )
            VIDEO -> return VideoPlayerViewHolder(
                VideoCardBinding.inflate(
                    context.layoutInflater,
                    parent,
                    false
                )
            )
            PHOTO -> return PhotoCardViewHolder(
                PhotoCardBinding.inflate(
                    context.layoutInflater,
                    parent,
                    false
                )
            )
            TIME -> return DateViewHolder(
                DateLayoutBinding.inflate(
                    context.layoutInflater,
                    parent,
                    false
                )
            )
            else -> return DateViewHolder(
                DateLayoutBinding.inflate(
                    context.layoutInflater,
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
            }
            is PhotoCardViewHolder -> {
                val media = itemList[position].substring(6).toEntity(GalleyMedia::class.java)
                holder.binding.photoCard.apply {
                    title = media.name
                    photo.let {
                        Glide.with(it.context).load(media.uri).into(it)
                    }
                }
            }
            is VideoPlayerViewHolder -> {
                val media = itemList[position].substring(6).toEntity(GalleyMedia::class.java)
                holder.binding.videoPlayer.apply {
                    setVideoPath(media.uri)
                }
            }
            is DateViewHolder -> holder.binding.modelTime.text = itemList[position].substring(6)
        }
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