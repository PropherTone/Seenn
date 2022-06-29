package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.core.view.isGone
import com.protone.api.context.layoutInflater
import com.protone.api.toStringMinuteTime
import com.protone.database.room.entity.Music
import com.protone.seen.databinding.MusicListLayoutBinding

class MusicListAdapter(context: Context) :
    SelectListAdapter<MusicListLayoutBinding, Music>(context) {

    var musicList: MutableList<Music> = mutableListOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field.clear()
            field.addAll(value)
            if (selectList.size >= 1) playPosition = field.indexOf(selectList[0])
            notifyDataSetChanged()
        }

    var clickCallback: (Music) -> Unit? = {}

    private var playPosition = -1

    override val select: (holder: Holder<MusicListLayoutBinding>, isSelect: Boolean) -> Unit =
        { holder, isSelect ->
            holder.binding.apply {
                clickAnimation(
                    isSelect,
                    musicListContainer,
                    musicListInContainer,
                    musicListPlayState,
                    musicListName,
                    musicListTime,
                    musicListDetail
                )
            }
        }

    override fun itemIndex(path: Music): Int = musicList.indexOf(path)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<MusicListLayoutBinding> {
        return Holder(
            MusicListLayoutBinding.inflate(
                context.layoutInflater,
                parent,
                false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder<MusicListLayoutBinding>, position: Int) {
        holder.binding.apply {
            musicList[position].let { music ->
                setSelect(holder, selectList.contains(music))
                musicListContainer.setOnClickListener {
                    if (playPosition == holder.layoutPosition) return@setOnClickListener
                    checkSelect(holder, music)
                    playPosition = holder.layoutPosition
                    clickCallback(music)
                }
                musicListName.text = music.title
                if (music.artist != null && music.album != null) {
                    musicListDetail.text = "${music.artist} · ${music.album}"
                } else musicListDetail.isGone = true
                musicListTime.text = music.duration.toStringMinuteTime()
            }
        }
    }

    override fun getItemCount(): Int = musicList.size

    fun playPosition(music: Music) {
        if (musicList.size <= 0) return
        if (musicList.contains(music)) {
            selectList.clear()
            selectList.add(music)
            notifyItemChanged(playPosition)
            playPosition = musicList.indexOf(music)
            notifyItemChanged(playPosition)
        } else {
            playPosition = -1
        }
    }

}