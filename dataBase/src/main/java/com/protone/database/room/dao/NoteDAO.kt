package com.protone.database.room.dao

import androidx.room.*
import com.protone.database.room.entity.Note

@Dao
interface NoteDAO {

    @Query("SELECT * FROM Note")
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