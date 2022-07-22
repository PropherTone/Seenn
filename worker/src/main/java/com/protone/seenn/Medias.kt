package com.protone.seenn

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.protone.api.baseType.getString
import com.protone.api.entity.GalleyMedia
import com.protone.api.entity.Music
import com.protone.api.entity.Note
import com.protone.seenn.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import java.util.*

object Medias {

    val galleyNotifier = MutableSharedFlow<MutableList<GalleyMedia>>()
    val audioNotifier = MutableSharedFlow<MutableList<Music>>()
    val musicBucketNotifier = MutableLiveData<Int>()

    var music: MutableList<Music>
        set(value) {
            musicBucket[R.string.all_music.getString()] = value
        }
        get() = musicBucket[R.string.all_music.getString()] ?: mutableListOf()

    val musicBucket = mutableMapOf<String, MutableList<Music>>()

    fun photoInToday(): GalleyMedia? {
        val ca = Calendar.getInstance(Locale.CHINA)
        val now = Calendar.getInstance(Locale.CHINA).apply {
            timeInMillis = System.currentTimeMillis()
        }
        val allPhoto = runBlocking(Dispatchers.IO) {
            DatabaseHelper.instance.signedGalleyDAOBridge.getAllMediaByType(false)
        }
        allPhoto?.forEach {
            ca.timeInMillis = it.date * 1000
            if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                && ca.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
                && ca.get(Calendar.YEAR) != now.get(Calendar.YEAR)
            ) {
                return it
            }
        }
        return allPhoto?.let { if (it.isEmpty()) null else it[(it.indices).random()] }
    }

    fun videoInToday(): GalleyMedia? {
        val ca = Calendar.getInstance(Locale.CHINA)
        val now = Calendar.getInstance(Locale.CHINA).apply {
            timeInMillis = System.currentTimeMillis()
        }
        val allVideo = runBlocking(Dispatchers.IO) {
            DatabaseHelper.instance.signedGalleyDAOBridge.getAllMediaByType(true)
        }
        allVideo?.forEach {
            ca.timeInMillis = it.date * 1000
            if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                && ca.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
                && ca.get(Calendar.YEAR) != now.get(Calendar.YEAR)
            ) {
                return it
            }
        }
        return allVideo?.let { if (it.isEmpty()) null else it[(it.indices).random()] }
    }

    fun randomNote(): Note? {
        val allNote = runBlocking(Dispatchers.IO) {
            DatabaseHelper.instance.noteDAOBridge.getAllNote()
        }
        return allNote?.let { if (it.isEmpty()) null else it[(it.indices).random()] }
    }
}

data class MusicState(
    val name: String,
    val duration: Long,
    val now_duration: Long,
    val albumUri: Uri,
    val isPlaying: Boolean = false
)