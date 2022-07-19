package com.protone.database.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.protone.api.context.SApplication
import com.protone.api.entity.*
import com.protone.database.room.dao.*
import java.lang.ref.WeakReference

@Database(
    entities = [
        GalleyMedia::class,
        Note::class,
        NoteDir::class,
        MusicBucket::class,
        Music::class,
        GalleyBucket::class,
        GalleriesWithNotes::class,
        NoteDirWithNotes::class,
        MusicWithMusicBucket::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SeennDataBase : RoomDatabase() {
    abstract fun getGalleyDAO(): SignedGalleyDAO
    abstract fun getNoteDAO(): NoteDAO
    abstract fun getNoteTypeDAO(): NoteTypeDAO
    abstract fun getMusicBucketDAO(): MusicBucketDAO
    abstract fun getMusicDAO(): MusicDAO
    abstract fun getGalleyBucketDAO(): GalleyBucketDAO
    abstract fun getGalleriesWithNotesDAO(): GalleriesWithNotesDAO
    abstract fun getNoteDirWithNoteDAO(): NoteDirWithNoteDAO
    abstract fun getMusicWithMusicBucketDAO(): MusicWithMusicBucketDAO

    companion object {
        @JvmStatic
        val database: SeennDataBase
            @Synchronized get() {
                return databaseImpl?.get() ?: init().apply {
                    databaseImpl = WeakReference(this)
                }
            }

        private var databaseImpl: WeakReference<SeennDataBase>? = null

        private fun init(): SeennDataBase {
            return Room.databaseBuilder(
                SApplication.app,
                SeennDataBase::class.java,
                "SeennDB"
            ).build()
        }
    }

}