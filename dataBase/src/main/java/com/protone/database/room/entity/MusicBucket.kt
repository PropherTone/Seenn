package com.protone.database.room.entity

import android.net.Uri
import androidx.room.*
import com.protone.database.room.converters.MusicTypeConverter

@Entity
@TypeConverters(MusicTypeConverter::class)
data class MusicBucket(
    var name: String,
    var icon: Uri?,
    var size: Int,
    var detail: String?,
    var date: String?
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @Ignore
    constructor() : this("", null, 0, null, null)
}