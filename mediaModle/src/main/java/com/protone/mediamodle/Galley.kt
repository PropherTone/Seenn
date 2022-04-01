package com.protone.mediamodle

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.protone.api.context.onBackground
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Music
import java.util.*
import kotlin.collections.ArrayList

object Galley {

    val photoLive = MutableLiveData<MutableMap<String, MutableList<GalleyMedia>>>()

    var photo: MutableMap<String, MutableList<GalleyMedia>> = mutableMapOf()
        set(value) {
            val ap = mutableListOf<GalleyMedia>()
            value.forEach { (_, mutableList) ->
                ap.addAll(mutableList)
            }
            value["ALL"] = ap
            field = value
            photoLive.postValue(field)
        }

    val videoLive = MutableLiveData<MutableMap<String, MutableList<GalleyMedia>>>()

    var video: MutableMap<String, MutableList<GalleyMedia>> = mutableMapOf()
        set(value) {
            val ap = mutableListOf<GalleyMedia>()
            value.forEach { (_, mutableList) ->
                ap.addAll(mutableList)
            }
            value["ALL"] = ap
            field = value
            videoLive.postValue(field)
        }

    val musicLive = MutableLiveData<MutableList<Music>>()

    var music: MutableList<Music> = mutableListOf()
        set(value) {
            field = value
            musicLive.postValue(field)
        }

    val musicBucket = mutableMapOf<String, MutableList<Music>>()

    val allPhoto: MutableList<GalleyMedia> by lazy {
        val ap = mutableListOf<GalleyMedia>()
        photo.forEach { (_, mutableList) ->
            ap.addAll(mutableList)
        }
        ap
    }

    val allVideo: MutableList<GalleyMedia> by lazy {
        val ap = mutableListOf<GalleyMedia>()
        video.forEach { (_, mutableList) ->
            ap.addAll(mutableList)
        }
        ap
    }

    fun photoInToday(): GalleyMedia? {
        val ca = Calendar.getInstance(Locale.CHINA)
        val now = Calendar.getInstance(Locale.CHINA).apply {
            timeInMillis = System.currentTimeMillis()
        }
        allPhoto.forEach {
            ca.timeInMillis = it.date * 1000
            return if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH) && ca.get(Calendar.DAY_OF_MONTH) == now.get(
                    Calendar.DAY_OF_MONTH
                )
            ) {
                it
            } else null
        }
        return null
    }

    fun videoInToday(): GalleyMedia? {
        val ca = Calendar.getInstance(Locale.CHINA)
        val now = Calendar.getInstance(Locale.CHINA).apply {
            timeInMillis = System.currentTimeMillis()
        }
        allVideo.forEach {
            ca.timeInMillis = it.date * 1000
            return if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH) && ca.get(Calendar.DAY_OF_MONTH) == now.get(
                    Calendar.DAY_OF_MONTH
                )
            ) {
                it
            } else null
        }
        return null
    }

    fun musicInToday(): Music? {
        val ca = Calendar.getInstance(Locale.CHINA)
        val now = Calendar.getInstance(Locale.CHINA).apply {
            timeInMillis = System.currentTimeMillis()
        }
        music.forEach {
            ca.timeInMillis = it.year * 1000
            return if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH) && ca.get(Calendar.DAY_OF_MONTH) == now.get(
                    Calendar.DAY_OF_MONTH
                )
            ) {
                it
            } else null
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