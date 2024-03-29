package com.protone.database.room.dao

import android.net.Uri
import androidx.room.*
import com.protone.api.converters.UriTypeConverter
import com.protone.api.entity.GalleriesWithNotes
import com.protone.api.entity.GalleryMedia
import com.protone.api.entity.Note

@Dao
@TypeConverters(UriTypeConverter::class)
@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
interface GalleriesWithNotesDAO {

    @Insert
    fun insertGalleriesWithNotes(galleriesWithNotes: GalleriesWithNotes)

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM Note INNER JOIN GalleriesWithNotes ON Note.noteId = GalleriesWithNotes.noteId WHERE GalleriesWithNotes.media_uri LIKE:uri")
    fun getNotesWithGallery(uri: Uri): List<Note>?

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM GalleryMedia INNER JOIN GalleriesWithNotes ON GalleryMedia.media_uri = GalleriesWithNotes.media_uri WHERE GalleriesWithNotes.noteId LIKE:noteId")
    fun getGalleriesWithNote(noteId: Long): List<GalleryMedia>?
}