package com.protone.seenn.activity

import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.root
import com.protone.api.context.statuesBarHeight
import com.protone.api.entity.Music
import com.protone.seen.adapter.TransparentPlayListAdapter
import com.protone.seen.itemDecoration.GalleyItemDecoration
import com.protone.seenn.database.userConfig
import com.protone.seenn.databinding.MusicViewActivityBinding
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.seenn.viewModel.MusicViewModel

class MusicViewActivity : BaseActivity<MusicViewActivityBinding, MusicViewModel>(false) {
    override val viewModel: MusicViewModel by viewModels()

    override fun createView(): View {
        binding = MusicViewActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        binding.toolBar.layoutParams =
            binding.toolBar.layoutParams.apply { height = statuesBarHeight }
        return binding.root
    }

    override suspend fun MusicViewModel.init() {
        val musicController = MusicControllerIMP(binding.musicPlayer)
        bindMusicService {
            musicController.setBinder(this@MusicViewActivity, it) {
                userConfig.musicLoopMode = it
            }
            musicController.refresh()
            musicController.setLoopMode(userConfig.musicLoopMode)
            initPlayList(
                it.getPlayList(), musicController.getPlayingMusic(),
                object : TransparentPlayListAdapter.OnPlayListClk {
                    override fun onClk(music: Music) {
                        musicController.play(music)
                    }
                })
            musicController.binder?.onMusicPlaying()?.observe(this@MusicViewActivity){
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
            addItemDecoration(GalleyItemDecoration(paddingEnd))
        }
    }

    fun showPop() {
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup?)
        binding.pop.isGone = !binding.pop.isGone
    }
}