@file:Suppress("DEPRECATION")

package com.protone.mediamodle.media

import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.Nullable
import com.protone.api.context.Global
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Music

fun scanPicture(): MutableMap<String, MutableList<GalleyMedia>> {
    val externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val queryArray = arrayOf(
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Thumbnails._ID,
        MediaStore.Images.Media.DATA
    )
    val galley = mutableMapOf<String, MutableList<GalleyMedia>>()
    scan(
        externalContentUri, queryArray,
    ) {
        val id = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val bucket =
            it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val data = it.getColumnIndex(MediaStore.Images.Media.DATA)
        val size = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val name = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val date = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val tn = it.getColumnIndexOrThrow(MediaStore.Images.Thumbnails._ID)
        while (it.moveToNext()) {
            val picID = it.getLong(id)
            val imageName = it.getString(name)
            val path: String? = if (Build.VERSION.SDK_INT < 31) it.getString(data) else null
            val bucketName = it.getString(bucket)
            val imageSize = it.getLong(size)
            val uri = Uri.withAppendedPath(externalContentUri, "$picID")
            val dateTime = it.getLong(date)
            val thumbnailUri =
                Uri.withAppendedPath(externalContentUri, "${it.getLong(tn)}")
            if (!galley.containsKey(bucketName)) {
                galley[bucketName] = arrayListOf()
            }
            galley[bucketName]?.add(
                GalleyMedia(
                    null,
                    imageName,
                    path,
                    bucketName,
                    imageSize,
                    null,
                    null,
                    uri,
                    dateTime,
                    thumbnailUri, 0, false,
                    null
                )
            )
        }
    }
    return galley
}

fun scanVideo(): MutableMap<String, MutableList<GalleyMedia>> {
    val externalContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val query = arrayOf(
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Thumbnails._ID,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.DATA
    )
    val galley = mutableMapOf<String, MutableList<GalleyMedia>>()
    scan(
        externalContentUri, query,
    ) {
        val id = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val bucket =
            it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val data = it.getColumnIndex(MediaStore.Images.Media.DATA)
        val size = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val name = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val date = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val tn = it.getColumnIndexOrThrow(MediaStore.Video.Thumbnails._ID)
        val du = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        while (it.moveToNext()) {
            val picID = it.getLong(id)
            val imageName = it.getString(name)
            val path: String? = if (Build.VERSION.SDK_INT < 31) it.getString(data) else null
            val bucketName = it.getString(bucket)
            val imageSize = it.getLong(size)
            val uri = Uri.withAppendedPath(externalContentUri, "$picID")
            val dateTime = it.getLong(date)
            val thumbnailUri =
                Uri.withAppendedPath(externalContentUri, "${it.getLong(tn)}")
            val duration = it.getLong(du)
            if (!galley.containsKey(bucketName)) {
                galley[bucketName] = arrayListOf()
            }
            galley[bucketName]?.add(
                GalleyMedia(
                    null,
                    imageName,
                    path,
                    bucketName,
                    imageSize,
                    null,
                    null,
                    uri,
                    dateTime,
                    thumbnailUri, duration,
                    true,
                    null
                )
            )
        }
    }
    return galley
}

fun scanAudio(): MutableList<Music> {
    val externalContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val bucketOrData =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
        else MediaStore.MediaColumns.DATA
    val query = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.MIME_TYPE,
        bucketOrData,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATE_ADDED
    )
    val audios = mutableListOf<Music>()
    scan(
        externalContentUri, query,
    ) {
        val id = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val title = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val size = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val album = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumId = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val artist = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val mimeType = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val bucketName =
            it.getColumnIndexOrThrow(bucketOrData)
        val name = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val duration = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val year = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        while (it.moveToNext()) {
            val audioID = it.getLong(id)
            val audioTitle = it.getString(title)
            val audioSize = it.getLong(size)
            val audioAlbum = it.getString(album)
            val audioAlbumID =
                Uri.withAppendedPath(externalContentUri, it.getString(albumId))
            val audioArtist = it.getString(artist)
            val audioMimeType = it.getString(mimeType)
            val audioBucketName = it.getString(bucketName)
            val audioName = it.getString(name)
            val audioDuration = it.getLong(duration)
            val audioYear = it.getLong(year)
            val uri = Uri.withAppendedPath(externalContentUri, "$audioID")
            audios.add(
                Music(
                    audioID,
                    audioTitle,
                    audioSize,
                    audioAlbum,
                    audioAlbumID,
                    audioArtist,
                    audioMimeType,
                    audioBucketName,
                    audioName,
                    audioDuration,
                    audioYear,
                    uri,
                    arrayListOf()
                )
            )
        }
    }
    return audios
}

private inline fun scan(
    @Nullable uri: Uri,
    @Nullable projection: Array<String>,
    block: (Cursor) -> Unit
) {
    Global.application.contentResolver.query(
        uri,
        projection,
        MediaStore.MediaColumns.SIZE + ">0",
        null,
        MediaStore.MediaColumns.DATE_ADDED + " DESC"
    )?.let {
        block(it)
        it.close()
    }
}