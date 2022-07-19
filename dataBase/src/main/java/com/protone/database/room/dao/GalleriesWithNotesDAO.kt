package com.protone.database.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.protone.api.entity.GalleriesWithNotes
import com.protone.api.entity.GalleyMedia
import com.protone.api.entity.Note

@Dao
interface GalleriesWithNotesDAO {

    @Insert
    fun insertGalleriesWithNotes(galleriesWithNotes: GalleriesWithNotes)

    @Query("SELECT * FROM Note INNER JOIN GalleriesWithNotes ON Note.noteId = GalleriesWithNotes.noteId WHERE GalleriesWithNotes.mediaId LIKE:mediaId")
    fun getNotesWithGalley(mediaId: Long): List<Note>?

    @Query("SELECT * FROM GalleyMedia INNER JOIN GalleriesWithNotes ON GalleyMedia.mediaId = GalleriesWithNotes.noteId WHERE GalleriesWithNotes.noteId LIKE:noteId")
    fun getGalleriesWithNote(noteId: Long): List<GalleyMedia>?
}