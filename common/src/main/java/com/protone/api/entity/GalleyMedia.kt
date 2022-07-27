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
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "path")
    var path: String?,
    @ColumnInfo(name = "bucket")
    var bucket: String,
    @ColumnInfo(name = "size")
    var size: Long,
    @ColumnInfo(name = "type")
    var type: List<String>?,
    @ColumnInfo(name = "cate")
    var cate: List<String>?,
    @ColumnInfo(name = "uri")
    val uri: Uri,
    @ColumnInfo(name = "date")
    var date: Long,
    @ColumnInfo(name = "thumbnailUri")
    val thumbnailUri: Uri?,
    @ColumnInfo(name = "duration")
    val duration: Long,
    @ColumnInfo(name = "isVideo")
    val isVideo: Boolean,
) {
    @Ignore
    var mediaStatus: MediaStatus = MediaStatus.NewInsert

    enum class MediaStatus{
        Updated,
        Deleted,
        NewInsert
    }

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