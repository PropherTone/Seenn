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
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.workIntentFilter
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.dao.DataBaseDAOHelper.deleteMusicMultiAsync
import com.protone.database.room.dao.DataBaseDAOHelper.getAllMusic
import com.protone.database.room.dao.DataBaseDAOHelper.getAllMusicBucket
import com.protone.database.room.dao.DataBaseDAOHelper.getAllMusicRs
import com.protone.database.room.dao.DataBaseDAOHelper.getMusicBucketByName
import com.protone.database.room.dao.DataBaseDAOHelper.updateMusicBucketAsync
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Music
import com.protone.mediamodle.Medias.audioLive
import com.protone.mediamodle.Medias.galleyLive
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
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
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

    private fun updateMusicBucket() = launch(Dispatchers.IO) {
        val allMusicBucket = getAllMusicBucket().let { l ->
            (l as ArrayList).filter {
                it.name != R.string.all_music.getString()
            }
        }
        allMusicBucket.forEach {
            musicBucket[it.name] =
                DataBaseDAOHelper.getMusicWithMusicBucket(it.musicBucketId) as MutableList<Music>
        }
        getAllMusicRs()?.let {
            music = it as MutableList<Music>
        }
        musicBucket.keys.forEach {
            getMusicBucketByName(it)?.let { mb ->
                musicBucket[it]?.size?.let { size ->
                    mb.size = size
                    updateMusicBucketAsync(mb)
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
            audioLive.postValue(arrayListOf(it))
        }
        updateMusicBucket()
        makeToast("音乐更新完毕")
    }

    private fun updateMusic() {
        fun sortMusic(allMusic: MutableList<Music>, music: Music): Boolean {
            val index = allMusic.indexOf(music)
            if (index != -1) {
                allMusic.removeAt(index)
                return true
            }
            return false
        }
        launch(Dispatchers.IO) {
            val allMusic = getAllMusic() as MutableList
            flow {
                scanAudio { _, music ->
                    if (sortMusic(allMusic, music)) {
                        emit(music)
                    }
                }
            }.buffer().collect {
                DataBaseDAOHelper.insertMusic(it)
            }
            deleteMusicMultiAsync(allMusic)
            if (allMusic.size != 0) {
                updateMusicBucket()
                audioLive.postValue(allMusic as ArrayList<Music>)
                makeToast("音乐更新完毕")
            }
            cancel()
        }
    }

    private fun updateGalley(uri: Uri) = launch(Dispatchers.IO) {
        scanGalleyWithUri(uri) {
            val checkedMedia = DataBaseDAOHelper.insertSignedMediaChecked(it)
            if (checkedMedia != null) {
                galleyLive.postValue(arrayListOf(checkedMedia))
                makeToast("相册更新完毕")
            }
        }
        cancel()
    }

    private fun updateGalley() = DataBaseDAOHelper.run {
        fun sortMedia(allSignedMedia: MutableList<GalleyMedia>, galleyMedia: GalleyMedia) {
            allSignedMedia.stream().filter { it.uri == galleyMedia.uri }
                .collect(Collectors.toList()).let { list ->
                    if (list.size > 0) allSignedMedia.remove(list[0])
                }
        }
        val updatedMedia = arrayListOf<GalleyMedia>()
        launch(Dispatchers.IO) {
            val allSignedMedia = getAllSignedMedia() as MutableList
            val scanPicture = async {
                flow {
                    scanPicture { _, galleyMedia ->
                        emit(galleyMedia)
                    }
                }.buffer().collect {
                    val checkedMedia = insertSignedMediaChecked(it)
                    synchronized(allSignedMedia) {
                        sortMedia(allSignedMedia, it)
                    }
                    if (checkedMedia != null && !updatedMedia.contains(checkedMedia)) {
                        updatedMedia.add(checkedMedia)
                    }
                }
            }
            val scanVideo = async {
                flow {
                    scanVideo { _, galleyMedia ->
                        emit(galleyMedia)
                    }
                }.buffer().collect {
                    val checkedMedia = insertSignedMediaChecked(it)
                    synchronized(allSignedMedia) {
                        sortMedia(allSignedMedia, it)
                    }
                    if (checkedMedia != null && !updatedMedia.contains(checkedMedia)) {
                        updatedMedia.add(checkedMedia)
                    }
                }

            }
            scanPicture.await()
            scanVideo.await()
            updatedMedia.addAll(allSignedMedia)
            deleteSignedMediaMultiAsync(allSignedMedia)
            if (updatedMedia.size != 0) {
                galleyLive.postValue(updatedMedia)
                makeToast("相册更新完毕")
            }
            cancel()
        }
    }

    private inner class WorkBinder : Binder(), IWorkService {
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
        msg.toast()
    }
}