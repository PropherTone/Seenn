package com.protone.seenn.activity

import android.content.Intent
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.appbar.AppBarLayout
import com.protone.api.context.UPDATE_MUSIC_BUCKET
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.todayDate
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.Music
import com.protone.database.sp.config.userConfig
import com.protone.mediamodle.Medias
import com.protone.seen.adapter.MainModelListAdapter
import com.protone.seen.itemDecoration.ModelListItemDecoration
import com.protone.seenn.MusicActivity
import com.protone.seenn.MusicViewActivity
import com.protone.seenn.R
import com.protone.seenn.UserConfigActivity
import com.protone.seenn.broadcast.workLocalBroadCast
import com.protone.seenn.databinding.MainActivityBinding
import com.protone.seenn.service.WorkService
import com.protone.seenn.viewModel.MainViewModel
import com.protone.seenn.viewModel.MusicControllerIMP

class MainActivity : BaseActivity<MainActivityBinding, MainViewModel>(),
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

    override suspend fun initView() {
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

    override suspend fun init() {
        val musicController = MusicControllerIMP(binding.musicPlayer)
        musicController.onClick {
            startActivity(MusicViewActivity::class.intent)
        }

        bindMusicService {
            musicController.setBinder(this, binder) {
                userConfig.musicLoopMode = it
            }
            musicController.setLoopMode(userConfig.musicLoopMode)
            Medias.musicBucket[userConfig.lastMusicBucket]?.let {
                musicController.setMusicList(it)
                musicController.refresh(
                    if (userConfig.lastMusic.isNotEmpty()) userConfig.lastMusic.toEntity(
                        Music::class.java
                    ) else binder.getPlayList()[0], userConfig.lastMusicProgress
                )
            }
        }

        Medias.mediaLive.observe(this) { code ->
            if (code == Medias.AUDIO_UPDATED) {
                musicController.refresh()
                workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC_BUCKET))
            } else {
                refreshModelList()
            }
        }

        onFinish = {
            DataBaseDAOHelper.shutdownNow()
            stopService(WorkService::class.intent)
            userConfig.lastMusicProgress = musicController.getProgress() ?: 0L
            userConfig.lastMusic = binder.onMusicPlaying().value?.toJson() ?: ""
            musicController.finish()
        }
    }

    override fun onStart() {
        super.onStart()
        userName = userConfig.userName
        userIcon = userConfig.userIcon
    }

    fun onGalley() = startActivity(GalleyActivity::class.intent)

    fun onNote() = if (userConfig.lockMusic == "")
        startActivity(MusicActivity::class.intent) else toast(getString(R.string.locked))

    fun onMusic() = if (userConfig.lockNote == "")
        startActivity(NoteActivity::class.intent) else toast(getString(R.string.locked))

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
            toolbar.addOnOffsetChangedListener(
                AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                    binding.toolMotion.progress =
                        -verticalOffset / appBarLayout.totalScrollRange.toFloat().also {
                            binding.musicPlayer.isVisible = it > 0.7f
                            binding.actionBtnContainer.also { btn ->
                                btn.y = viewModel.btnY - (viewModel.btnH * binding.toolMotion.progress) * 2
                            }
                        }
                })
            root.viewTreeObserver.removeOnGlobalLayoutListener(this@MainActivity)
        }
    }
}