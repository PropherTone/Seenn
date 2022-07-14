package com.protone.database.room.entity

import androidx.room.*
import com.protone.database.room.converters.UriTypeConverter

@Entity
@TypeConverters(UriTypeConverter::class)
data class MusicBucket(
    var name: String,
    var icon: String?,
    var size: Int,
    var detail: String?,
    var date: String?
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "musicBucketId")
    var musicBucketId: Long = 0

    @Ignore
    constructor() : this("", null, 0, null, null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MusicBucket

        if (name != other.name) return false
        if (size != other.size) return false
        if (detail != other.detail) return false
        if (date != other.date) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + size
        result = 31 * result + (detail?.hashCode() ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "MusicBucket(name='$name', icon=$icon, size=$size, detail=$detail, date=$date, id=$musicBucketId)"
    }


}