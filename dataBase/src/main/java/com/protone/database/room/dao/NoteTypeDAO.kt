package com.protone.database.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.protone.database.room.entity.NoteType

@Dao
interface NoteTypeDAO {

    @Insert
    fun insertNoteType(noteType: NoteType)

    @Query("SELECT * FROM NoteType WHERE type LIKE :name")
    fun getNoteType(name: String): NoteType?

    @Delete
    fun deleteNoteType(noteType: NoteType)

    @Query("SELECT * FROM NoteType")
    fun getALLNoteType(): List<NoteType>?
}