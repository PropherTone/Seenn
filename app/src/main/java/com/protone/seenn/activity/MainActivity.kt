package com.protone.seenn.activity

import androidx.activity.viewModels
import androidx.core.view.isGone
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.protone.api.baseType.getString
import com.protone.api.baseType.toDateString
import com.protone.api.baseType.toast
import com.protone.api.context.intent
import com.protone.api.context.onGlobalLayout
import com.protone.api.context.root
import com.protone.api.entity.Music
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.todayDate
import com.protone.seenn.databinding.MainActivityTempBinding
import com.protone.seenn.service.WorkService
import com.protone.seenn.service.getEmptyMusic
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.userConfig
import com.protone.worker.viewModel.GalleryViewViewModel
import com.protone.worker.viewModel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity :
    BaseActivity<MainActivityTempBinding, MainViewModel, MainViewModel.MainViewEvent>(true) {
    override val viewModel: MainViewModel by viewModels()

    private var userName: String? = null
        set(value) {
            if (value == null) return
            binding.userWelcome.text =
                if (value == "") getString(com.protone.ui.R.string.welcome_msg) else value
            binding.userDate.text = todayDate("yyyy/MM/dd")
            field = value
        }
        get() = binding.userWelcome.text.toString()

    private var userIcon: String? = null
        set(value) {
            binding.userIcon.isGone = value == null || value.isEmpty()
            if (value == null) return
            if (field == value) return
            if (value.isNotEmpty()) {
                Glide.with(this)
                    .asDrawable()
                    .load(value)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(binding.userIcon)
                launch(Dispatchers.IO) {
                    val loadBlurIcon = viewModel.loadBlurIcon(value)
                    withContext(Dispatchers.Main) {
                        binding.userBack.setImageBitmap(loadBlurIcon)
                    }
                }
            }
            field = value
        }

    override fun createView(): MainActivityTempBinding {
        return MainActivityTempBinding.inflate(layoutInflater, root, false).apply {
            activity = this@MainActivity
            fitStatuesBarUsePadding(mainGroup)
            root.onGlobalLayout {
                actionBtnContainer.also {
                    it.y = it.y + viewModel.btnH * 2
                    viewModel.btnY = it.y
                }
                musicPlayer.duration = userConfig.lastMusicProgress
            }
        }
    }

    override suspend fun MainViewModel.init() {
        val musicController = MusicControllerIMP(binding.musicPlayer)
        musicController.setOnBlurAlbumCover {
            binding.userBack.setImageBitmap(it)
        }
        musicController.onClick {
            startActivity(MusicViewActivity::class.intent)
        }

        onResume = {
            userName = userConfig.userName
            userIcon = userConfig.userIcon.also {
                musicController.setInterceptAlbumCover(it.isEmpty())
            }
        }

        onFinish = {
            userConfig.lastMusicProgress = musicController.getProgress() ?: 0L
            userConfig.lastMusic = musicController.getPlayingMusic()?.toJson() ?: ""
            DatabaseHelper.instance.shutdownNow()
            stopService(WorkService::class.intent)
        }

        refreshModelList()
        bindMusicService {
            musicController.setBinder(this@MainActivity, it) { loopMode ->
                userConfig.musicLoopMode = loopMode
            }
            musicController.setLoopMode(userConfig.musicLoopMode)
            launch(Dispatchers.Default) {
                getMusics(userConfig.lastMusicBucket)?.let { list ->
                    list as MutableList<Music>
                    musicController.setMusicList(list)
                    musicController.refresh(
                        if (userConfig.lastMusic.isNotEmpty())
                            userConfig.lastMusic.toEntity(Music::class.java)
                        else if (list.isNotEmpty()) list[0] else getEmptyMusic(),
                        userConfig.lastMusicProgress
                    )
                }
            }
        }

//        startActivity(NoteActivity::class.intent)

        onViewEvent {
            when (it) {
                MainViewModel.MainViewEvent.Gallery ->
                    startActivity(GalleryActivity::class.intent)
                MainViewModel.MainViewEvent.Note ->
                    if (userConfig.lockNote == "")
                        startActivity(NoteActivity::class.intent)
                    else R.string.locked.getString().toast()
                MainViewModel.MainViewEvent.Music ->
                    if (userConfig.lockMusic == "")
                        startActivity(MusicActivity::class.intent)
                    else R.string.locked.getString().toast()
                MainViewModel.MainViewEvent.UserConfig ->
                    startActivity(UserConfigActivity::class.intent)
            }
        }
    }

    private suspend fun MainViewModel.refreshModelList() {
        getPhotoInToday()?.let { media ->
            Glide.with(this@MainActivity).load(media.uri).into(binding.photoCardPhoto)
            binding.photoCardTitle.text = media.date.toDateString("yyyy/MM/dd")
            binding.timePhoto.setOnClickListener {
                startActivity(GalleryViewActivity::class.intent.apply {
                    putExtra(GalleryViewViewModel.MEDIA, media.toJson())
                    putExtra(GalleryViewViewModel.IS_VIDEO, false)
                    putExtra(GalleryViewViewModel.GALLERY, R.string.all_gallery.getString())
                })
            }
        }
        getVideoInToday()?.let { media ->
            binding.videoPlayer.setVideoPath(media.uri)
            binding.videoCardTitle.text = media.date.toDateString()
            binding.videoPlayer.setFullScreen {
                startActivity(GalleryViewActivity::class.intent.apply {
                    putExtra(GalleryViewViewModel.MEDIA, media.toJson())
                    putExtra(GalleryViewViewModel.IS_VIDEO, true)
                    putExtra(GalleryViewViewModel.GALLERY, R.string.all_gallery.getString())
                })
            }
        }
    }
}