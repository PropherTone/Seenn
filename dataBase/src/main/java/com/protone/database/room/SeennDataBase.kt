package com.protone.database.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.protone.api.context.Global
import com.protone.database.room.dao.*
import com.protone.database.room.entity.*
import java.lang.ref.SoftReference

@Database(
    entities = [GalleyMedia::class, Note::class, NoteType::class, MusicBucket::class, Music::class, GalleyBucket::class],
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

    companion object {
        @JvmStatic
        val database: SeennDataBase
            @Synchronized get() {
                return databaseImpl?.get() ?: init().apply {
                    databaseImpl = SoftReference(this)
                }
            }

        private var databaseImpl: SoftReference<SeennDataBase>? = null

        private fun init(): SeennDataBase {
            return Room.databaseBuilder(
                Global.app,
                SeennDataBase::class.java,
                "SeennDB"
            ).build()
        }
    }

}