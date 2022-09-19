package com.protone.seenn.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import androidx.activity.viewModels
import com.protone.api.context.*
import com.protone.seenn.broadcast.workLocalBroadCast
import com.protone.seenn.databinding.SplashActivityBinding
import com.protone.seenn.service.MusicService
import com.protone.seenn.service.WorkService
import com.protone.worker.viewModel.SplashViewModel

@SuppressLint("CustomSplashScreen")
class SplashActivity :
    BaseActivity<SplashActivityBinding, SplashViewModel, SplashViewModel.SplashEvent>(true) {

    override val viewModel: SplashViewModel by viewModels()

    override fun createView(): View {
        binding = SplashActivityBinding.inflate(layoutInflater, root, false)
        binding.root.apply {
            onGlobalLayout {
                SApplication.apply {
                    screenHeight = measuredHeight
                    screenWidth = measuredWidth
                }
            }
        }
        return binding.root
    }

    override suspend fun SplashViewModel.init() {
        startService(WorkService::class.intent)

        onViewEvent {
            when (it) {
                SplashViewModel.SplashEvent.InitConfig -> {
                    viewModel.firstBootWork()
                    startService(MusicService::class.intent)
                    startActivity(MainActivity::class.intent)
                    finish()
                }
                SplashViewModel.SplashEvent.UpdateMedia -> updateMedia()
            }
        }
    }

    override suspend fun doStart() {
        checkNeededPermission({
            requestContentPermission()
        }, {
            sendViewEvent(SplashViewModel.SplashEvent.UpdateMedia)
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED
        ) {
            sendViewEvent(SplashViewModel.SplashEvent.UpdateMedia)
        } else {
            finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun updateMedia() {
        workLocalBroadCast.sendBroadcast(Intent(UPDATE_GALLEY))
        workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC))
        sendViewEvent(SplashViewModel.SplashEvent.InitConfig)
    }
}