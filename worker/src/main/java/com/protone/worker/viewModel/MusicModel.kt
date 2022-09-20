package com.protone.worker.viewModel

import androidx.lifecycle.viewModelScope
import com.protone.api.entity.Music
import com.protone.api.entity.MusicBucket
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.database.MediaAction
import com.protone.worker.database.userConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MusicModel : BaseViewModel() {

    sealed class MusicEvent : ViewEvent {
        data class PlayMusic(val music: Music) : MusicEvent()
        data class SetBucketCover(val name: String) : MusicEvent()
        data class AddMusic(val bucket: String) : MusicEvent()
        data class Edit(val bucket: String) : MusicEvent()
        data class Delete(val bucket: String) : MusicEvent()
        data class RefreshBucket(val newName: String) : MusicEvent()
        data class AddBucket(val bucket: String) : MusicEvent()
        data class DeleteBucket(val bucket: String) : MusicEvent()
        object AddMusicBucket : MusicEvent()
        object Locate : MusicEvent()
        object Search : MusicEvent()
    }

    var lastBucket: String = userConfig.lastMusicBucket
        set(value) {
            userConfig.lastMusicBucket = value
            field = value
        }
        get() = userConfig.lastMusicBucket

    var onMusicDataEvent: OnMusicDataEvent? = null

    init {
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

    suspend fun getCurrentMusicList(bucket: MusicBucket): MutableList<Music> =
        withContext(Dispatchers.IO) {
            DatabaseHelper.instance.musicWithMusicBucketDAOBridge.getMusicWithMusicBucket(bucket.musicBucketId) as MutableList<Music>
        }

    suspend fun getBucketRefreshed(name: String) = withContext(Dispatchers.IO) {
        getBucket(name)?.let {
            it.size =
                DatabaseHelper.instance.musicWithMusicBucketDAOBridge.getMusicWithMusicBucket(it.musicBucketId).size
            it
        }
    }

    suspend fun getBucket(name: String) =
        DatabaseHelper.instance.musicBucketDAOBridge.getMusicBucketByName(name)

    suspend fun getLastMusicBucket(list: MutableList<MusicBucket>): MusicBucket =
        withContext(Dispatchers.Default) {
            list.find { it.name == lastBucket } ?: MusicBucket()
        }

    suspend fun getMusicBuckets(): MutableList<MusicBucket> = withContext(Dispatchers.IO) {
        DatabaseHelper.instance.run {
            ((musicBucketDAOBridge.getAllMusicBucket() as MutableList<MusicBucket>?)
                ?: mutableListOf()).let { list ->
                list.forEach {
                    it.size = musicWithMusicBucketDAOBridge
                        .getMusicWithMusicBucket(it.musicBucketId).size
                }
                list
            }
        }
    }

    suspend fun tryDeleteMusicBucket(name: String): MusicBucket? {
        return DatabaseHelper.instance.musicBucketDAOBridge.let {
            val musicBucketByName = it.getMusicBucketByName(name)
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

    suspend fun doSearch(playList: List<Music>, text: String) = withContext(Dispatchers.Default) {
        playList.filter {
            it.displayName?.contains(text, true) == true
                    || it.album?.contains(text, true) == true
                    || it.title.contains(text, true)
                    || it.artist?.contains(text, true) == true
        }
    }

    interface OnMusicDataEvent {
        suspend fun onNewMusicBucket(musicBucket: MusicBucket)
        suspend fun onMusicBucketUpdated(musicBucket: MusicBucket)
        suspend fun onMusicBucketDeleted(musicBucket: MusicBucket)
    }
}