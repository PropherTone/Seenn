package com.protone.seenn.activity

import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.intent
import com.protone.api.context.onGlobalLayout
import com.protone.api.context.root
import com.protone.api.entity.Music
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.todayDate
import com.protone.seenn.databinding.MainActivityBinding
import com.protone.seenn.service.WorkService
import com.protone.seenn.service.getEmptyMusic
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.ui.adapter.MainModelListAdapter
import com.protone.ui.itemDecoration.ModelListItemDecoration
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.userConfig
import com.protone.worker.viewModel.GalleyViewViewModel
import com.protone.worker.viewModel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity<MainActivityBinding, MainViewModel,MainViewModel.MainViewEvent>(true),
    MainModelListAdapter.ModelClk {
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
                        Glide.with(this@MainActivity)
                            .asDrawable()
                            .load(loadBlurIcon)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(binding.userBack)
                    }
                }
            }
            field = value
        }

    override fun createView(): View {
        binding = MainActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@MainActivity
            root.onGlobalLayout {
                actionBtnContainer.also {
                    it.y = it.y + viewModel.btnH * 2
                    viewModel.btnY = it.y
                }
                toolbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
                    binding.toolMotion.progress =
                        -verticalOffset / appBarLayout.totalScrollRange.toFloat().also {
                            binding.musicPlayer.isVisible = it > 0.7f
                            binding.actionBtnContainer.also { btn ->
                                btn.y =
                                    viewModel.btnY - (viewModel.btnH * binding.toolMotion.progress) * 2
                            }
                        }
                }
            }
            musicPlayer.apply {
                duration
            }
            modelList.apply {
                setPadding(
                    paddingLeft,
                    paddingTop,
                    paddingRight,
                    viewModel.btnH
                )
            }

        }

        return binding.root
    }

    override suspend fun MainViewModel.init() {
        val musicController = MusicControllerIMP(binding.musicPlayer)
        musicController.onClick {
            startActivity(MusicViewActivity::class.intent)
        }

        refreshModelList()
        bindMusicService {
            (binding.modelList.adapter as MainModelListAdapter).loadDataBelow()
            musicController.setBinder(this@MainActivity, it) { binder ->
                userConfig.musicLoopMode = binder
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


        onFinish = {
            DatabaseHelper.instance.shutdownNow()
            stopService(WorkService::class.intent)
            userConfig.lastMusicProgress = musicController.getProgress() ?: 0L
            userConfig.lastMusic = musicController.getPlayingMusic()?.toJson() ?: ""
        }

        onViewEvent {
            when (it) {
                MainViewModel.MainViewEvent.Galley ->
                    startActivity(GalleyActivity::class.intent)
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

    override suspend fun doResume() {
        userName = userConfig.userName
        userIcon = userConfig.userIcon
    }

    private fun refreshModelList() {
        binding.modelList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MainModelListAdapter(
                context,
                object : MainModelListAdapter.MainModelListAdapterDataProxy {
                    override fun photoInTodayJson(): String? {
                        return viewModel.photoInTodayJson()
                    }

                    override fun videoInTodayJson(): String? {
                        return viewModel.videoInTodayJson()
                    }

                    override fun randomNoteJson(): String? {
                        return viewModel.randomNoteJson()
                    }
                }
            ).apply {
                modelClkListener = this@MainActivity
            }
            addItemDecoration(ModelListItemDecoration(0))
        }
    }

    override fun onPhoto(json: String) {
        startActivity(GalleyViewActivity::class.intent.apply {
            putExtra(GalleyViewViewModel.MEDIA, json)
            putExtra(GalleyViewViewModel.TYPE, false)
            putExtra(GalleyViewViewModel.GALLEY, R.string.all_galley.getString())
        })
    }

    override fun onNote(json: String) {
    }

    override fun onVideo(json: String) {
    }

    override fun onMusic(json: String) {
    }
}