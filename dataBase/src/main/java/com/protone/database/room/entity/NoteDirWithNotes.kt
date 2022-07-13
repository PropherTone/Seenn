package com.protone.database.room.entity

import androidx.room.*
import com.protone.database.room.converters.ListTypeConverter

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = NoteDir::class,
            parentColumns = ["noteDirId"],
            childColumns = ["noteDirId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Note::class,
            parentColumns = ["noteId"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ], indices = [
        Index(
            value = ["noteDirId", "noteId"],
            unique = true
        )
    ]
)
@TypeConverters(ListTypeConverter::class)
data class NoteDirWithNotes(
    @PrimaryKey(autoGenerate = true)
    val noteDirWithNotes_id: Long?,
    val noteDirId: Long,
    val noteId: Long
)