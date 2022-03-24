package com.protone.seenn

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.protone.api.Config
import com.protone.api.TAG
import com.protone.api.checkNeededPermission
import com.protone.api.context.intent
import com.protone.api.requestContentPermission
import com.protone.database.room.config.UserConfig
import com.protone.database.room.dao.MusicBucketDAOHelper
import com.protone.database.room.dao.MusicDAOHelper
import com.protone.database.room.entity.Music
import com.protone.database.room.entity.MusicBucket
import com.protone.mediamodle.Galley
import com.protone.mediamodle.media.scanAudio
import com.protone.mediamodle.media.scanPicture
import com.protone.mediamodle.media.scanVideo
import com.protone.seen.SplashSeen
import com.protone.seenn.service.MusicService
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<SplashSeen>() {

    private val userConfig by lazy { UserConfig(this) }

    private val mHandler: Handler = Handler(Looper.getMainLooper()) {
        when (it.what) {
            1 -> {
                if (!userConfig.isFirstBoot) {
                    MusicBucketDAOHelper.addMusicBucket(MusicBucket("ALL", null,0,null,null))
                    MusicDAOHelper.insertMusicMulti(Galley.music)
                    userConfig.isFirstBoot = true
                }
                startService(Intent(this, MusicService::class.java))
                startActivity(MainActivity::class.intent)
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
                            Config.apply {
                                splashSeen.globalLayout = { height, width ->
                                    screenHeight = height
                                    screenWidth = width
                                }
                            }
                            checkNeededPermission({
                                requestContentPermission()
                            }, {
                                mHandler.sendEmptyMessage(2)
                                registerBroadcast()
                            })
                        }
                        else -> {}
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
        Thread {
            Galley.apply {
                photo = scanPicture()
                video = scanVideo()
                music = scanAudio()
            }
            mHandler.sendEmptyMessage(1)
        }.start()
    }
}