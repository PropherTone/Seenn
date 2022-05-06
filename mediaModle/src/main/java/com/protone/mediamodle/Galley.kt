package com.protone.mediamodle

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.protone.api.context.Global
import com.protone.api.context.onBackground
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Music
import java.util.*

object Galley {

    var photo: MutableMap<String, MutableList<GalleyMedia>> = mutableMapOf()
        set(value) {
            val ap = mutableListOf<GalleyMedia>()
            value.forEach { (_, mutableList) ->
                ap.addAll(mutableList)
            }
            value[Global.application.getString(R.string.all_galley)] = ap
            field = value
        }

    var video: MutableMap<String, MutableList<GalleyMedia>> = mutableMapOf()
        set(value) {
            val ap = mutableListOf<GalleyMedia>()
            value.forEach { (_, mutableList) ->
                ap.addAll(mutableList)
            }
            value[Global.application.getString(R.string.all_galley)] = ap
            field = value
        }

    var music: MutableList<Music> = mutableListOf()
        set(value) {
            musicBucket[Global.application.getString(R.string.all_music)] = value
            field = value
        }
        get() = musicBucket[Global.application.getString(R.string.all_music)] ?: mutableListOf()

    val musicBucketLive = MutableLiveData<Set<String>>()

    val musicBucket = mutableMapOf<String, MutableList<Music>>()

    val allPhoto: MutableList<GalleyMedia>?
        get() = photo[Global.application.getString(R.string.all_galley)]

    val allVideo: MutableList<GalleyMedia>?
        get() = video[Global.application.getString(R.string.all_galley)]

    fun photoInToday(): GalleyMedia? {
        val ca = Calendar.getInstance(Locale.CHINA)
        val now = Calendar.getInstance(Locale.CHINA).apply {
            timeInMillis = System.currentTimeMillis()
        }
        allPhoto?.forEach {
            ca.timeInMillis = it.date * 1000
            if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                && ca.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
            ) {
                return it
            }
        }
        return allPhoto?.let { it[(0 until it.size).random()] }
    }

    fun videoInToday(): GalleyMedia? {
        val ca = Calendar.getInstance(Locale.CHINA)
        val now = Calendar.getInstance(Locale.CHINA).apply {
            timeInMillis = System.currentTimeMillis()
        }
        allVideo?.forEach {
            ca.timeInMillis = it.date * 1000
            if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                && ca.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
            ) {
                return it
            }
        }
        return null
    }

    fun musicInToday(): Music? {
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
        return null
    }

    val musicState by lazy { MutableLiveData<MusicState>() }

    fun deleteMedia(isVideo: Boolean, media: GalleyMedia) {
        onBackground {
            if (isVideo) video else photo.forEach { (_, list) ->
                for (i in 0 until list.size) {
                    if (list[i].name == media.name) {
                        list.removeAt(i)
                        return@onBackground
                    }
                }
            }
        }
    }
}

data class MusicState(
    val name: String,
    val duration: Long,
    val now_duration: Long,
    val albumUri: Uri,
    val isPlaying: Boolean = false
)