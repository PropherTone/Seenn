package com.protone.api.entity

import android.net.Uri
import androidx.room.*
import com.protone.api.converters.ListTypeConverter
import com.protone.api.converters.UriTypeConverter


@Entity(indices = [Index(value = ["mediaId"], unique = true)])
@TypeConverters(UriTypeConverter::class, ListTypeConverter::class)
data class GalleyMedia(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "mediaId")
    val mediaId: Long?,
    var name: String,
    var path : String?,
    var bucket: String,
    var size: Long,
    var type: List<String>?,
    var cate: List<String>?,
    val uri: Uri,
    var date: Long,
    val thumbnailUri: Uri?,
    val duration: Long,
    val isVideo: Boolean
) {

    override fun toString(): String {
        return "GalleyMedia(id=$mediaId, name='$name', bucket='$bucket', size=$size, type='$type', cate='$cate', uri=$uri, date=$date, thumbnailUri=$thumbnailUri, duration=$duration, isVideo=$isVideo)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleyMedia

        if (name != other.name) return false
        if (bucket != other.bucket) return false
        if (size != other.size) return false
        if (uri != other.uri) return false
        if (date != other.date) return false
        if (thumbnailUri != other.thumbnailUri) return false
        if (duration != other.duration) return false
        if (isVideo != other.isVideo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + bucket.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + (thumbnailUri?.hashCode() ?: 0)
        result = 31 * result + duration.hashCode()
        result = 31 * result + isVideo.hashCode()
        return result
    }


}