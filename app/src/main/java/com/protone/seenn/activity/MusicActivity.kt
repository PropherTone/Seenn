package com.protone.seenn.activity

import androidx.core.view.marginBottom
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.protone.api.baseType.getDrawable
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.*
import com.protone.api.entity.MusicBucket
import com.protone.seenn.R
import com.protone.seenn.databinding.MusicActivtiyBinding
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.ui.adapter.MusicBucketAdapter
import com.protone.ui.adapter.MusicListAdapter
import com.protone.ui.customView.StatusImageView
import com.protone.worker.viewModel.AddBucketViewModel
import com.protone.worker.viewModel.MusicModel
import com.protone.worker.viewModel.PickMusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicActivity : BaseActivity<MusicActivtiyBinding, MusicModel, MusicModel.MusicEvent>(true),
    StatusImageView.StateListener {
    override val viewModel: MusicModel by lazy {
        ViewModelProvider(this).get(MusicModel::class.java).apply {
            onMusicDataEvent = object : MusicModel.OnMusicDataEvent {
                override suspend fun onNewMusicBucket(musicBucket: MusicBucket) {
                    getMusicBucketAdapter()?.addBucket(musicBucket)
                }

                override suspend fun onMusicBucketUpdated(musicBucket: MusicBucket) {
                    refreshListAndBucket(musicBucket)
                }

                override suspend fun onMusicBucketDeleted(musicBucket: MusicBucket) {
                    getMusicBucketAdapter()?.deleteBucket(musicBucket)
                }

            }
        }
    }

    override fun createView(): MusicActivtiyBinding {
        return MusicActivtiyBinding.inflate(layoutInflater, root, false).apply {
            activity = this@MusicActivity
            fitStatuesBar(musicBucketContainer)
            root.onGlobalLayout {
                actionBtnContainer.marginBottom(mySmallMusicPlayer.height + search.marginBottom)
                appToolbar.paddingTop(appToolbar.paddingTop + statuesBarHeight)
                musicBucketContainer.botBlock = mySmallMusicPlayer.measuredHeight.toFloat()
                musicShowBucket.setOnStateListener(this@MusicActivity)
                musicMusicList.setPadding(0, 0, 0, mySmallMusicPlayer.height)
                appToolbar.onGlobalLayout {
                    appToolbar.setExpanded(false, false)
                }
            }
            appToolbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
                toolbar.progress = -verticalOffset / appBarLayout.totalScrollRange.toFloat()
            }
        }
    }

    override suspend fun MusicModel.init() {
        val controller = MusicControllerIMP(binding.mySmallMusicPlayer)

        onViewEvent {
            when (it) {
                is MusicModel.MusicEvent.PlayMusic -> withContext(Dispatchers.Default) {
                    if (lastBucket != binding.musicBucketName.text) {
                        getMusicListAdapter()?.getPlayList()?.let { list ->
                            lastBucket = binding.musicBucketName.text.toString()
                            controller.setMusicList(list)
                        }
                    }
                    controller.play(it.music)
                }
                is MusicModel.MusicEvent.SetBucketCover -> getBucket(it.name)?.let {
                    withContext(Dispatchers.Main) {
                        binding.apply {
                            if (it.icon != null) {
                                Glide.with(this@MusicActivity).asDrawable().load(it.icon)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .skipMemoryCache(true)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .override(
                                        musicBucketIcon.measuredWidth,
                                        musicBucketIcon.measuredHeight
                                    ).into(musicBucketIcon)
                            } else {
                                musicBucketIcon.setImageDrawable(R.drawable.ic_baseline_music_note_24.getDrawable())
                            }
                            musicBucketName.text = it.name
                            musicBucketMsg.text =
                                if (it.date != null && it.detail != null) "${it.date} ${it.detail}" else R.string.none.getString()
                        }
                    }
                }
                is MusicModel.MusicEvent.AddMusic -> {
                    if (it.bucket == R.string.all_music.getString()) {
                        R.string.bruh.getString().toast()
                        return@onViewEvent
                    }
                    startActivityForResult(
                        PickMusicActivity::class.intent.putExtra(
                            PickMusicViewModel.BUCKET_NAME,
                            it.bucket
                        )
                    ).run {
                        getBucketRefreshed(it.bucket)?.let { mb ->
                            refreshListAndBucket(mb)
                        }
                    }
                }
                is MusicModel.MusicEvent.Edit -> withContext(Dispatchers.Default) {
                    if (it.bucket == R.string.all_music.getString()) {
                        R.string.bruh.getString().toast()
                        return@withContext
                    }
                    val ar = startActivityForResult(
                        AddBucketActivity::class.intent.putExtra(
                            AddBucketViewModel.BUCKET_NAME,
                            it.bucket
                        )
                    )
                    ar?.also { re ->
                        if (re.resultCode != RESULT_OK) return@also
                        re.data?.getStringExtra(AddBucketViewModel.BUCKET_NAME)?.let { name ->
                            sendViewEvent(MusicModel.MusicEvent.RefreshBucket(name))
                        }
                    }
                }
                is MusicModel.MusicEvent.Delete -> {
                    if (it.bucket == R.string.all_music.getString()) {
                        R.string.bruh.getString().toast()
                        return@onViewEvent
                    }
                    val musicBucket = tryDeleteMusicBucket(it.bucket)
                    if (musicBucket == null) {
                        R.string.failed_msg.getString().toast()
                    }
                }
                is MusicModel.MusicEvent.RefreshBucket -> {
                    getBucket(it.newName)?.let { bucket ->
                        getMusicBucketAdapter()?.refreshBucket(bucket)
                    }
                }
                is MusicModel.MusicEvent.AddMusicBucket -> {
                    val re = startActivityForResult(AddBucketActivity::class.intent)
                    when (re?.resultCode) {
                        RESULT_CANCELED -> R.string.cancel.getString().toast()
                    }
                }
                is MusicModel.MusicEvent.AddBucket ->
                    getBucket(it.bucket)?.let { mb -> getMusicBucketAdapter()?.addBucket(mb) }
                is MusicModel.MusicEvent.DeleteBucket ->
                    getBucket(it.bucket)?.let { mb -> getMusicBucketAdapter()?.deleteBucket(mb) }
                is MusicModel.MusicEvent.Locate ->
                    getMusicListAdapter()?.getPlayingPosition()
                        ?.let { position ->
                            if (position != -1)
                                binding.musicMusicList.smoothScrollToPosition(position)
                        }
                is MusicModel.MusicEvent.Search ->
                    startActivity(
                        PickMusicActivity::class.intent
                            .putExtra(PickMusicViewModel.BUCKET_NAME, lastBucket)
                            .putExtra(PickMusicViewModel.MODE, PickMusicViewModel.SEARCH_MUSIC)
                    )
            }
        }

        initMusicList()
        initMusicBucketList()
        bindMusicService {
            controller.getPlayingMusic()?.let { music ->
                getMusicListAdapter()?.playPosition(music)
            }
            controller.setBinder(this@MusicActivity, it, onPlaying = { music ->
                getMusicListAdapter()?.playPosition(music)
            })
            binding.mySmallMusicPlayer.coverSwitcher.setOnClickListener {
                startActivity(MusicViewActivity::class.intent)
            }
            controller.onClick {
                binding.musicShowBucket.performClick()
            }
        }
    }

    private fun initMusicList() {
        binding.musicMusicList.apply {
            layoutManager = LinearLayoutManager(this@MusicActivity)
            adapter = MusicListAdapter(this@MusicActivity, mutableListOf()).apply {
                clickCallback = { sendViewEvent(MusicModel.MusicEvent.PlayMusic(it)) }
            }
        }
    }

    private suspend fun MusicModel.initMusicBucketList() {
        binding.musicBucket.apply {
            val allMusicBucket = getMusicBuckets()
            layoutManager = LinearLayoutManager(this@MusicActivity)
            adapter = MusicBucketAdapter(
                this@MusicActivity,
                getLastMusicBucket(allMusicBucket)
            ).apply {
                clickCallback = {
                    launch {
                        hideBucket()
                        if (binding.musicBucketName.text != it.name) {
                            sendViewEvent(MusicModel.MusicEvent.SetBucketCover(it.name))
                            switchMusicBucket(it)
                        }
                    }
                }
                musicBuckets = allMusicBucket
                getBucket(lastBucket)?.let {
                    if (binding.musicBucketName.text != it.name) {
                        sendViewEvent(MusicModel.MusicEvent.SetBucketCover(it.name))
                        getMusicListAdapter()?.insertMusics(getCurrentMusicList(it))
                    }
                }
                musicBucketEventListener = object : MusicBucketAdapter.MusicBucketEvent {
                    override fun addMusic(bucket: String, position: Int) =
                        sendViewEvent(MusicModel.MusicEvent.AddMusic(bucket))

                    override fun delete(bucket: String, position: Int) =
                        sendViewEvent(MusicModel.MusicEvent.Delete(bucket))

                    override fun edit(bucket: String, position: Int) =
                        sendViewEvent(MusicModel.MusicEvent.Edit(bucket))
                }
            }
        }
    }

    private suspend fun MusicModel.refreshListAndBucket(musicBucket: MusicBucket) {
        getMusicBucketAdapter()?.refreshBucket(musicBucket)
        if (binding.musicBucketName.text == musicBucket.name) {
            getMusicListAdapter()?.insertMusics(getCurrentMusicList(musicBucket))
        }
    }

    private suspend fun MusicModel.switchMusicBucket(musicBucket: MusicBucket) {
        binding.musicMusicList.swapAdapter(
            MusicListAdapter(
                this@MusicActivity,
                getCurrentMusicList(musicBucket)
            ).apply {
                getMusicListAdapter()?.getPlayingMusic()?.let { selectList.add(it) }
                clickCallback = getMusicListAdapter()?.clickCallback
            }, true
        )
    }

    private fun getMusicBucketAdapter() = (binding.musicBucket.adapter as MusicBucketAdapter?)

    private fun getMusicListAdapter() = (binding.musicMusicList.adapter as MusicListAdapter?)

    private suspend fun hideBucket() = withContext(Dispatchers.Main) {
        binding.musicShowBucket.negative()
    }

    fun sendEdit() {
        sendViewEvent(MusicModel.MusicEvent.Edit(binding.musicBucketName.text.toString()))
    }

    fun sendDelete() {
        sendViewEvent(MusicModel.MusicEvent.Delete(binding.musicBucketName.text.toString()))
    }

    fun sendAddMusic() {
        sendViewEvent(MusicModel.MusicEvent.AddMusic(binding.musicBucketName.text.toString()))
    }

    override fun onActive() {
        binding.appToolbar.setExpanded(false, false)
        binding.musicBucketContainer.show()
    }

    override fun onNegative() {
        binding.musicBucketContainer.hide()
    }

}