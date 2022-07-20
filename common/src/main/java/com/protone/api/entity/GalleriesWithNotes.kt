package com.protone.api.entity

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
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
    @ColumnInfo(name = "mediaId")
    val mediaId: Long,
    @ColumnInfo(name = "noteId")
    val noteId: Long
)