package com.protone.seen.adapter

import android.content.Context
import android.view.ViewGroup
import com.protone.api.context.newLayoutInflater
import com.protone.api.entity.Music
import com.protone.seen.R
import com.protone.seen.databinding.TpPlaylistAdapterLayoutBinding

class TransparentPlayListAdapter(
    context: Context,
    onPlay: Music?,
    private val playList: MutableList<Music>
) : SelectListAdapter<TpPlaylistAdapterLayoutBinding, Music,Any>(context) {
    override val select: (holder: Holder<TpPlaylistAdapterLayoutBinding>, isSelect: Boolean) -> Unit =
        { holder, isSelect ->
            if (isSelect) {
                holder.binding.playListName.setBackgroundResource(R.drawable.round_background_tans_white_lite)
            } else {
                holder.binding.playListName.setBackgroundResource(R.drawable.round_background_fore_dark)
            }
        }

    init {
        onPlay?.let { selectList.add(it) }
    }

    override fun itemIndex(path: Music): Int = playList.indexOf(path)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<TpPlaylistAdapterLayoutBinding> {
        val binding = TpPlaylistAdapterLayoutBinding.inflate(context.newLayoutInflater, parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder<TpPlaylistAdapterLayoutBinding>, position: Int) {
        playList[position].let { music ->
            setSelect(holder, selectList.contains(music))
            holder.binding.playListName.text = music.title
            holder.binding.playListName.setOnClickListener {
                checkSelect(holder, music)
                onPlayListClkListener?.onClk(music)
            }
        }
    }

    override fun getItemCount(): Int = playList.size

    var onPlayListClkListener: OnPlayListClk? = null

    interface OnPlayListClk {
        fun onClk(music: Music)
    }
}