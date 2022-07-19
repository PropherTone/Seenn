package com.protone.database.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.protone.api.entity.Note
import com.protone.api.entity.NoteDir
import com.protone.api.entity.NoteDirWithNotes

@Dao
interface NoteDirWithNoteDAO {

    @Insert
    fun insertNoteDirWithNote(noteDirWithNotes: NoteDirWithNotes)

    @Query("SELECT * FROM Note INNER JOIN NoteDirWithNotes ON Note.noteId = NoteDirWithNotes.noteId WHERE NoteDirWithNotes.noteDirId LIKE:noteDirId")
    fun getNotesWithNoteDir(noteDirId: Long): List<Note>?

    @Query("SELECT * FROM NoteDir INNER JOIN NoteDirWithNotes ON NoteDir.noteDirId = NoteDirWithNotes.noteDirId WHERE NoteDirWithNotes.noteId LIKE:noteId")
    fun getNoteDirWithNote(noteId: Long): List<NoteDir>?
}