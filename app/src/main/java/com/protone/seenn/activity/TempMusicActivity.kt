package com.protone.seenn.activity

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import com.protone.api.animation.AnimationHelper
import com.protone.api.context.navigationBarHeight
import com.protone.api.context.root
import com.protone.api.context.statuesBarHeight
import com.protone.seenn.databinding.MusicActivtiyBinding
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.ui.customView.StatusImageView
import com.protone.worker.viewModel.TempMusicModel

class TempMusicActivity : BaseActivity<MusicActivtiyBinding, TempMusicModel>(true),
    StatusImageView.StateListener,
    ViewTreeObserver.OnGlobalLayoutListener {
    override val viewModel: TempMusicModel by viewModels()

    private var containerAnimator: ObjectAnimator? = null

    override fun createView(): View {
        binding = MusicActivtiyBinding.inflate(layoutInflater, root, false).apply {
            root.viewTreeObserver.addOnGlobalLayoutListener(this@TempMusicActivity)
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

        onViewEvent {

        }
    }

    override fun onActive() {
        TODO("Not yet implemented")
    }

    override fun onNegative() {
        TODO("Not yet implemented")
    }

    override fun onGlobalLayout() {
        binding.apply {
            appToolbar.setPadding(
                appToolbar.paddingLeft,
                appToolbar.paddingTop + statuesBarHeight,
                appToolbar.paddingRight,
                appToolbar.paddingBottom
            )
            musicBucketContainer.let {
                it.setPadding(
                    it.paddingLeft,
                    it.paddingTop,
                    it.paddingRight,
                    navigationBarHeight + musicAddBucket.measuredHeight - musicAddBucket.paddingBottom
                )
                containerAnimator = AnimationHelper.translationY(
                    it,
                    it.height.toFloat() - mySmallMusicPlayer.measuredHeight.toFloat()
                )

                it.y = toolbar.minHeight + statuesBarHeight.toFloat()
            }
            musicShowBucket.setOnStateListener(this@TempMusicActivity)
            musicMusicList.setPadding(0, 0, 0, mySmallMusicPlayer.height)
            var value: ViewTreeObserver.OnGlobalLayoutListener? = null
            value = ViewTreeObserver.OnGlobalLayoutListener {
                appToolbar.setExpanded(false, false)
                appToolbar.viewTreeObserver.removeOnGlobalLayoutListener(value)
            }
            appToolbar.viewTreeObserver.addOnGlobalLayoutListener(value)
            root.viewTreeObserver.removeOnGlobalLayoutListener(this@TempMusicActivity)
        }
    }
}