package com.protone.seenn.service

import android.content.BroadcastReceiver
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.protone.api.ActiveTimer
import com.protone.api.TAG
import com.protone.api.baseType.toast
import com.protone.api.context.workIntentFilter
import com.protone.api.entity.GalleyMedia
import com.protone.api.entity.Music
import com.protone.seenn.broadcast.MediaContentObserver
import com.protone.seenn.broadcast.WorkReceiver
import com.protone.seenn.broadcast.workLocalBroadCast
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.media.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class WorkService : LifecycleService(), CoroutineScope by CoroutineScope(Dispatchers.IO) {

    companion object {
        private const val UPDATE_MUSIC = 1
        private const val UPDATE_GALLEY = 2
    }

    private val activeTimer = ActiveTimer().apply {
        addFunction(UPDATE_MUSIC) { this@WorkService.updateMusic() }
        addFunction(UPDATE_GALLEY) { this@WorkService.updateGalley() }
    }

    private val workReceiver: BroadcastReceiver = object : WorkReceiver() {

        override fun updateMusic(data: Uri?) {
            if (data != null) {
                this@WorkService.updateMusic(data)
            } else {
                activeTimer.active(UPDATE_MUSIC)
            }
        }

        override fun updateGalley(data: Uri?) {
            if (data != null) {
                this@WorkService.updateGalley(data)
            } else {
                activeTimer.active(UPDATE_GALLEY)
            }
        }
    }

    private val mediaContentObserver = MediaContentObserver(Handler(Looper.getMainLooper()))

    override fun onCreate() {
        super.onCreate()
        registerBroadcast()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        activeTimer.destroy()
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
        workLocalBroadCast.registerReceiver(workReceiver, workIntentFilter)
    }

    private fun updateMusicBucket() {
        makeToast("歌单更新完毕")
    }

    private fun updateMusic(uri: Uri) {
        launch(Dispatchers.IO) {
            scanAudioWithUri(uri) {
                DatabaseHelper.instance.musicDAOBridge.insertMusicCheck(it)
            }
            updateMusicBucket()
        }
        makeToast("音乐更新完毕")
    }

    private fun updateMusic() {
        fun sortMusic(allMusic: MutableList<Music>, music: Music): Boolean {
            val index = allMusic.indexOf(music)
            return if (index != -1) {
                allMusic.removeAt(index)
                true
            } else false
        }

        DatabaseHelper.instance.musicDAOBridge.run {
            val allMusic = mutableListOf<Music>()
            launch(Dispatchers.IO) {
                getAllMusic()?.let { allMusic.addAll(it) }
                flow {
                    scanAudio { _, music ->
                        if (!sortMusic(allMusic, music)) {
                            emit(music)
                        }
                    }
                }.buffer().collect {
                    insertMusic(it)
                }
                deleteMusicMulti(allMusic)
                updateMusicBucket()
                if (allMusic.size != 0) {
                    makeToast("音乐更新完毕")
                }
            }
        }
    }

    private fun updateGalley(uri: Uri) = launch(Dispatchers.IO) {
        scanGalleyWithUri(uri) {
            val checkedMedia =
                DatabaseHelper
                    .instance
                    .signedGalleyDAOBridge
                    .insertSignedMediaChecked(it)
            if (checkedMedia != null) {
                Log.d(TAG, "updateGalley(uri: Uri): 相册更新完毕")
                makeToast("相册更新完毕")
            }
        }
    }

    private fun updateGalley() = DatabaseHelper.instance.signedGalleyDAOBridge.run {
        launch(Dispatchers.IO) {
            val sortMedias = async(Dispatchers.Default) {
                val allSignedMedia = getAllSignedMedia() as MutableList
                flow {
                    allSignedMedia.forEach {
                        if (!isUriExist(it.uri)) {
                            emit(it)
                        }
                    }
                }.buffer().collect {
                    deleteSignedMedia(it)
                }
            }

            val allSignedMedia = getAllSignedMedia()

            suspend fun sortMedia(
                dao: DatabaseHelper.GalleyDAOBridge,
                allSignedMedia: List<GalleyMedia>?,
                it: GalleyMedia
            ) {
                if (allSignedMedia != null && allSignedMedia.isNotEmpty()) {
                    allSignedMedia.indexOf(it).let { index ->
                        if (index != -1) {
                            if (allSignedMedia[index] == it) return@let
                            dao.updateSignedMedia(it)
                        } else {
                            dao.insertSignedMedia(it)
                        }
                    }
                } else {
                    dao.insertSignedMedia(it)
                }
            }

            val scanPicture = async(Dispatchers.Default) {
                flow {
                    scanPicture { _, galleyMedia ->
                        emit(galleyMedia)
                    }
                }.buffer().collect {
                    sortMedia(this@run, allSignedMedia, it)
                }
            }

            val scanVideo = async(Dispatchers.Default) {
                flow {
                    scanVideo { _, galleyMedia ->
                        emit(galleyMedia)
                    }
                }.buffer().collect {
                    sortMedia(this@run, allSignedMedia, it)
                }
            }

            sortMedias.await()
            scanPicture.await()
            scanVideo.await()
        }
    }

    private fun makeToast(msg: String) {
        msg.toast()
    }
}