package com.protone.mediamodle

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.protone.api.context.Global
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Music
import com.protone.database.room.entity.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.*

object Medias {

    const val GALLEY_UPDATED = 0x01
    const val AUDIO_UPDATED = 0X02

    val mediaLive = MutableLiveData<Int>()

    var music: MutableList<Music> = mutableListOf()
        set(value) {
            musicBucket[Global.application.getString(R.string.all_music)] = value
            field = value
        }
        get() = musicBucket[Global.application.getString(R.string.all_music)] ?: mutableListOf()

    val musicBucketLive = MutableLiveData<Int>()

    val musicBucket = mutableMapOf<String, MutableList<Music>>()

    fun photoInToday(): GalleyMedia? {
        val ca = Calendar.getInstance(Locale.CHINA)
        val now = Calendar.getInstance(Locale.CHINA).apply {
            timeInMillis = System.currentTimeMillis()
        }
        val allPhoto = runBlocking(Dispatchers.IO) {
            DataBaseDAOHelper.getAllMediaByType(false)
        }
        allPhoto?.forEach {
            ca.timeInMillis = it.date * 1000
            if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                && ca.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
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
            DataBaseDAOHelper.getAllMediaByType(true)
        }
        allVideo?.forEach {
            ca.timeInMillis = it.date * 1000
            if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                && ca.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
            ) {
                return it
            }
        }
        return allVideo?.let { if (it.isEmpty()) null else it[(it.indices).random()] }
    }

    fun musicInToday(): Music {
        val ca = Calendar.getInstance(Locale.getDefault())
        val now = Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = System.currentTimeMillis()
        }
        music.forEach {
            ca.timeInMillis = it.year * 1000
            if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                && ca.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
            ) {
                return it
            }
        }
        return music.let { it[(it.indices).random()] }
    }

    fun noteInToday(): Note? {
        val ca = Calendar.getInstance(Locale.getDefault())
        val now = Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = System.currentTimeMillis()
        }
        val allNote = runBlocking(Dispatchers.IO) {
            DataBaseDAOHelper.getAllNote()
        }
        allNote?.forEach {
            ca.timeInMillis = it.time * 1000
            if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                && ca.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
            ) {
                return it
            }
        }
        return allNote?.let { if (it.isEmpty()) null else it[(it.indices).random()] }
    }

    val musicState by lazy { MutableLiveData<MusicState>() }
}

data class MusicState(
    val name: String,
    val duration: Long,
    val now_duration: Long,
    val albumUri: Uri,
    val isPlaying: Boolean = false
)