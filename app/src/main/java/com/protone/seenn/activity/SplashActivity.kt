package com.protone.seenn.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import com.protone.api.context.checkNeededPermission
import com.protone.api.context.*
import com.protone.api.context.requestContentPermission
import com.protone.mediamodle.GalleyHelper
import com.protone.seenn.broadcast.workLocalBroadCast
import com.protone.seenn.databinding.SplashActivityBinding
import com.protone.seenn.service.MusicService
import com.protone.seenn.service.WorkService
import com.protone.seenn.viewModel.SplashViewModel

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<SplashActivityBinding, SplashViewModel>(true),
    ViewTreeObserver.OnGlobalLayoutListener {

    override val viewModel: SplashViewModel by viewModels()

    override fun createView() {
        binding = SplashActivityBinding.inflate(layoutInflater, root, false)
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override suspend fun SplashViewModel.init() {
        startService(WorkService::class.intent)
    }

    override suspend fun doStart() {
        checkNeededPermission({
            requestContentPermission()
        }, {
            sendViewEvent(SplashViewModel.ViewEvent.UpdateMedia.name)
        })
    }

    override suspend fun onViewEvent(event: String) {
        when (event) {
            SplashViewModel.ViewEvent.InitConfig.name -> {
                viewModel.firstBootWork()
                startService(MusicService::class.intent)
                startActivity(MainActivity::class.intent)
                finish()
            }
            SplashViewModel.ViewEvent.UpdateMedia.name -> updateMedia()
        }
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
            sendViewEvent(SplashViewModel.ViewEvent.UpdateMedia.name)
        } else {
            finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun updateMedia() {
        workLocalBroadCast.sendBroadcast(Intent().setAction(UPDATE_GALLEY))
        workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC))
        GalleyHelper.updateAll {
            sendViewEvent(SplashViewModel.ViewEvent.InitConfig.name)
        }
    }

    override fun onGlobalLayout() {
        binding.root.let {
            SApplication.apply {
                screenHeight = it.measuredHeight
                screenWidth = it.measuredWidth
            }
            it.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }
}