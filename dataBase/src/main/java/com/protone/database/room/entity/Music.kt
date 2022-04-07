package com.protone.database.room.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.protone.database.room.converters.ListTypeConverter
import com.protone.database.room.converters.UriTypeConverter

@Entity
@TypeConverters(UriTypeConverter::class, ListTypeConverter::class)
data class Music(
    val musicId: Long,
    val title: String,
    val size: Long,
    val album: String,
    val albumID: Uri,
    val artist: String,
    val mimeType: String,
    val bucketDisplayName: String,
    val displayName: String?,
    val duration: Long,
    val year: Long,
    var uri: Uri,
    var myBucket: List<String>?
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
