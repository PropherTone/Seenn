package com.protone.database.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.protone.database.room.converters.ListTypeConverter

@Entity(
    primaryKeys = ["noteDirId","noteId"],
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
            value = ["noteId", "noteDirId"],
            unique = true
        )
    ]
)
@TypeConverters(ListTypeConverter::class)
data class NoteDirWithNotes(
    val noteDirId: Long,
    val noteId: Long
)