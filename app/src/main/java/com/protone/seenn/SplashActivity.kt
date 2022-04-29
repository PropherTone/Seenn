package com.protone.seenn

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.protone.api.checkNeededPermission
import com.protone.api.context.intent
import com.protone.api.requestContentPermission
import com.protone.api.toBitmapByteArray
import com.protone.api.todayTime
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.mediamodle.Galley
import com.protone.mediamodle.GalleyHelper
import com.protone.seen.SplashSeen
import com.protone.seenn.service.MusicService
import com.protone.seenn.service.WorkService
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<SplashSeen>() {

    private val mHandler: Handler = Handler(Looper.getMainLooper()) {
        when (it.what) {
            1 -> {
                if (!userConfig.isFirstBoot) {
                    DataBaseDAOHelper.addMusicBucketThread(
                        MusicBucket(
                            getString(R.string.all_music),
                            if (Galley.music.size > 0) Galley.music[0].uri.toBitmapByteArray() else null,
                            Galley.music.size,
                            null,
                            todayTime("yyyy/MM/dd")
                        )
                    )
                    DataBaseDAOHelper.insertMusicMulti(Galley.music)
                    userConfig.apply {
                        isFirstBoot = true
                        playedMusicBucket = getString(R.string.all_music)
                        playedMusicPosition = 0
                    }
                }
                startService(Intent(this, MusicService::class.java))
                startService(Intent(this, WorkService::class.java))
                startActivity(MainActivity::class.intent)
                finish()
            }
            2 -> updateMedia()
        }
        true
    }

    override suspend fun main() {
        val splashSeen = SplashSeen(this)
        setContentSeen(splashSeen)
        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnStart -> {
                            checkNeededPermission({
                                requestContentPermission()
                            }, {
                                mHandler.sendEmptyMessage(2)
                                registerBroadcast()
                            })
                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            mHandler.sendEmptyMessage(2)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun registerBroadcast() {
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaContentObserver
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaContentObserver
        )
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaContentObserver
        )
    }

    private fun updateMedia() {
        GalleyHelper.run {
            updateAll {
                mHandler.sendEmptyMessage(1)
            }
        }
    }
}