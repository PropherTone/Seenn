package com.protone.seenn.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import com.protone.api.context.onUiThread
import com.protone.api.context.workIntentFilter
import com.protone.api.getString
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.dao.DataBaseDAOHelper.deleteMusicMulti
import com.protone.database.room.dao.DataBaseDAOHelper.getAllMusic
import com.protone.database.room.dao.DataBaseDAOHelper.getAllMusicBucket
import com.protone.database.room.dao.DataBaseDAOHelper.getAllMusicRs
import com.protone.database.room.dao.DataBaseDAOHelper.getMusicBucketByName
import com.protone.database.room.dao.DataBaseDAOHelper.insertMusicMulti
import com.protone.database.room.dao.DataBaseDAOHelper.updateMusicBucketBack
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Music
import com.protone.mediamodle.Medias.AUDIO_UPDATED
import com.protone.mediamodle.Medias.GALLEY_UPDATED
import com.protone.mediamodle.Medias.mediaLive
import com.protone.mediamodle.Medias.music
import com.protone.mediamodle.Medias.musicBucket
import com.protone.mediamodle.Medias.musicBucketLive
import com.protone.mediamodle.media.*
import com.protone.seenn.R
import com.protone.seenn.broadcast.IWorkService
import com.protone.seenn.broadcast.MediaContentObserver
import com.protone.seenn.broadcast.WorkReceiver
import com.protone.seenn.broadcast.workLocalBroadCast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.function.Consumer
import java.util.stream.Collectors

class WorkService : Service(), CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val workReceiver: BroadcastReceiver = object : WorkReceiver() {
        override fun updateMusicBucket() {
            this@WorkService.updateMusicBucket()
        }

        override fun updateMusic(data: Uri?) {
            if (data != null) {
                this@WorkService.updateMusic(data)
            } else {
                this@WorkService.updateMusic()
            }
        }

        override fun updateGalley(data: Uri?) {
            if (data != null) {
                this@WorkService.updateGalley(data)
            } else {
                this@WorkService.updateGalley()
            }
        }
    }

    private val mediaContentObserver = MediaContentObserver(Handler(Looper.getMainLooper()))

    override fun onCreate() {
        super.onCreate()
        registerBroadcast()
        workLocalBroadCast.registerReceiver(workReceiver, workIntentFilter)
    }

    override fun onBind(intent: Intent): IBinder {
        return WorkBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
        workLocalBroadCast.unregisterReceiver(workReceiver)
        contentResolver.unregisterContentObserver(mediaContentObserver)
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

    private fun updateMusicBucket() = launch {
        val cacheList = arrayListOf<Music>().apply { addAll(music) }
        val cacheAllMusic = arrayListOf<Music>()
        val allMusic = (getAllMusic() as ArrayList?)?.also { cacheAllMusic.addAll(it) }
        val allMusicBucket = getAllMusicBucket().let { l ->
            (l as ArrayList).filter {
                it.name != R.string.all_music.getString()
            }
        }
        allMusicBucket.forEach { (name) -> musicBucket[name] = ArrayList() }
        allMusic?.also {
            it.asFlow().onEach { music ->
                music.myBucket.forEach(Consumer {
                    musicBucket[it]?.add(music)
                })
            }.buffer().collect { music ->
                if (cacheList.contains(music)) {
                    cacheList.remove(music)
                    cacheAllMusic.remove(music)
                }
            }
        }
        if (cacheList.size > 0) {
            insertMusicMulti(cacheList)
        } else if (cacheAllMusic.size > 0) {
            deleteMusicMulti(cacheAllMusic)
        }
        getAllMusicRs()?.let {
            music = it as MutableList<Music>
        }
        musicBucket.keys.forEach {
            getMusicBucketByName(it)?.let { mb ->
                musicBucket[it]?.size?.let { size ->
                    mb.size = size
                    updateMusicBucketBack(mb)
                }
            }
        }
        musicBucketLive.postValue(1)
        makeToast("歌单更新完毕")
        cancel()
    }

    private fun updateMusic(uri: Uri) {
        scanAudioWithUri(uri) {
            DataBaseDAOHelper.insertMusic(it)
            mediaLive.postValue(AUDIO_UPDATED)
        }
        makeToast("音乐更新完毕")
    }

    private fun updateMusic() {
        fun sortMusic(allMusic: MutableList<Music>, music: Music) {
            allMusic.stream().filter { it.uri == music.uri }
                .collect(Collectors.toList()).let { list ->
                    if (list.size > 0) allMusic.remove(list[0])
                }
        }
        launch {
            val allMusic = getAllMusic() as MutableList
            flow {
                scanAudio { _, music ->
                    emit(music)
                    DataBaseDAOHelper.insertMusicCheck(music)
                }
            }.buffer().collect {
                synchronized(allMusic) { sortMusic(allMusic, it) }
            }
            deleteMusicMulti(allMusic)
            mediaLive.postValue(AUDIO_UPDATED)
            makeToast("音乐更新完毕")
            cancel()
        }
    }

    private fun updateGalley(uri: Uri) = launch {
        scanGalleyWithUri(uri) {
            DataBaseDAOHelper.insertSignedMedia(it)
            mediaLive.postValue(GALLEY_UPDATED)
        }
        makeToast("相册更新完毕")
        cancel()
    }

    private fun updateGalley() = DataBaseDAOHelper.run {
        fun sortMedia(allSignedMedia: MutableList<GalleyMedia>, galleyMedia: GalleyMedia) {
            allSignedMedia.stream().filter { it.uri == galleyMedia.uri }
                .collect(Collectors.toList()).let { list ->
                    if (list.size > 0) allSignedMedia.remove(list[0])
                }
        }
        launch {
            val allSignedMedia = getAllSignedMedia() as MutableList
            val scanPicture = async {
                flow {
                    scanPicture { _, galleyMedia ->
                        emit(galleyMedia)
                    }
                }.buffer().collect {
                    sortSignedMedia(it)
                    synchronized(allSignedMedia) {
                        sortMedia(allSignedMedia, it)
                    }
                }
            }
            val scanVideo = async {
                flow {
                    scanVideo { _, galleyMedia ->
                        emit(galleyMedia)
                    }
                }.buffer().collect {
                    sortSignedMedia(it)
                    synchronized(allSignedMedia) {
                        sortMedia(allSignedMedia, it)
                    }
                }
            }
            scanPicture.await()
            scanVideo.await()
            deleteSignedMedias(allSignedMedia)
            mediaLive.postValue(GALLEY_UPDATED)
            makeToast("相册更新完毕")
            cancel()
        }
    }

    private inner class WorkBinder : Binder(),IWorkService {
        override fun updateMusicBucket() {
            this@WorkService.updateMusicBucket()
        }

        override fun updateMusic(data: Uri?) {
            this@WorkService.updateMusic()
        }

        override fun updateGalley(data: Uri?) {
            this@WorkService.updateGalley()
        }
    }

    private fun makeToast(msg: String) {
        this.onUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}