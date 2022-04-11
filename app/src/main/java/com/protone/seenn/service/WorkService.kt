package com.protone.seenn.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.protone.api.TAG
import com.protone.api.context.Global
import com.protone.api.context.workIntentFilter
import com.protone.database.room.dao.DataBaseDAOHelper.deleteMusicMulti
import com.protone.database.room.dao.DataBaseDAOHelper.getAllMusic
import com.protone.database.room.dao.DataBaseDAOHelper.getAllMusicBucket
import com.protone.database.room.dao.DataBaseDAOHelper.insertMusicMulti
import com.protone.database.room.entity.Music
import com.protone.mediamodle.Galley
import com.protone.mediamodle.Galley.music
import com.protone.mediamodle.Galley.musicBucket
import com.protone.mediamodle.IWorkService
import com.protone.mediamodle.WorkReceiver
import com.protone.mediamodle.workLocalBroadCast
import com.protone.seenn.R
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Consumer

class WorkService : Service() {
    private val workReceiver: BroadcastReceiver = object : WorkReceiver() {
        override fun updateMusicBucket() {
            this@WorkService.updateMusicBucket()
        }

        override fun updateMusic() {
            this@WorkService.updateMusic()
        }

        override fun updatePhoto() {
            this@WorkService.updatePhoto()
        }

        override fun updateVideo() {
            this@WorkService.updateVideo()
        }
    }
    private val executor: Executor = Executors.newCachedThreadPool()

    override fun onCreate() {
        super.onCreate()
        workLocalBroadCast.registerReceiver(workReceiver, workIntentFilter)
    }

    override fun onBind(intent: Intent): IBinder {
        return WorkBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        workLocalBroadCast.unregisterReceiver(workReceiver)
    }

    private fun updateMusicBucket() {
        executor.execute {
            val cacheList = arrayListOf<Music>().apply { addAll(music) }
            val cacheAllMusic = arrayListOf<Music>()
            val allMusic = (getAllMusic() as ArrayList?)?.also { cacheAllMusic.addAll(it) }
            val allMusicBucket = getAllMusicBucket().let { l ->
                (l as ArrayList).stream().filter {
                    it.name != Global.application.getString(R.string.all_music)
                }
            }
            allMusicBucket?.forEach { (name) -> musicBucket[name] = ArrayList() }
            Log.d(TAG, "updateMusicBucket: update")
            allMusic?.forEach { music ->
                music.myBucket?.forEach(Consumer {
                    Log.d(TAG, "updateMusicBucket: ${music.title}")
                    Log.d(TAG, "updateMusicBucket: $it")
                    musicBucket[it]?.add(music)
                })
                cacheList.stream().filter { it.title != music.title }
                if (cacheList.size > 0) {
                    cacheAllMusic.remove(music)
                }
            }
            if (cacheList.size > 0) {
                insertMusicMulti(cacheList)
            } else if (cacheAllMusic.size > 0) {
                deleteMusicMulti(cacheAllMusic)
            }
            getAllMusic { music = it as MutableList<Music> }
        }
    }

    private fun updateMusic() {}
    private fun updatePhoto() {}
    private fun updateVideo() {}
    private inner class WorkBinder : Binder(), IWorkService {
        override fun UpdateMusicBucket() {
            updateMusicBucket()
        }

        override fun UpdateMusic() {
            updateMusic()
        }

        override fun UpdatePhoto() {
            updatePhoto()
        }

        override fun UpdateVideo() {
            updateVideo()
        }
    }
}