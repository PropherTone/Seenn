package com.protone.database.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.protone.database.room.entity.Note

@Dao
interface NoteDAO {

    @Query("SELECT * FROM Note")
    fun getAllNote(): List<Note>?

    @Insert
    fun insertNote(note: Note)

    @Query("SELECT * FROM Note WHERE Title LIKE :name")
    fun getNoteByName(name: String) : Note?

}