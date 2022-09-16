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
    private val bucket: String,
    private val multiSelect: Boolean,
    private val adapterDataBaseProxy: AddMusicListAdapterDataProxy
) : SelectListAdapter<MusicListLayoutBinding, Music, Any>(context) {

    init {
        multiChoose = multiSelect
        if (multiSelect) launch {
            selectList.addAll(adapterDataBaseProxy.getMusicWithMusicBucket(bucket))
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
                    musicListDetail
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
                setSelect(holder, selectList.contains(music))

                musicListContainer.setOnClickListener {
                    if (onBusy) return@setOnClickListener
                    onBusy = true
                    launch(Dispatchers.Default) {
                        viewQueue.add(position)
                        if (selectList.contains(music)) {
                            if (!multiSelect) return@launch
                            adapterDataBaseProxy.deleteMusicWithMusicBucket(
                                music.musicBaseId,
                                bucket
                            )
                            withContext(Dispatchers.Main) {
                                checkSelect(holder, music)
                            }
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
                                    if (multiSelect) {
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

    @SuppressLint("NotifyDataSetChanged")
    fun noticeDataUpdate(list: MutableList<Music>) {
        launch(Dispatchers.IO) {
            musicList.clear()
            musicList.addAll(list)
            withContext(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int = musicList.size

    interface AddMusicListAdapterDataProxy {
        suspend fun getMusicWithMusicBucket(bucket: String): Collection<Music>
        suspend fun insertMusicWithMusicBucket(musicBaseId: Long, bucket: String): Long
        fun deleteMusicWithMusicBucket(musicBaseId: Long, musicBucket: String)
    }
}