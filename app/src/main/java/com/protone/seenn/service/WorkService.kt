package com.protone.seenn.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.protone.api.context.Global
import com.protone.api.context.onBackground
import com.protone.api.context.workIntentFilter
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.dao.DataBaseDAOHelper.deleteMusicMulti
import com.protone.database.room.dao.DataBaseDAOHelper.getAllMusic
import com.protone.database.room.dao.DataBaseDAOHelper.getAllMusicBucket
import com.protone.database.room.dao.DataBaseDAOHelper.getMusicBucketByName
import com.protone.database.room.dao.DataBaseDAOHelper.insertMusicMulti
import com.protone.database.room.dao.DataBaseDAOHelper.sortSignedMedia
import com.protone.database.room.dao.DataBaseDAOHelper.updateMusicBucketBack
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Music
import com.protone.mediamodle.Galley.music
import com.protone.mediamodle.Galley.musicBucket
import com.protone.mediamodle.Galley.musicBucketLive
import com.protone.mediamodle.GalleyHelper
import com.protone.mediamodle.IWorkService
import com.protone.mediamodle.WorkReceiver
import com.protone.mediamodle.media.scanPicture
import com.protone.mediamodle.media.scanVideo
import com.protone.mediamodle.workLocalBroadCast
import com.protone.seenn.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Consumer
import java.util.stream.Collectors

class WorkService : Service(), CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val workReceiver: BroadcastReceiver = object : WorkReceiver() {
        override fun updateMusicBucket() {
            this@WorkService.updateMusicBucket()
        }

        override fun updateMusic() {
            this@WorkService.updateMusic()
        }

        override fun updateGalley() {
            this@WorkService.updateGalley()
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
            allMusic?.forEach { music ->
                music.myBucket.forEach(Consumer {
                    musicBucket[it]?.add(music)
                })
                if (cacheList.contains(music)) {
                    cacheList.remove(music)
                    cacheAllMusic.remove(music)
                }
            }
            if (cacheList.size > 0) {
                insertMusicMulti(cacheList)
            } else if (cacheAllMusic.size > 0) {
                deleteMusicMulti(cacheAllMusic)
            }
            getAllMusic { music = it as MutableList<Music> }
            musicBucket.keys.forEach {
                getMusicBucketByName(it)?.let { mb ->
                    musicBucket[it]?.size?.let { size ->
                        mb.size = size
                        updateMusicBucketBack(mb)
                    }
                }
            }
            musicBucketLive.postValue(musicBucket.keys)
        }
    }

    private fun updateMusic() {}
    private fun updateGalley() = DataBaseDAOHelper.run {
        fun sortMedia(allSignedMedia: MutableList<GalleyMedia>, galleyMedia: GalleyMedia) {
            allSignedMedia.stream().filter { it.uri == galleyMedia.uri }
                .collect(Collectors.toList()).let { list ->
                    if (list.size > 0) allSignedMedia.remove(list[0])
                }
        }
        launch {
            val allSignedMedia = getAllSignedMedia() as MutableList
            val scanPicture = launch {
                scanPicture { _, galleyMedia ->
                    synchronized(allSignedMedia) {
                        sortMedia(allSignedMedia, galleyMedia)
                    }
                    sortSignedMedia(galleyMedia)
                }
            }
            val scanVideo = launch {
                scanVideo { _, galleyMedia ->
                    synchronized(allSignedMedia) {
                        sortMedia(allSignedMedia, galleyMedia)
                    }
                    sortSignedMedia(galleyMedia)
                }
            }
            while (scanPicture.isActive || scanVideo.isActive) continue
            allSignedMedia.forEach {
                deleteSignedMedia(it)
            }
        }
    }

    private inner class WorkBinder : Binder(), IWorkService {
        override fun updateMusicBucket() {
            this@WorkService.updateMusicBucket()
        }

        override fun updateMusic() {
            this@WorkService.updateMusic()
        }

        override fun updateGalley() {
            this@WorkService.updateGalley()
        }
    }
}