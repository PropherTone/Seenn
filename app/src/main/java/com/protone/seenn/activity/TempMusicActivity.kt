package com.protone.seenn.activity

import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.protone.api.baseType.getDrawable
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.*
import com.protone.seenn.R
import com.protone.seenn.databinding.MusicActivtiyBinding
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.ui.adapter.MusicBucketAdapter
import com.protone.ui.adapter.MusicListAdapter
import com.protone.ui.customView.StatusImageView
import com.protone.worker.Medias
import com.protone.worker.viewModel.AddBucketViewModel
import com.protone.worker.viewModel.TempMusicModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TempMusicActivity : BaseActivity<MusicActivtiyBinding, TempMusicModel>(true),
    StatusImageView.StateListener {
    override val viewModel: TempMusicModel by viewModels()

    override fun createView(): View {
        binding = MusicActivtiyBinding.inflate(layoutInflater, root, false).apply {
            activity = this@TempMusicActivity
            fitStatuesBar(musicBucketContainer)
            root.onGlobalLayout {
                appToolbar.paddingTop(appToolbar.paddingTop + statuesBarHeight)
                musicBucketContainer.botBlock = mySmallMusicPlayer.measuredHeight.toFloat()
                musicShowBucket.setOnStateListener(this@TempMusicActivity)
                musicMusicList.setPadding(0, 0, 0, mySmallMusicPlayer.height)
                appToolbar.onGlobalLayout {
                    appToolbar.setExpanded(false, false)
                }
            }
            appToolbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
                binding.toolbar.progress =
                    -verticalOffset / appBarLayout.totalScrollRange.toFloat()
            }
        }
        return binding.root
    }

    override suspend fun TempMusicModel.init() {
        val controller = MusicControllerIMP(binding.mySmallMusicPlayer)
        bindMusicService {
            controller.setBinder(this@TempMusicActivity, it, onPlaying = { music ->
                getMusicListAdapter().playPosition(music)
            })
            controller.onClick {
                startActivity(MusicViewActivity::class.intent)
            }
        }

        onViewEvent {
            when (it) {
                is TempMusicModel.TempMusicEvent.PlayMusic -> withContext(Dispatchers.Default) {
                    if (lastBucket != binding.musicBucketName.text) {
                        lastBucket = binding.musicBucketName.text.toString()
                        controller.setMusicList(getCurrentMusicList(lastBucket))
                    }
                    controller.play(it.music)
                }
                is TempMusicModel.TempMusicEvent.SetBucketCover -> getBucket(it.name)?.let {
                    withContext(Dispatchers.Main) {
                        binding.apply {
                            if (it.icon != null) {
                                Glide.with(this@TempMusicActivity).asDrawable().load(it.icon)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .skipMemoryCache(true)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .override(
                                        musicBucketIcon.measuredWidth,
                                        musicBucketIcon.measuredHeight
                                    )
                                    .into(musicBucketIcon)
                            } else {
                                musicBucketIcon.setImageDrawable(R.drawable.ic_baseline_music_note_24.getDrawable())
                            }
                            musicBucketName.text = it.name
                            musicBucketMsg.text =
                                if (it.date != null && it.detail != null) "${it.date} ${it.detail}" else R.string.none.getString()
                        }
                    }
                }
                is TempMusicModel.TempMusicEvent.AddMusic -> {
                    if (it.bucket == R.string.all_music.getString()) {
                        R.string.bruh.getString().toast()
                        return@onViewEvent
                    }
                    startActivity(
                        PickMusicActivity::class.intent.putExtra(
                            "BUCKET",
                            it.bucket
                        )
                    )
                }
                is TempMusicModel.TempMusicEvent.Edit -> withContext(Dispatchers.Default) {
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
                            sendViewEvent(
                                TempMusicModel.TempMusicEvent.RefreshBucket(
                                    it.bucket,
                                    name
                                )
                            )
                        }
                    }
                }
                is TempMusicModel.TempMusicEvent.Delete -> {
                    if (it.bucket == R.string.all_music.getString()) {
                        R.string.bruh.getString().toast()
                        return@onViewEvent
                    }
                    val musicBucket = tryDeleteMusicBucket(it.bucket)
                    if (musicBucket != null) {
                        getMusicBucketAdapter().deleteBucket(musicBucket)
                    } else {
                        R.string.failed_msg.getString().toast()
                    }
                }
                is TempMusicModel.TempMusicEvent.RefreshBucket -> {
                    getBucket(it.newName)?.let { bucket ->
                        getMusicBucketAdapter().refreshBucket(it.oldName, bucket)
                    }
                }
                is TempMusicModel.TempMusicEvent.AddMusicBucket -> {
                    val re = startActivityForResult(AddBucketActivity::class.intent)
                    when (re?.resultCode) {
                        RESULT_OK -> re.data?.getStringExtra(AddBucketViewModel.BUCKET_NAME)
                            ?.let { data ->
                                getBucket(data)?.let { bucket ->
                                    getMusicBucketAdapter().addBucket(bucket)
                                }
                            }
                        RESULT_CANCELED -> R.string.cancel.getString().toast()
                    }
                }
            }
        }

        initMusicList()
        initMusicBucketList()
        getMusicListAdapter().clickCallback = {
            sendViewEvent(TempMusicModel.TempMusicEvent.PlayMusic(it))
        }

        Medias.musicBucketNotifier.observe(this@TempMusicActivity) {
            if (it == "") return@observe
            sendViewEvent(TempMusicModel.TempMusicEvent.RefreshBucket(it, it))
        }

    }

    private fun initMusicList() {
        binding.musicMusicList.apply {
            layoutManager = LinearLayoutManager(this@TempMusicActivity)
            adapter = MusicListAdapter(this@TempMusicActivity)
        }
    }

    private suspend fun TempMusicModel.initMusicBucketList() {
        binding.musicBucket.apply {
            val allMusicBucket = getMusicBuckets()
            layoutManager = LinearLayoutManager(this@TempMusicActivity)
            adapter = MusicBucketAdapter(
                this@TempMusicActivity,
                getLastMusicBucket(allMusicBucket)
            ).apply {
                clickCallback = {
                    hideBucket()
                    if (binding.musicBucketName.text != it) {
                        sendViewEvent(TempMusicModel.TempMusicEvent.SetBucketCover(it))
                        getMusicListAdapter().musicList = getCurrentMusicList(it)
                    }
                }
                musicBuckets = allMusicBucket
                clickCallback?.invoke(lastBucket)
                musicBucketEventListener = object : MusicBucketAdapter.MusicBucketEvent {
                    override fun addMusic(bucket: String, position: Int) =
                        sendViewEvent(TempMusicModel.TempMusicEvent.AddMusic(bucket))

                    override fun delete(bucket: String, position: Int) =
                        sendViewEvent(TempMusicModel.TempMusicEvent.Delete(bucket))

                    override fun edit(bucket: String, position: Int) =
                        sendViewEvent(TempMusicModel.TempMusicEvent.Edit(bucket))
                }
            }
        }
    }

    private fun getMusicBucketAdapter() = (binding.musicBucket.adapter as MusicBucketAdapter)

    private fun getMusicListAdapter() = (binding.musicMusicList.adapter as MusicListAdapter)

    private fun hideBucket() {
        binding.musicShowBucket.negative()
    }

    fun sendEdit() {
        sendViewEvent(TempMusicModel.TempMusicEvent.Edit(binding.musicBucketName.text.toString()))
    }

    fun sendDelete() {
        sendViewEvent(TempMusicModel.TempMusicEvent.Delete(binding.musicBucketName.text.toString()))
    }

    fun sendAddMusic() {
        sendViewEvent(TempMusicModel.TempMusicEvent.AddMusic(binding.musicBucketName.text.toString()))
    }

    override fun onActive() {
        binding.appToolbar.setExpanded(false, false)
        binding.musicBucketContainer.show()
    }

    override fun onNegative() {
        binding.musicBucketContainer.hide()
    }

}