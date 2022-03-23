package com.protone.database.room.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


data class GalleyMedia(
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
    val isVideo : Boolean
)