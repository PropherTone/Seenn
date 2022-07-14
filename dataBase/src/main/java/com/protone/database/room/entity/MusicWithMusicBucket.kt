package com.protone.database.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.protone.database.room.converters.ListTypeConverter

@Entity(
    primaryKeys = ["musicBucketId", "musicBaseId"],
    foreignKeys = [
        ForeignKey(
            entity = MusicBucket::class,
            parentColumns = ["musicBucketId"],
            childColumns = ["musicBucketId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Music::class,
            parentColumns = ["musicBaseId"],
            childColumns = ["musicBaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ], indices = [
        Index(
            value = ["musicBaseId", "musicBucketId"],
            unique = true
        )
    ]
)
@TypeConverters(ListTypeConverter::class)
data class MusicWithMusicBucket(
    val musicBucketId: Long,
    val musicBaseId: Long
)