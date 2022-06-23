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
    val album: String?,
    val albumID: Uri?,
    val artist: String?,
    val mimeType: String,
    val bucketDisplayName: String?,
    val displayName: String?,
    val duration: Long,
    val year: Long,
    val uri: Uri,
    var myBucket: List<String>
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Music

        if (title != other.title) return false
        if (size != other.size) return false
        if (album != other.album) return false
        if (artist != other.artist) return false
        if (mimeType != other.mimeType) return false
        if (bucketDisplayName != other.bucketDisplayName) return false
        if (duration != other.duration) return false
        if (year != other.year) return false

        return true
    }

    override fun hashCode(): Int {
        var result = musicId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bucketDisplayName.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + year.hashCode()
        return result
    }

}
