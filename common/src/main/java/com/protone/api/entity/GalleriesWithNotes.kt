package com.protone.api.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.TypeConverters
import com.protone.api.converters.ListTypeConverter

@Entity(
    primaryKeys = ["mediaId", "noteId"],
    foreignKeys = [
        ForeignKey(
            entity = GalleyMedia::class,
            parentColumns = ["mediaId"],
            childColumns = ["mediaId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = Note::class,
            parentColumns = ["noteId"],
            childColumns = ["noteId"],
            onDelete = CASCADE
        )
    ], indices = [
        Index(
            value = ["noteId", "mediaId"],
            unique = true
        )
    ]
)
@TypeConverters(ListTypeConverter::class)
data class GalleriesWithNotes(
    val mediaId: Long,
    val noteId: Long
)