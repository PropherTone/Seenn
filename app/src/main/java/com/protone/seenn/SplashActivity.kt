package com.protone.seenn

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.protone.api.checkNeededPermission
import com.protone.api.context.UPDATE_GALLEY
import com.protone.api.context.UPDATE_MUSIC
import com.protone.api.context.intent
import com.protone.api.requestContentPermission
import com.protone.api.toBitmapByteArray
import com.protone.api.todayDate
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.MusicBucket
import com.protone.database.sp.config.userConfig
import com.protone.mediamodle.GalleyHelper
import com.protone.mediamodle.Medias
import com.protone.seen.SplashSeen
import com.protone.seenn.broadcast.workLocalBroadCast
import com.protone.seenn.service.MusicService
import com.protone.seenn.service.WorkService
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<SplashSeen>() {

    private val mHandler: Handler = Handler(Looper.getMainLooper()) {
        when (it.what) {
            1 -> {
                if (userConfig.isFirstBoot) {
                    DataBaseDAOHelper.addMusicBucketThread(
                        MusicBucket(
                            getString(R.string.all_music),
                            if (Medias.music.size > 0) Medias.music[0].uri.toBitmapByteArray() else null,
                            Medias.music.size,
                            null,
                            todayDate("yyyy/MM/dd")
                        )
                    )
                    DataBaseDAOHelper.insertMusicMulti(Medias.music)
                    userConfig.apply {
                        isFirstBoot = false
                        lastMusicBucket = getString(R.string.all_music)
                        playedMusicPosition = -1
                    }
                }
                startService(MusicService::class.intent)
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
        startService(WorkService::class.intent)

        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnStart -> {
                            checkNeededPermission({
                                requestContentPermission()
                            }, {
                                mHandler.sendEmptyMessage(2)
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
        } else {
            finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun updateMedia() {
        workLocalBroadCast.sendBroadcast(Intent().setAction(UPDATE_GALLEY))
        workLocalBroadCast.sendBroadcast(Intent(UPDATE_MUSIC))
        GalleyHelper.updateAll {
            mHandler.sendEmptyMessage(1)
        }
    }
}