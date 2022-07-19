package com.protone.database.room.dao

import androidx.room.*
import com.protone.api.entity.Note

@Dao
interface NoteDAO {

    @Query("SELECT * FROM Note ORDER BY Note_Time DESC")
    fun getAllNote(): List<Note>?

    @Insert
    fun insertNote(note: Note): Long

    @Query("SELECT * FROM Note WHERE Note_Title LIKE :name")
    fun getNoteByName(name: String): Note?

    @Update
    fun updateNote(note: Note): Int?

    @Delete
    fun deleteNote(note: Note)

}