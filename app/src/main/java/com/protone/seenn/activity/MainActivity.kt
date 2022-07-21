package com.protone.seenn.activity

import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.entity.Music
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.todayDate
import com.protone.seen.adapter.MainModelListAdapter
import com.protone.seen.itemDecoration.ModelListItemDecoration
import com.protone.seenn.Medias
import com.protone.seenn.R
import com.protone.seenn.database.DatabaseHelper
import com.protone.seenn.database.userConfig
import com.protone.seenn.databinding.MainActivityBinding
import com.protone.seenn.service.WorkService
import com.protone.seenn.service.getEmptyMusic
import com.protone.seenn.viewModel.BaseViewModel
import com.protone.seenn.viewModel.MainViewModel
import com.protone.seenn.viewModel.MusicControllerIMP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<MainActivityBinding, MainViewModel>(false),
    ViewTreeObserver.OnGlobalLayoutListener {
    override val viewModel: MainViewModel by viewModels()

    private var userName: String? = null
        set(value) {
            if (value == null) return
            binding.userWelcome.text =
                if (value == "") getString(com.protone.seen.R.string.welcome_msg) else value
            binding.userDate.text = todayDate("yyyy/MM/dd")
            field = value
        }
        get() = binding.userWelcome.text.toString()

    private var userIcon: String? = null
        set(value) {
            if (value == null) return
            if (value.isNotEmpty()) {
                Glide.with(this)
                    .asDrawable()
                    .load(value)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(binding.userIcon)
            }
            field = value
        }

    override fun createView() {
        binding = MainActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@MainActivity
            fitNavigationBar(root)
            root.viewTreeObserver.addOnGlobalLayoutListener(this@MainActivity)
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
        refreshModelList()
    }

    override suspend fun onViewEvent(event: BaseViewModel.ViewEvent) = Unit

    override suspend fun MainViewModel.init() {
        val musicController = MusicControllerIMP(binding.musicPlayer)
        musicController.onClick {
            startActivity(MusicViewActivity::class.intent)
        }

        bindMusicService {
            musicController.setBinder(this@MainActivity, binder) {
                userConfig.musicLoopMode = it
            }
            musicController.setLoopMode(userConfig.musicLoopMode)
            launch(Dispatchers.IO) {
                while (isActive) {
                    if (Medias.musicBucketNotifier.value != null && Medias.musicBucketNotifier.value == 1) {
                        break
                    }
                }
                Medias.musicBucket[userConfig.lastMusicBucket]?.let {
                    musicController.setMusicList(it)
                    musicController.refresh(
                        if (userConfig.lastMusic.isNotEmpty())
                            userConfig.lastMusic.toEntity(Music::class.java)
                        else if (it.size > 0)it[0] else getEmptyMusic(), userConfig.lastMusicProgress
                    )
                }
            }
        }

        Medias.apply {
            galleyNotifier.collect {
                refreshModelList()
            }
            audioNotifier.collect {
                musicController.refresh()
            }
        }

        onFinish = {
            DatabaseHelper.instance.shutdownNow()
            stopService(WorkService::class.intent)
            userConfig.lastMusicProgress = musicController.getProgress() ?: 0L
            userConfig.lastMusic = binder.onMusicPlaying().value?.toJson() ?: ""
        }

    }

    override suspend fun doStart() {
        userName = userConfig.userName
        userIcon = userConfig.userIcon
    }

    fun onGalley() = startActivity(GalleyActivity::class.intent)

    fun onNote() = if (userConfig.lockNote == "")
        startActivity(NoteActivity::class.intent) else R.string.locked.getString().toast()

    fun onMusic() = if (userConfig.lockMusic == "")
        startActivity(MusicActivity::class.intent) else R.string.locked.getString().toast()

    fun onUserConfig() = startActivity(UserConfigActivity::class.intent)

    private fun refreshModelList() {
        binding.modelList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MainModelListAdapter(context)
            addItemDecoration(ModelListItemDecoration(0))
        }
    }

    override fun onGlobalLayout() {
        binding.apply {
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
            root.viewTreeObserver.removeOnGlobalLayoutListener(this@MainActivity)
        }
    }
}