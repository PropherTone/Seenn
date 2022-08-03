package com.protone.database.room.dao

import androidx.room.*
import com.protone.api.entity.Note
import com.protone.api.entity.NoteDir
import com.protone.api.entity.NoteDirWithNotes

@Dao
@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
interface NoteDirWithNoteDAO {

    @Insert
    fun insertNoteDirWithNote(noteDirWithNotes: NoteDirWithNotes)

    @Query("SELECT * FROM Note INNER JOIN NoteDirWithNotes ON Note.noteId = NoteDirWithNotes.noteId WHERE NoteDirWithNotes.noteDirId LIKE:noteDirId")
    @RewriteQueriesToDropUnusedColumns
    fun getNotesWithNoteDir(noteDirId: Long): List<Note>?

    @Query("SELECT * FROM NoteDir INNER JOIN NoteDirWithNotes ON NoteDir.noteDirId = NoteDirWithNotes.noteDirId WHERE NoteDirWithNotes.noteId LIKE:noteId")
    @RewriteQueriesToDropUnusedColumns
    fun getNoteDirWithNote(noteId: Long): List<NoteDir>?
}