package com.protone.seenn.activity

import android.view.View
import androidx.activity.viewModels
import com.protone.api.context.onGlobalLayout
import com.protone.api.context.paddingTop
import com.protone.api.context.root
import com.protone.api.context.statuesBarHeight
import com.protone.seenn.databinding.MusicActivtiyBinding
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.ui.customView.StatusImageView
import com.protone.worker.viewModel.TempMusicModel

class TempMusicActivity : BaseActivity<MusicActivtiyBinding, TempMusicModel>(true),
    StatusImageView.StateListener {
    override val viewModel: TempMusicModel by viewModels()

    override fun createView(): View {
        binding = MusicActivtiyBinding.inflate(layoutInflater, root, false).apply {
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
        val musicControllerIMP = MusicControllerIMP(binding.mySmallMusicPlayer)
        bindMusicService {
            musicControllerIMP.setBinder(this@TempMusicActivity, it)
        }

        initList()

        onViewEvent {

        }
    }

    override fun onActive() {
        TODO("Not yet implemented")
    }

    override fun onNegative() {
        TODO("Not yet implemented")
    }

    private fun initList(){

    }

}