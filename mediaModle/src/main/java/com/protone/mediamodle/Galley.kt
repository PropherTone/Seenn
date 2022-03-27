package com.protone.mediamodle

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.protone.api.TAG
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
            value["ALL"] = ap
            field = value
        }

    var video: MutableMap<String, MutableList<GalleyMedia>> = mutableMapOf()
        set(value) {
            val ap = mutableListOf<GalleyMedia>()
            value.forEach { (_, mutableList) ->
                ap.addAll(mutableList)
            }
            value["ALL"] = ap
            field = value
        }

    var music: MutableList<Music> = mutableListOf()

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

    val photoInToday: GalleyMedia?
        get() {
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

    val videoInToday: GalleyMedia?
        get() {
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

    val musicInToday: Music?
        get() {
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