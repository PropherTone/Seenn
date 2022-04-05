package com.protone.seen

import android.animation.ObjectAnimator
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.LinearLayoutManager
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

class MusicSeen(context: Context) : Seen<MusicSeen.Event>(context), StateImageView.StateListener,
    ViewTreeObserver.OnGlobalLayoutListener {

    enum class Event {
        AddBucket,
        Finish
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
            Log.d("TAG", "$value: ")
                binding.mySmallMusicPlayer.isPlaying = value
            field = value
        }

    init {
        binding.self = this
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun offer(event: Event) {
        viewEvent.offer(event)
    }

    suspend fun initList(musicBucket: MutableList<MusicBucket>, musicList: MutableList<Music>) =
        withContext(Dispatchers.Main) {
            binding.musicBucket.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = MusicBucketAdapter(
                    context
                ).apply {
                    this.musicBuckets = musicBucket
                }
            }
            binding.musicMusicList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = MusicListAdapter(context).apply {
                    this.musicList = musicList
                }
            }
        }

    suspend fun initSmallMusic(func1: () -> Unit, func2: () -> Unit) = withContext(Dispatchers.IO) {
        binding.mySmallMusicPlayer.apply {
            playMusic = func1
            pauseMusic = func2
        }
    }

    fun mbClickCallBack(callback: (String) -> Unit) {
        (binding.musicBucket.adapter as MusicBucketAdapter).clickCallback = callback
    }

    fun mlClickCallBack(callback: (Int) -> Unit) {
        (binding.musicMusicList.adapter as MusicListAdapter).clickCallback = callback
    }

    suspend fun addBucket(bucket: MusicBucket) = withContext(Dispatchers.Main) {
        (binding.musicBucket.adapter as MusicBucketAdapter).addBucket(bucket)
    }

    fun playPosition(position: Int){
        (binding.musicMusicList.adapter as MusicListAdapter).playPosition(position)
    }

    override fun onActive() {
        containerAnimator?.reverse()
    }

    override fun onNegative() {
        containerAnimator?.start()
    }

    fun hideBucket(){
        binding.musicShowBucket.negative()
    }

    override fun onGlobalLayout() {
        binding.musicBucketContainer.height.toFloat().let {
            containerAnimator = ObjectAnimator.ofFloat(
                binding.musicBucketContainer,
                "translationY",
                it - binding.mySmallMusicPlayer.height
            )
        }
        binding.musicShowBucket.setOnStateListener(this)

        binding.musicMusicList.setPadding(0, 0, 0, binding.mySmallMusicPlayer.height)

        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
}