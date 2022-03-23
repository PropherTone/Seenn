package com.protone.database.room.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

data class VideoMedia(
    var id: Long?,
    val name: String,
    val bucket: String,
    var size: Long,
    var type: String,
    var cate: String,
    var uri: Uri,
    var date: Long,
    var thumbnailUri: Uri
) {
    constructor() : this(null, "", "", 0, "", "", Uri.parse(""),0, Uri.parse(""))
}