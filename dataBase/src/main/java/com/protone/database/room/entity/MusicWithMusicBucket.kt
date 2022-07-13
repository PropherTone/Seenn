package com.protone.database.room.entity

import androidx.room.*
import com.protone.database.room.converters.ListTypeConverter

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MusicBucket::class,
            parentColumns = ["musicBucketId"],
            childColumns = ["musicBucketId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Music::class,
            parentColumns = ["musicID"],
            childColumns = ["musicID"],
            onDelete = ForeignKey.CASCADE
        )
    ], indices = [
        Index(
            value = ["musicBucketId", "musicID"],
            unique = true
        )
    ]
)
@TypeConverters(ListTypeConverter::class)
data class MusicWithMusicBucket(
    @PrimaryKey(autoGenerate = true)
    val musicWithMusicBucket_id: Long?,
    val musicBucketId: Long,
    val musicID: Long
)