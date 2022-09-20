package com.protone.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Animatable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isGone
import com.protone.api.animation.AnimationHelper
import com.protone.api.baseType.getDrawable
import com.protone.api.baseType.toStringMinuteTime
import com.protone.api.context.newLayoutInflater
import com.protone.api.entity.Music
import com.protone.ui.R
import com.protone.ui.databinding.MusicListLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AddMusicListAdapter(
    context: Context,
    val bucket: String,
    val mode: String,
    val adapterDataBaseProxy: AddMusicListAdapterDataProxy
) : SelectListAdapter<MusicListLayoutBinding, Music, Any>(context) {

    init {
        multiChoose = mode == "ADD"
        if (multiChoose) launch(Dispatchers.Default) {
            selectList.addAll(adapterDataBaseProxy.getMusicWithMusicBucket(bucket))
            selectList.forEach {
                musicList.indexOf(it).let { index ->
                    if (index != -1) withContext(Dispatchers.Main) { notifyItemChanged(index) }
                }
            }
        }
    }

    var musicList = mutableListOf<Music>()
        set(value) {
            field.clear()
            field.addAll(value)
        }

    private val viewQueue = PriorityQueue<Int>()

    private var onBusy = false

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
                    backgroundColor = R.color.transparent_black1,
                    backgroundColorPressed = R.color.transparent_black,
                    textsColor = R.color.white
                )
                if (isSelect) {
                    musicListPlayState.setImageDrawable(R.drawable.load_animation.getDrawable())
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
        ).apply {
            binding.isLoad = true
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder<MusicListLayoutBinding>, position: Int) {
        holder.binding.apply {
            musicList[position].also { music ->
                setSelect(holder, music in selectList)
                musicListContainer.setOnClickListener {
                    if (onBusy) return@setOnClickListener
                    onBusy = true
                    launch(Dispatchers.Default) {
                        if (mode == "SEARCH") {
                            adapterDataBaseProxy.play(music)
                            withContext(Dispatchers.Main) {
                                checkSelect(holder, music)
                            }
                            onBusy = false
                            return@launch
                        }
                        viewQueue.add(position)
                        if (selectList.contains(music)) {
                            if (!multiChoose) return@launch
                            adapterDataBaseProxy.deleteMusicWithMusicBucket(
                                music.musicBaseId,
                                bucket
                            )
                            withContext(Dispatchers.Main) {
                                checkSelect(holder, music)
                            }
                            onBusy = false
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            checkSelect(holder, music)
                        }
                        musicListPlayState.drawable.let { d ->
                            when (d) {
                                is Animatable -> {
                                    withContext(Dispatchers.Main) {
                                        d.start()
                                    }
                                    if (multiChoose) {
                                        val re = adapterDataBaseProxy
                                            .insertMusicWithMusicBucket(music.musicBaseId, bucket)
                                        if (re != -1L) {
                                            changeIconAni(musicListPlayState)
                                        } else {
                                            selectList.remove(music)
                                            notifyItemChanged()
                                        }
                                    } else {
                                        changeIconAni(musicListPlayState)
                                    }
                                    d.stop()
                                }
                                else -> {
                                    selectList.remove(music)
                                    notifyItemChanged()
                                }
                            }
                        }
                        onBusy = false
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

    private suspend fun notifyItemChanged() {
        while (!viewQueue.isEmpty()) {
            val poll = viewQueue.poll()
            if (poll != null) {
                withContext(Dispatchers.Main) {
                    notifyItemChanged(poll)
                }
            }
        }
    }

    private suspend fun changeIconAni(view: ImageView) {
        withContext(Dispatchers.Main) {
            AnimationHelper.apply {
                animatorSet(scaleX(view, 0f), scaleY(view, 0f), doOnEnd = {
                    view.setImageDrawable(R.drawable.ic_baseline_check_24_white.getDrawable())
                    animatorSet(scaleX(view, 1f), scaleY(view, 1f), play = true)
                }, play = true)
            }
        }
    }

    override fun getItemCount(): Int = musicList.size

    interface AddMusicListAdapterDataProxy {
        suspend fun getMusicWithMusicBucket(bucket: String): Collection<Music>
        suspend fun insertMusicWithMusicBucket(musicBaseId: Long, bucket: String): Long
        fun deleteMusicWithMusicBucket(musicBaseId: Long, musicBucket: String)
        fun play(music: Music)
    }
}