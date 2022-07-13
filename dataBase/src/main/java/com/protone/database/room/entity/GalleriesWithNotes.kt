package com.protone.database.room.entity

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.protone.database.room.converters.ListTypeConverter

@Entity(
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
            value = ["mediaId", "noteId"],
            unique = true
        )
    ]
)
@TypeConverters(ListTypeConverter::class)
data class GalleriesWithNotes(
    @PrimaryKey(autoGenerate = true)
    val galleriesWithNotes_id: Long?,
    val mediaId: Long,
    val noteId: Long
)