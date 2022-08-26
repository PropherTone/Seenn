package com.protone.worker.viewModel

import androidx.lifecycle.viewModelScope
import com.protone.api.entity.Music
import com.protone.api.entity.MusicBucket
import com.protone.worker.Medias
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.MediaAction
import com.protone.worker.database.userConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.streams.toList

class MusicModel : BaseViewModel() {

    sealed class MusicEvent {
        data class PlayMusic(val music: Music) : ViewEvent
        data class SetBucketCover(val name: String) : ViewEvent
        data class AddMusic(val bucket: String) : ViewEvent
        data class Edit(val bucket: String) : ViewEvent
        data class Delete(val bucket: String) : ViewEvent
        data class RefreshBucket(val newName: String) : ViewEvent
        data class AddBucket(val bucket: String) : ViewEvent
        data class DeleteBucket(val bucket: String) : ViewEvent
        object AddMusicBucket : ViewEvent
    }

    var lastBucket: String = userConfig.lastMusicBucket
        set(value) {
            userConfig.lastMusicBucket = value
            field = value
        }
        get() = userConfig.lastMusicBucket

    var onMusicDataEvent: OnMusicDataEvent? = null

    init {
        DatabaseHelper.instance.apply {
            musicBucketDAOBridge.startEvent()
            musicDAOBridge.startEvent()
            musicWithMusicBucketDAOBridge.startEvent()
        }
        viewModelScope.launch(Dispatchers.Default) {
            DatabaseHelper.instance.mediaNotifier.buffer().collect {
                when (it) {
                    is MediaAction.OnNewMusicBucket ->
                        onMusicDataEvent?.onNewMusicBucket(it.musicBucket)
                    is MediaAction.OnMusicBucketUpdated ->
                        onMusicDataEvent?.onMusicBucketUpdated(it.musicBucket)
                    is MediaAction.OnMusicBucketDeleted ->
                        onMusicDataEvent?.onMusicBucketDeleted(it.musicBucket)
                    else -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        DatabaseHelper.instance.apply {
            musicBucketDAOBridge.stopEvent()
            musicDAOBridge.stopEvent()
            musicWithMusicBucketDAOBridge.stopEvent()
        }
        super.onCleared()
    }

    fun getCurrentMusicList(bucket: String): MutableList<Music> =
        Medias.musicBucket[bucket] ?: mutableListOf()

    suspend fun getBucketRefreshed(name: String) = withContext(Dispatchers.IO) {
        getBucket(name)?.let {
            it.size =
                DatabaseHelper.instance.musicWithMusicBucketDAOBridge.getMusicWithMusicBucket(it.musicBucketId).size
            it
        }
    }

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

    interface OnMusicDataEvent {
        fun onNewMusicBucket(musicBucket: MusicBucket)
        fun onMusicBucketUpdated(musicBucket: MusicBucket)
        fun onMusicBucketDeleted(musicBucket: MusicBucket)
    }
}