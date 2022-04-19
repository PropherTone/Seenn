package com.protone.seen

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.appbar.AppBarLayout
import com.protone.api.animation.AnimationHelper
import com.protone.api.context.layoutInflater
import com.protone.api.context.navigationBarHeight
import com.protone.api.context.root
import com.protone.api.context.statuesBarHeight
import com.protone.database.room.entity.Music
import com.protone.database.room.entity.MusicBucket
import com.protone.seen.adapter.MusicBucketAdapter
import com.protone.seen.adapter.MusicListAdapter
import com.protone.seen.customView.StateImageView
import com.protone.seen.databinding.MusicLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.stream.Collectors

@SuppressLint("ClickableViewAccessibility")
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
            binding.mySmallMusicPlayer.isPlaying = value
            field = value
        }

    var bucket: String = ""

    override fun getToolBar(): View = binding.appToolbar

    init {
        setNavigation()
        binding.apply {
            self = this@MusicSeen
            root.viewTreeObserver.addOnGlobalLayoutListener(this@MusicSeen)
            binding.toolbar.progress = 1f
            binding.toolbar.transitionToEnd()
            appToolbar.addOnOffsetChangedListener(
                AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                    binding.toolbar.progress =
                        -verticalOffset / appBarLayout.totalScrollRange.toFloat()
                })
        }

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
                        .collect(Collectors.toList()).let {
                            if (it.size > 0) it[0] else MusicBucket()
                        }
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

    suspend fun setBucket(bitArray: ByteArray?, bucketName: String, detail: String) =
        withContext(Dispatchers.Main) {
            binding.apply {
                if (bitArray != null) {
                    Glide.with(context).asDrawable().load(bitArray)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .override(musicBucketIcon.measuredWidth, musicBucketIcon.measuredHeight)
                        .into(musicBucketIcon)
                } else {
                    musicBucketIcon.setImageDrawable(
                        ResourcesCompat
                            .getDrawable(
                                context.resources,
                                R.drawable.ic_baseline_music_note_24,
                                null
                            )
                    )
                }

                musicBucketName.text = bucketName
                musicBucketMsg.text = detail
            }
        }

    fun mbClickCallBack(callback: (String) -> Unit) {
        (binding.musicBucket.adapter as MusicBucketAdapter?)?.clickCallback = callback
    }

    suspend fun refreshBucket(bucket: MusicBucket) = withContext(Dispatchers.Main) {
        (binding.musicBucket.adapter as MusicBucketAdapter?)?.refreshBucket(
            this@MusicSeen.bucket,
            bucket
        )
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
        binding.apply {
            musicBucketContainer.let {
                it.setPadding(
                    it.paddingLeft,
                    it.paddingTop,
                    it.paddingRight,
                    context.navigationBarHeight + musicAddBucket.measuredHeight - musicAddBucket.paddingBottom
                )
                it.setOnTouchListener { _, _ -> false }
                it.setOnClickListener { }
                containerAnimator = AnimationHelper.translationY(
                    it,
                    it.height.toFloat() - mySmallMusicPlayer.height
                ).also { ani ->
                    ani.doOnStart {
                        val event = MotionEvent.obtain(
                            SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_DOWN,
                            binding.root.width / 2f,
                            binding.root.height / 2f,
                            0
                        )
                        binding.musicMusicList.dispatchTouchEvent(event)
                        ani.addUpdateListener { va ->
                            event.setLocation(
                                binding.root.width / 2f,
                                (va.animatedValue as Float) / 2
                            )
                            event.action = MotionEvent.ACTION_MOVE
                            binding.musicMusicList.dispatchTouchEvent(event)
                        }
                        ani.doOnEnd {
                            event.action = MotionEvent.ACTION_UP
                            binding.musicMusicList.dispatchTouchEvent(event)
                        }
                        event.recycle()
                    }
                }

                it.y = toolbar.minHeight + context.statuesBarHeight.toFloat()
            }
            musicShowBucket.setOnStateListener(this@MusicSeen)
            musicMusicList.setPadding(0, 0, 0, mySmallMusicPlayer.height)
            root.viewTreeObserver.removeOnGlobalLayoutListener(this@MusicSeen)
        }
    }

    fun clearMer() {
        Glide.get(context).clearMemory()
    }
}