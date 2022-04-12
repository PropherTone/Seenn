package com.protone.mediamodle

import android.util.Log
import com.protone.api.context.Global
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.mediamodle.media.scanAudio
import com.protone.mediamodle.media.scanPicture
import com.protone.mediamodle.media.scanVideo
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.concurrent.Executors

object GalleyHelper : CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val threadPool = Executors.newCachedThreadPool()

    private val updatePicture = Runnable {
        Galley.photo = scanPicture()
        sortSignedMedia(false)
    }

    private val updateVideo = Runnable {
        Galley.video = scanVideo()
        sortSignedMedia(true)
    }

    private val updateMusic = Runnable {
        Galley.music = scanAudio()
    }

    suspend fun updatePictureCO() = withContext(Dispatchers.IO) {
        Galley.photo = scanPicture()
        sortSignedMedia(false)
        cancel()
    }

    suspend fun updateVideoCO() = withContext(Dispatchers.IO) {
        Galley.video = scanVideo()
        sortSignedMedia(true)
        cancel()
    }

    suspend fun updateMusicCO() = withContext(Dispatchers.IO) {
        Galley.musicBucket[Global.application.getString(R.string.all_music)] = scanAudio()
        cancel()
    }

    fun updateAll(callBack: () -> Unit) = threadPool.execute {
        val job1 = launch { updatePictureCO() }
        val job2 = launch { updateVideoCO() }
        val job3 = launch { updateMusicCO() }

        while (job1.isActive || job2.isActive || job3.isActive) continue
        callBack()
    }

    fun updatePicture() = threadPool.execute(updatePicture)

    fun updateVideo() = threadPool.execute(updateVideo)

    fun updateMusic() = threadPool.execute(updateMusic)

    private fun sortSignedMedia(isVideo: Boolean) {
        DataBaseDAOHelper.getAllSignedMedia()?.let { signed ->
            val map = HashMap<String, GalleyMedia?>()
            signed.forEach { gm ->
                if (if (isVideo) gm.isVideo else !gm.isVideo) {
                    map[gm.name] = gm
                }
            }
            if (map.isNotEmpty()) {
                val list = arrayListOf<GalleyMedia>()
                (if (isVideo) Galley.allVideo else Galley.allPhoto).forEach { gm ->
                    if (map[gm.name] != null) {
                        map[gm.name]?.let { it -> list.add(it) }
                    }
                }
                list.forEach { gm ->
                    DataBaseDAOHelper.deleteSignedMedia(gm)
                }
            }
        }
    }

}