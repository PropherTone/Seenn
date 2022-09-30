package com.protone.worker

import com.protone.api.entity.GalleryMedia
import com.protone.api.entity.Note
import com.protone.worker.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.*

fun photoInToday(): GalleryMedia? {
    val ca = Calendar.getInstance(Locale.CHINA)
    val now = Calendar.getInstance(Locale.CHINA).apply {
        timeInMillis = System.currentTimeMillis()
    }
    val allPhoto = runBlocking(Dispatchers.IO) {
        DatabaseHelper.instance.signedGalleryDAOBridge.getAllMediaByType(false)
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

fun videoInToday(): GalleryMedia? {
    val ca = Calendar.getInstance(Locale.CHINA)
    val now = Calendar.getInstance(Locale.CHINA).apply {
        timeInMillis = System.currentTimeMillis()
    }
    val allVideo = runBlocking(Dispatchers.IO) {
        DatabaseHelper.instance.signedGalleryDAOBridge.getAllMediaByType(true)
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