package com.protone.worker.viewModel

import com.protone.api.entity.Music
import com.protone.api.entity.MusicBucket
import com.protone.worker.Medias
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.userConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.streams.toList

class TempMusicModel : BaseViewModel() {

    sealed class TempMusicEvent {
        data class PlayMusic(val music: Music) : ViewEvent
        data class SetBucketCover(val name: String) : ViewEvent
        data class AddMusic(val bucket: String) : ViewEvent
        data class Edit(val bucket: String) : ViewEvent
        data class Delete(val bucket: String) : ViewEvent
        data class RefreshBucket(val oldName: String, val newName: String) : ViewEvent
        object AddMusicBucket : ViewEvent
    }

    var lastBucket: String = userConfig.lastMusicBucket
        set(value) {
            userConfig.lastMusicBucket = value
            field = value
        }
        get() = userConfig.lastMusicBucket

    fun getCurrentMusicList(bucket: String): MutableList<Music> =
        Medias.musicBucket[bucket] ?: mutableListOf()

    suspend fun getBucket(name: String) =
        DatabaseHelper.instance.musicBucketDAOBridge.getMusicBucketByNameRs(name)

    suspend fun getLastMusicBucket(list: MutableList<MusicBucket>): MusicBucket =
        withContext(Dispatchers.Default) {
            list.stream().filter { it.name == lastBucket }.toList()
                .let { if (it.isNotEmpty()) it[0] else MusicBucket() }
        }

    suspend fun getMusicBuckets(): MutableList<MusicBucket> {
        return DatabaseHelper.instance.musicBucketDAOBridge.getAllMusicBucketRs() as MutableList<MusicBucket>?
            ?: mutableListOf()
    }

    suspend fun tryDeleteMusicBucket(name: String): MusicBucket? {
        return DatabaseHelper.instance.musicBucketDAOBridge.let {
            val musicBucketByName = it.getMusicBucketByNameRs(name)
            if (musicBucketByName != null) {
                if (it.deleteMusicBucketRs(musicBucketByName)) {
                    musicBucketByName.icon?.let { path ->
                        val file = File(path)
                        if (file.isFile && file.exists()) file.delete()
                    }
                }
                musicBucketByName
            } else null
        }
    }
}