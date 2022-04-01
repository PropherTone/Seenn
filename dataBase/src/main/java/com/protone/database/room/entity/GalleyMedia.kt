package com.protone.database.room.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.protone.database.room.converters.UriTypeConverter


@Entity
@TypeConverters(UriTypeConverter::class)
data class GalleyMedia(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    var name: String,
    var bucket: String,
    val size: Long,
    var type: String,
    var cate: String,
    var uri: Uri,
    var date: Long,
    val thumbnailUri: Uri,
    val duration: Long,
    val isVideo: Boolean
) {
    override fun toString(): String {
        return "GalleyMedia(id=$id, name='$name', bucket='$bucket', size=$size, type='$type', cate='$cate', uri=$uri, date=$date, thumbnailUri=$thumbnailUri, duration=$duration, isVideo=$isVideo)"
    }
}