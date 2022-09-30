package com.protone.seenn.activity

import android.transition.TransitionManager
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.root
import com.protone.api.context.statuesBarHeight
import com.protone.api.entity.Music
import com.protone.seenn.databinding.MusicViewActivityBinding
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.ui.adapter.TransparentPlayListAdapter
import com.protone.ui.itemDecoration.GalleryItemDecoration
import com.protone.worker.database.userConfig
import com.protone.worker.viewModel.BaseViewModel
import com.protone.worker.viewModel.MusicViewModel

class MusicViewActivity :
    BaseActivity<MusicViewActivityBinding, MusicViewModel, BaseViewModel.ViewEvent>(false) {
    override val viewModel: MusicViewModel by viewModels()

    override fun createView(): MusicViewActivityBinding {
        return MusicViewActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@MusicViewActivity
            toolBar.layoutParams = toolBar.layoutParams.apply { height = statuesBarHeight }
        }
    }

    override suspend fun MusicViewModel.init() {
        val musicController = MusicControllerIMP(binding.musicPlayer)
        bindMusicService { binder ->
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
            musicController.binder?.onMusicPlaying()?.observe(this@MusicViewActivity) {
                (binding.playList.adapter as TransparentPlayListAdapter).setOnPlay(it)
            }
        }
    }

    private fun initPlayList(
        playList: MutableList<Music>, onPlay: Music?,
        listener: TransparentPlayListAdapter.OnPlayListClk
    ) {
        binding.playList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TransparentPlayListAdapter(context, onPlay, playList).also {
                it.onPlayListClkListener = listener
            }
            addItemDecoration(GalleryItemDecoration(paddingEnd))
        }
    }

    fun showPop() {
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup?)
        binding.pop.isGone = !binding.pop.isGone
    }
}