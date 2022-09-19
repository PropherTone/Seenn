package com.protone.worker.viewModel

import androidx.lifecycle.viewModelScope
import com.protone.api.entity.Music
import com.protone.worker.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickMusicViewModel : BaseViewModel() {

    companion object {
        const val BUCKET_NAME = "BUCKET"
        const val MODE = "MODE"

        const val ADD_BUCKET = "ADD"
        const val PICK_MUSIC = "PICK"
        const val SEARCH_MUSIC = "SEARCH"
    }

    val data: MutableList<Music> = mutableListOf()

    suspend fun getMusics() =
        DatabaseHelper.instance.musicDAOBridge.getAllMusic() as MutableList<Music>

    suspend fun filterData(input: String) = data.asFlow().filter {
        it.displayName?.contains(input, true) == true || it.album?.contains(
            input,
            true
        ) == true
    }.buffer().toList() as MutableList<Music>

    suspend fun getMusicWithMusicBucket(bucket: String): Collection<Music> =
        withContext(Dispatchers.IO) {
            DatabaseHelper.instance.run {
                val musicBucket = musicBucketDAOBridge.getMusicBucketByName(bucket)
                if (musicBucket != null) {
                    musicWithMusicBucketDAOBridge.getMusicWithMusicBucket(musicBucket.musicBucketId)
                } else listOf()
            }
        }

    fun deleteMusicWithMusicBucket(musicBaseId: Long, musicBucket: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseHelper.instance.run {
                    musicBucketDAOBridge.getMusicBucketByName(musicBucket)?.let { musicBucket ->
                    musicWithMusicBucketDAOBridge
                        .deleteMusicWithMusicBucketAsync(musicBaseId, musicBucket.musicBucketId)
                }
            }
        }
    }

    suspend fun insertMusicWithMusicBucket(musicBaseId: Long, bucket: String): Long =
        withContext(Dispatchers.IO) {
            DatabaseHelper
                .instance
                .musicWithMusicBucketDAOBridge
                .insertMusicWithMusicBucket(musicBaseId, bucket)
        }

}