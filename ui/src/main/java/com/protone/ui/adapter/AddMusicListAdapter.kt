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
import com.protone.api.context.onUiThread
import com.protone.api.entity.Music
import com.protone.ui.R
import com.protone.ui.databinding.MusicListLayoutBinding
import com.protone.worker.Medias
import com.protone.worker.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AddMusicListAdapter(
    context: Context,
    private val bucket: String,
    private val multiSelect: Boolean
) : SelectListAdapter<MusicListLayoutBinding, Music, Any>(context) {

    init {
        multiChoose = multiSelect.also { b ->
            if (b) Medias.musicBucket[bucket]?.let { selectList.addAll(it) }
        }
    }

    var musicList = mutableListOf<Music>()
        set(value) {
            field.clear()
            field.addAll(value)
        }

    private val viewQueue = PriorityQueue<Int>()

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
                    viewQueue.add(position)
                    if (selectList.contains(music)) {
                        if (!multiSelect) return@setOnClickListener
                        DatabaseHelper
                            .instance
                            .musicWithMusicBucketDAOBridge
                            .deleteMusicWithMusicBucketAsync(music.musicBaseId)
                        checkSelect(holder, music)
                        return@setOnClickListener
                    }
                    checkSelect(holder, music)
                    musicListPlayState.drawable.let { d ->
                        when (d) {
                            is Animatable -> {
                                d.start()
                                if (multiSelect) {
                                    launch(Dispatchers.IO) {
                                        val re = DatabaseHelper
                                            .instance
                                            .musicWithMusicBucketDAOBridge
                                            .insertMusicWithMusicBucket(music.musicBaseId, bucket)
                                        if (re != -1L) {
                                            changeIconAni(musicListPlayState)
                                        } else {
                                            selectList.remove(music)
                                            notifyItemChanged()
                                        }
                                    }
                                } else {
                                    changeIconAni(musicListPlayState)
                                }
                                d.stop()
                            }
                            else -> {
                                launch(Dispatchers.IO) {
                                    selectList.remove(music)
                                    notifyItemChanged()
                                }
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

    private fun changeIconAni(view: ImageView) = context.onUiThread {
        AnimationHelper.apply {
            animatorSet(scaleX(view, 0f), scaleY(view, 0f), doOnEnd = {
                view.setImageDrawable(R.drawable.ic_baseline_check_24_white.getDrawable())
                animatorSet(scaleX(view, 1f), scaleY(view, 1f), play = true)
            }, play = true)
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
}