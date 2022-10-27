package com.protone.worker

import com.protone.api.entity.GalleryMedia
import com.protone.api.entity.Note
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.media.isUriExist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.*

suspend fun photoInToday(): GalleryMedia? =
    getOldDateMedia(DatabaseHelper.instance.signedGalleryDAOBridge.getAllMediaByType(false))

suspend fun videoInToday(): GalleryMedia? =
    getOldDateMedia(DatabaseHelper.instance.signedGalleryDAOBridge.getAllMediaByType(true))

private fun getOldDateMedia(medias: List<GalleryMedia>?): GalleryMedia? {
    val ca = Calendar.getInstance(Locale.CHINA)
    val now = Calendar.getInstance(Locale.CHINA).apply {
        timeInMillis = System.currentTimeMillis()
    }
    medias?.forEach {
        ca.timeInMillis = it.date * 1000
        if (ca.get(Calendar.MONTH) == now.get(Calendar.MONTH)
            && ca.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
            && ca.get(Calendar.YEAR) != now.get(Calendar.YEAR)
        ) {
            if (isUriExist(it.uri)) return it
        }
    }
    return medias?.let {
        if (it.isEmpty()) null else {
            it[(it.indices).random()].let { media ->
                if (isUriExist(media.uri)) media else null
            }
        }
    }
}

fun randomNote(): Note? {
    val allNote = runBlocking(Dispatchers.IO) {
        DatabaseHelper.instance.noteDAOBridge.getAllNote()
    }
    return allNote?.let { if (it.isEmpty()) null else it[(it.indices).random()] }
}