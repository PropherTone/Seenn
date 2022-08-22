package com.protone.seenn.activity

import android.view.View
import androidx.activity.viewModels
import com.protone.api.context.root
import com.protone.seenn.databinding.MusicActivtiyBinding
import com.protone.seenn.viewModel.MusicControllerIMP
import com.protone.seenn.viewModel.TempMusicModel

class TempMusicActivity : BaseActivity<MusicActivtiyBinding, TempMusicModel>(true) {

    override val viewModel: TempMusicModel by viewModels()

    override fun createView(): View {
        binding = MusicActivtiyBinding.inflate(layoutInflater, root, false)
        return binding.root
    }

    override suspend fun TempMusicModel.init() {
        val musicControllerIMP = MusicControllerIMP(binding.mySmallMusicPlayer)
        bindMusicService {
            musicControllerIMP.setBinder(this@TempMusicActivity,it)
        }

        onViewEvent {

        }
    }
}