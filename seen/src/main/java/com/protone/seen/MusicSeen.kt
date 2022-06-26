package com.protone.seen

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
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
        AddList,
        Delete,
        Edit,
        RefreshBucket
    }

    private var containerAnimator: ObjectAnimator? = null
    private val binding = MusicLayoutBinding.inflate(context.layoutInflater, context.root, true)
    val musicController = binding.mySmallMusicPlayer

    override val viewRoot: View
        get() = binding.root

    var bucket: String = ""

    var actionPosition: Int = 0

    override fun getToolBar(): View = binding.appToolbar

    init {
        setNavigation()
        binding.apply {
            self = this@MusicSeen
            root.viewTreeObserver.addOnGlobalLayoutListener(this@MusicSeen)
            appToolbar.addOnOffsetChangedListener(
                AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                    binding.toolbar.progress =
                        -verticalOffset / appBarLayout.totalScrollRange.toFloat()
                })
        }

    }

    override fun offer(event: Event) {
        viewEvent.trySend(event)
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
                    this.musicBucketEvent = object : MusicBucketAdapter.MusicBucketEvent {
                        override fun addList(bucket: String, position: Int) {
                            this@MusicSeen.bucket = bucket
                            actionPosition = position
                            offer(Event.AddList)
                        }

                        override fun delete(bucket: String, position: Int) {
                            this@MusicSeen.bucket = bucket
                            actionPosition = position
                            offer(Event.Delete)
                        }

                        override fun edit(bucket: String, position: Int) {
                            this@MusicSeen.bucket = bucket
                            actionPosition = position
                            offer(Event.Edit)
                        }
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

    suspend fun setBucket(bitArray: ByteArray?, bucketName: String, detail: String) =
        withContext(Dispatchers.Main) {
            bucket = bucketName
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
        if (binding.musicBucket.adapter is MusicBucketAdapter)
            (binding.musicBucket.adapter as MusicBucketAdapter).clickCallback = callback
    }

    fun performListClick(bucket: String) {
        if (binding.musicBucket.adapter is MusicBucketAdapter)
            (binding.musicBucket.adapter as MusicBucketAdapter).clickCallback(bucket)
    }

    private fun getActionBucket(): String? = if (binding.musicBucket.adapter is MusicBucketAdapter
        && (binding.musicBucket.adapter as MusicBucketAdapter).musicBuckets.size > 0
        && (binding.musicBucket.adapter as MusicBucketAdapter).musicBuckets.size > actionPosition
    ) {
        (binding.musicBucket.adapter as MusicBucketAdapter).musicBuckets[actionPosition].name
    } else null


    fun deleteBucket(musicBucket: MusicBucket): Boolean =
        if (binding.musicBucket.adapter is MusicBucketAdapter)
            (binding.musicBucket.adapter as MusicBucketAdapter).deleteBucket(musicBucket)
        else false

    suspend fun refreshBucket(bucket: MusicBucket) = withContext(Dispatchers.Main) {
        val actionBucket = getActionBucket()
        if (binding.musicBucket.adapter is MusicBucketAdapter && actionBucket != null)
            (binding.musicBucket.adapter as MusicBucketAdapter)
                .refreshBucket(actionBucket, bucket)
    }

    fun mlClickCallBack(callback: (Music) -> Unit) {
        if (binding.musicMusicList.adapter is MusicListAdapter)
            (binding.musicMusicList.adapter as MusicListAdapter).clickCallback = callback
    }

    fun updateMusicList(list: MutableList<Music>) {
        if (binding.musicMusicList.adapter is MusicListAdapter)
            (binding.musicMusicList.adapter as MusicListAdapter).musicList = list
    }

    suspend fun addBucket(bucket: MusicBucket) = withContext(Dispatchers.Main) {
        if (binding.musicBucket.adapter is MusicBucketAdapter)
            (binding.musicBucket.adapter as MusicBucketAdapter).addBucket(bucket)
    }

    fun addBucketNoCheck(bucket: MusicBucket) {
        if (binding.musicBucket.adapter is MusicBucketAdapter)
            (binding.musicBucket.adapter as MusicBucketAdapter).addBucket(bucket)
    }

    fun playPosition(music: Music) {
        if (binding.musicMusicList.adapter is MusicListAdapter)
            (binding.musicMusicList.adapter as MusicListAdapter).playPosition(music)
    }

    override fun onActive() {
        binding.appToolbar.setExpanded(false, false)
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
            appToolbar.setPadding(
                appToolbar.paddingLeft,
                appToolbar.paddingTop + context.statuesBarHeight,
                appToolbar.paddingRight,
                appToolbar.paddingBottom
            )
            musicBucketContainer.let {
                it.setPadding(
                    it.paddingLeft,
                    it.paddingTop,
                    it.paddingRight,
                    context.navigationBarHeight + musicAddBucket.measuredHeight - musicAddBucket.paddingBottom
                )
                containerAnimator = getAni(it, mySmallMusicPlayer.measuredHeight.toFloat())

                it.y = toolbar.minHeight + context.statuesBarHeight.toFloat()
            }
            musicShowBucket.setOnStateListener(this@MusicSeen)
            musicMusicList.setPadding(0, 0, 0, mySmallMusicPlayer.height)
            var value: ViewTreeObserver.OnGlobalLayoutListener? = null
            value = ViewTreeObserver.OnGlobalLayoutListener {
                appToolbar.setExpanded(false, false)
                appToolbar.viewTreeObserver.removeOnGlobalLayoutListener(value)
            }
            appToolbar.viewTreeObserver.addOnGlobalLayoutListener(value)
            root.viewTreeObserver.removeOnGlobalLayoutListener(this@MusicSeen)
        }
    }

    private fun getAni(target: View, value: Float) = AnimationHelper.translationY(
        target,
        target.height.toFloat() - value
    )

    fun clearMer() {
        Glide.get(context).clearMemory()
    }
}