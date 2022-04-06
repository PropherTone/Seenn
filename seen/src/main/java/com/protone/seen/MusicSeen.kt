package com.protone.seen

import android.animation.ObjectAnimator
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.animation.AnimationHelper
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.database.room.entity.Music
import com.protone.database.room.entity.MusicBucket
import com.protone.seen.adapter.MusicBucketAdapter
import com.protone.seen.adapter.MusicListAdapter
import com.protone.seen.customView.StateImageView
import com.protone.seen.databinding.MusicLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.stream.Collectors

class MusicSeen(context: Context) : Seen<MusicSeen.Event>(context), StateImageView.StateListener,
    ViewTreeObserver.OnGlobalLayoutListener {

    enum class Event {
        AddBucket,
        Play,
        Finish,
        AddList
    }

    private var containerAnimator: ObjectAnimator? = null
    private val binding = MusicLayoutBinding.inflate(context.layoutInflater, context.root, true)

    override val viewRoot: View
        get() = binding.root

    var musicName: String = ""
        set(value) {
            binding.mySmallMusicPlayer.name = value
            field = value
        }


    var icon: Uri = Uri.parse("")
        set(value) {
            binding.mySmallMusicPlayer.icon = value
            field = value
        }

    var isPlaying: Boolean = false
        set(value) {
            Log.d("TAG", "seen: $value")
            binding.mySmallMusicPlayer.isPlaying = value
            field = value
        }

    var bucket : String = ""

    init {
        binding.self = this
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun offer(event: Event) {
        viewEvent.offer(event)
    }

    suspend fun initList(
        musicBucket: MutableList<MusicBucket>,
        musicList: MutableList<Music>,
        userConfig: String
    ) =
        withContext(Dispatchers.Main) {
            binding.musicBucket.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = MusicBucketAdapter(
                    context,
                    musicBucket.stream().filter { it.name == userConfig }
                        .collect(Collectors.toList())[0]
                ).apply {
                    this.musicBuckets = musicBucket
                    this.addList = {
                        bucket = it
                        offer(Event.AddList)
                    }
                }
            }
            binding.musicMusicList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = MusicListAdapter(context).apply {
                    this.musicList = musicList
                }
            }
        }

    suspend fun initSmallMusic() = withContext(Dispatchers.IO) {
        binding.mySmallMusicPlayer.apply {
            { offer(Event.Play) }.let {
                playMusic = it
                pauseMusic = it
            }
        }
    }

    fun mbClickCallBack(callback: (String) -> Unit) {
        (binding.musicBucket.adapter as MusicBucketAdapter?)?.clickCallback = callback
    }

    fun mlClickCallBack(callback: (Int) -> Unit) {
        (binding.musicMusicList.adapter as MusicListAdapter?)?.clickCallback = callback
    }

    fun updateMusicList(list: MutableList<Music>) {
        (binding.musicMusicList.adapter as MusicListAdapter?)?.musicList = list
    }

    suspend fun addBucket(bucket: MusicBucket) = withContext(Dispatchers.Main) {
        (binding.musicBucket.adapter as MusicBucketAdapter?)?.addBucket(bucket)
    }

    fun playPosition(position: Int) {
        (binding.musicMusicList.adapter as MusicListAdapter?)?.playPosition(position)
    }

    override fun onActive() {
        containerAnimator?.reverse()
    }

    override fun onNegative() {
        containerAnimator?.start()
    }

    fun hideBucket() {
        binding.musicShowBucket.negative()
    }

    override fun onGlobalLayout() {
        binding.musicBucketContainer.height.toFloat().let {
            containerAnimator = AnimationHelper.translationY(
                binding.musicBucketContainer,
                it - binding.mySmallMusicPlayer.height
            )
//            containerAnimator = ObjectAnimator.ofFloat(
//                binding.musicBucketContainer,
//                "translationY",
//                it - binding.mySmallMusicPlayer.height
//            )
        }
        binding.musicShowBucket.setOnStateListener(this)

        binding.musicMusicList.setPadding(0, 0, 0, binding.mySmallMusicPlayer.height)

        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
}