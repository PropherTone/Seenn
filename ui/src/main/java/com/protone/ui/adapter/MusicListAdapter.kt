package com.protone.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.core.view.isGone
import com.protone.api.TAG
import com.protone.api.baseType.toStringMinuteTime
import com.protone.api.context.newLayoutInflater
import com.protone.api.entity.Music
import com.protone.ui.R
import com.protone.ui.databinding.MusicListLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicListAdapter(context: Context) :
    SelectListAdapter<MusicListLayoutBinding, Music, Any>(context) {

    var musicList: MutableList<Music> = mutableListOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field.clear()
            field.addAll(value)
            if (selectList.size >= 1) playPosition = field.indexOf(selectList[0])
            launch {
                notifyDataSetChanged()
            }
        }

    var clickCallback: ((Music) -> Unit?)? = null

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
                    musicListDetail,
                    dispatch = false
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
                context.newLayoutInflater,
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
                    itemClickChange(
                        R.color.blue_2,
                        R.color.white,
                        musicListContainer,
                        musicListInContainer,
                        arrayOf(
                            musicListName,
                            musicListTime,
                            musicListDetail
                        ),
                        true
                    )
                    clickCallback?.invoke(music)
                    launch(Dispatchers.Default) {
                        delay(1000)
                        if (!selectList.contains(music)) {
                            withContext(Dispatchers.Main) {
                                setSelect(holder, false)
                            }
                        }
                    }
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
        launch(Dispatchers.Default) {
            if (musicList.size <= 0) return@launch
            if (musicList.contains(music)) {
                selectList.clear()
                selectList.add(music)
                withContext(Dispatchers.Main) {
                    notifyItemChanged(playPosition)
                }
                playPosition = musicList.indexOf(music)
                withContext(Dispatchers.Main) {
                    notifyItemChanged(playPosition)
                }
            } else {
                playPosition = -1
                val iterator = selectList.iterator()
                while (iterator.hasNext()) {
                    val index = selectList.indexOf(iterator.next())
                    iterator.remove()
                    withContext(Dispatchers.Main) {
                        notifyItemChanged(index)
                    }
                }
            }
        }
    }

}