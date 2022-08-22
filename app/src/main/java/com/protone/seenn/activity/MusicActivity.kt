package com.protone.seenn.activity

import android.content.Intent
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.protone.api.animation.AnimationHelper
import com.protone.api.baseType.getDrawable
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.*
import com.protone.api.entity.Music
import com.protone.api.entity.MusicBucket
import com.protone.seen.adapter.MusicBucketAdapter
import com.protone.seen.adapter.MusicListAdapter
import com.protone.seen.customView.StatusImageView
import com.protone.seenn.Medias
import com.protone.seenn.R
import com.protone.seenn.broadcast.workLocalBroadCast
import com.protone.seenn.database.userConfig
import com.protone.seenn.databinding.MusicActivtiyBinding
import com.protone.seenn.viewModel.AddBucketViewModel
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.seenn.viewModel.MusicModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicActivity : BaseActivity<MusicActivtiyBinding, MusicModel>(true),
    StatusImageView.StateListener,
    ViewTreeObserver.OnGlobalLayoutListener {
    override val viewModel: MusicModel by viewModels()

    override fun createView(): View {
        binding = MusicActivtiyBinding.inflate(layoutInflater, root, false).apply {
            activity = this@MusicActivity
            root.viewTreeObserver.addOnGlobalLayoutListener(this@MusicActivity)
            appToolbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
                binding.toolbar.progress =
                    -verticalOffset / appBarLayout.totalScrollRange.toFloat()
            }
        }
        return binding.root
    }

    @Suppress("ObjectLiteralToLambda")
    override suspend fun MusicModel.init() {
        bucket = userConfig.lastMusicBucket
        val musicController = MusicControllerIMP(binding.mySmallMusicPlayer)
        musicController.onClick {
            startActivity(MusicViewActivity::class.intent)
        }

        initList(
            Medias.musicBucket[lastBucket] ?: Medias.music
        )

        mbClickCallBack { name ->
            bucket = name
            hideBucket()
            updateBucket()
        }
        mlClickCallBack { music ->
            if (lastBucket != bucket) {
                musicController.setMusicList(
                    getMusicList()
                )
            }
            musicController.play(music)
            userConfig.lastMusicBucket = bucket
        }

        Medias.musicBucketNotifier.observe(this@MusicActivity) {
            sendViewEvent(MusicModel.MusicEvent.RefreshBucket)
        }
        setBucket()
        bindMusicService {
            musicController.setBinder(this@MusicActivity, it, onPlaying = {
                onUiThread {
                    getMusicListAdapter().playPosition(it)
                }
            })
            musicController.setMusicList(Medias.musicBucket[bucket] ?: Medias.music)
        }

        workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))

        onViewEvent {
            when (it) {
                MusicModel.MusicEvent.Delete -> delete()
                MusicModel.MusicEvent.RefreshBucket -> refreshBucket()
                MusicModel.MusicEvent.Edit -> edit()
                MusicModel.MusicEvent.AddList -> {
                    if (viewModel.compareName()) return@onViewEvent
                    startActivity(
                        PickMusicActivity::class.intent.putExtra(
                            "BUCKET",
                            viewModel.bucket
                        )
                    )
                }
                MusicModel.MusicEvent.AddBucket -> addBucket()
            }
        }
    }

    private fun MusicModel.updateBucket() = launch(Dispatchers.Main) {
        setBucket()
        getMusicListAdapter().musicList = getMusicList()
    }

    fun sendDelete() {
        sendViewEvent(MusicModel.MusicEvent.Delete)
    }

    private suspend fun edit() {
        if (viewModel.compareName()) return
        withContext(Dispatchers.Default) {
            val ar = startActivityForResult(
                AddBucketActivity::class.intent.putExtra(
                    AddBucketViewModel.BUCKET_NAME,
                    viewModel.bucket
                )
            )
            ar?.also { re ->
                if (re.resultCode != RESULT_OK) return@also
                re.data?.getStringExtra(AddBucketViewModel.BUCKET_NAME)?.let {
                    viewModel.bucket = it
                    getMusicBucketAdapter().clickCallback?.invoke(viewModel.bucket)
                    sendViewEvent(MusicModel.MusicEvent.RefreshBucket)
                }
            }
        }
    }

    private suspend fun addBucket(): Unit = withContext(Dispatchers.Default) {
        val re = startActivityForResult(AddBucketActivity::class.intent)
        when (re?.resultCode) {
            RESULT_OK -> re.data?.getStringExtra(AddBucketViewModel.BUCKET_NAME)
                ?.let { addBucket(it) }
            RESULT_CANCELED -> R.string.cancel.getString().toast()
        }
    }

    private suspend fun MusicModel.refreshBucket() {
        getBucket()?.let { refreshBucket(it) }
    }

    private suspend fun MusicModel.delete() {
        if (compareName()) return
        val musicBucket = getMusicBucketByName(bucket)
        if (musicBucket != null) {
            if (getMusicBucketAdapter().deleteBucket(musicBucket)) {
                val re = doDeleteBucket(musicBucket)
                if (re) {
                    workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
                    bucket = R.string.all_music.getString()
                    musicBucket.icon?.let { deleteMusicBucketCover(it) }
                    R.string.success.getString().toast()
                    actionPosition = 0
                    updateBucket()
                } else {
                    R.string.failed_msg.getString().toast()
                    getMusicBucketAdapter().addBucket(musicBucket)
                }
            } else {
                R.string.failed_msg.getString().toast()
            }
        }
    }

    private suspend fun MusicModel.setBucket() {
        getBucket()?.let {
            setBucket(
                it.icon,
                it.name,
                if (it.date != null && it.detail != null) "${it.date} ${it.detail}" else R.string.none.getString()
            )
        }
    }

    suspend fun MusicModel.initList(musicList: MutableList<Music>) {
        binding.musicBucket.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MusicBucketAdapter(
                context,
                filterBucket()
            ).apply {
                this.musicBuckets = getMusicBucket()
                this.musicBucketEvent = object : MusicBucketAdapter.MusicBucketEvent {
                    override fun addList(bucket: String, position: Int) {
                        this@initList.bucket = bucket
                        actionPosition = position
                        sendViewEvent(MusicModel.MusicEvent.AddList)
                    }

                    override fun delete(bucket: String, position: Int) {
                        this@initList.bucket = bucket
                        actionPosition = position
                        sendViewEvent(MusicModel.MusicEvent.Delete)
                    }

                    override fun edit(bucket: String, position: Int) {
                        this@initList.bucket = bucket
                        actionPosition = position
                        sendViewEvent(MusicModel.MusicEvent.Edit)
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

    private suspend fun MusicModel.setBucket(
        iconPath: String?,
        bucketName: String,
        detail: String
    ) = withContext(Dispatchers.Main) {
        bucket = bucketName
        binding.apply {
            if (iconPath != null) {
                Glide.with(this@MusicActivity).asDrawable().load(iconPath)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .override(musicBucketIcon.measuredWidth, musicBucketIcon.measuredHeight)
                    .into(musicBucketIcon)
            } else {
                musicBucketIcon.setImageDrawable(R.drawable.ic_baseline_music_note_24.getDrawable())
            }
            musicBucketName.text = bucketName
            musicBucketMsg.text = detail
        }
    }

    private suspend fun addBucket(name: String): Unit? = viewModel.getMusicBucketByName(name)?.let {
        withContext(Dispatchers.Main) {
            getMusicBucketAdapter().addBucket(it)
        }
    }

    private fun MusicModel.getActionBucket(): String? =
        if ((binding.musicBucket.adapter as MusicBucketAdapter).musicBuckets.size > 0
            && (binding.musicBucket.adapter as MusicBucketAdapter).musicBuckets.size > actionPosition
        ) {
            (binding.musicBucket.adapter as MusicBucketAdapter).musicBuckets[actionPosition].name
        } else null

    private suspend fun MusicModel.refreshBucket(bucket: MusicBucket) =
        withContext(Dispatchers.Main) {
            getActionBucket()?.let {
                getMusicBucketAdapter().refreshBucket(it, bucket)
            }
        }

    private fun mlClickCallBack(callback: (Music) -> Unit) {
        getMusicListAdapter().clickCallback = callback
    }

    private fun mbClickCallBack(callback: (String) -> Unit) {
        getMusicBucketAdapter().clickCallback = callback
    }

    private fun getMusicBucketAdapter() = (binding.musicBucket.adapter as MusicBucketAdapter)

    private fun getMusicListAdapter() = (binding.musicMusicList.adapter as MusicListAdapter)

    override fun onActive() {
        binding.appToolbar.setExpanded(false, false)
        viewModel.containerAnimator?.reverse()
    }

    override fun onNegative() {
        viewModel.containerAnimator?.start()
    }

    private fun hideBucket() {
        binding.musicShowBucket.negative()
    }

    override fun onGlobalLayout() {
        binding.apply {
            appToolbar.setPadding(
                appToolbar.paddingLeft,
                appToolbar.paddingTop + statuesBarHeight,
                appToolbar.paddingRight,
                appToolbar.paddingBottom
            )
            musicBucketContainer.let {
                it.setPadding(
                    it.paddingLeft,
                    it.paddingTop,
                    it.paddingRight,
                    navigationBarHeight + musicAddBucket.measuredHeight - musicAddBucket.paddingBottom
                )
                viewModel.containerAnimator =
                    getAni(it, mySmallMusicPlayer.measuredHeight.toFloat())

                it.y = toolbar.minHeight + statuesBarHeight.toFloat()
            }
            musicShowBucket.setOnStateListener(this@MusicActivity)
            musicMusicList.setPadding(0, 0, 0, mySmallMusicPlayer.height)
            var value: ViewTreeObserver.OnGlobalLayoutListener? = null
            value = ViewTreeObserver.OnGlobalLayoutListener {
                appToolbar.setExpanded(false, false)
                appToolbar.viewTreeObserver.removeOnGlobalLayoutListener(value)
            }
            appToolbar.viewTreeObserver.addOnGlobalLayoutListener(value)
            root.viewTreeObserver.removeOnGlobalLayoutListener(this@MusicActivity)
        }
    }

    private fun getAni(target: View, value: Float) = AnimationHelper.translationY(
        target,
        target.height.toFloat() - value
    )
}