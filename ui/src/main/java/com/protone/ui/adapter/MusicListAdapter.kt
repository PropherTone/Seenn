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
import kotlinx.coroutines.withContext

class MusicListAdapter(context: Context, private val musicList: MutableList<Music>) :
    SelectListAdapter<MusicListLayoutBinding, Music, MusicListAdapter.MusicListEvent>(
        context,
        true
    ) {

    sealed class MusicListEvent {
        data class PlayPosition(val music: Music) : MusicListEvent()
        data class InsertMusics(val musics: Collection<Music>) : MusicListEvent()
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

    override suspend fun onEventIO(data: MusicListEvent) {
        when (data) {
            is MusicListEvent.PlayPosition -> {
                if (musicList.size <= 0) return
                if (musicList.contains(data.music)) {
                    selectList.clear()
                    selectList.add(data.music)
                    playPosition = musicList.indexOf(data.music)
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "onEventIO PlayPosition: $playPosition")
                        notifyItemChanged(playPosition)
                    }
                }
            }
            is MusicListEvent.InsertMusics -> {
                if (data.musics.isEmpty()) return
                val oldSize = musicList.size - 1
                musicList.addAll(data.musics)
                withContext(Dispatchers.Main){
                    notifyItemRangeInserted(oldSize,musicList.size -1)
                }
            }
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
                    if (selectList.contains(music)) return@setOnClickListener
                    if (playPosition == holder.layoutPosition) return@setOnClickListener
                    clearSelected()
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

    fun getPlayingPosition(): Int {
        if (musicList.size <= 0) return -1
        return musicList.indexOf(selectList.getOrNull(0) ?: musicList[0])
    }

    fun getPlayingMusic(): Music? = selectList.getOrNull(0)

    fun getPlayList() = musicList.toMutableList()

    fun playPosition(music: Music) {
        emit(MusicListEvent.PlayPosition(music))
    }

    fun insertMusics(musics: Collection<Music>) {
        emit(MusicListEvent.InsertMusics(musics))
    }

}