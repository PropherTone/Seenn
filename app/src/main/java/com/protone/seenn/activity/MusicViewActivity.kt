package com.protone.seenn.activity

import android.transition.TransitionManager
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.root
import com.protone.api.context.statuesBarHeight
import com.protone.database.room.entity.Music
import com.protone.database.sp.config.userConfig
import com.protone.seen.adapter.TransparentPlayListAdapter
import com.protone.seen.itemDecoration.GalleyItemDecoration
import com.protone.seenn.databinding.MusicViewActivityBinding
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.seenn.viewModel.MusicViewModel

class MusicViewActivity : BaseActivity<MusicViewActivityBinding, MusicViewModel>(false) {
    override val viewModel: MusicViewModel by viewModels()

    override fun initView() {
        binding = MusicViewActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitNavigationBarUsePadding(binding.musicPlayer)
        binding.toolBar.apply {
            setPadding(
                paddingLeft,
                paddingTop + context.statuesBarHeight,
                paddingRight,
                paddingBottom
            )
        }
    }

    override suspend fun MusicViewModel.init() {
        val musicController = MusicControllerIMP(binding.musicPlayer)
        bindMusicService {
            musicController.setBinder(this@MusicViewActivity, binder) {
                userConfig.musicLoopMode = it
            }
            musicController.refresh()
            musicController.setLoopMode(userConfig.musicLoopMode)
            initPlayList(
                binder.getPlayList(), musicController.getPlayingMusic(),
                object : TransparentPlayListAdapter.OnPlayListClk {
                    override fun onClk(music: Music) {
                        musicController.play(music)
                    }
                })
        }
        onFinish = {
            musicController.finish()
        }
    }

    override suspend fun onViewEvent(event: String) = Unit

    private fun initPlayList(
        playList: MutableList<Music>, onPlay: Music?,
        listener: TransparentPlayListAdapter.OnPlayListClk
    ) {
        binding.playList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TransparentPlayListAdapter(context, onPlay, playList).also {
                it.onPlayListClkListener = listener
            }
            addItemDecoration(GalleyItemDecoration(paddingEnd))
        }
    }

    fun showPop() {
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup?)
        binding.pop.isGone = !binding.pop.isGone
    }
}