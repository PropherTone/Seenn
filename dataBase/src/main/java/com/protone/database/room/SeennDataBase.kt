package com.protone.database.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.protone.api.context.Global
import com.protone.database.room.dao.MusicBucketDAO
import com.protone.database.room.dao.MusicDAO
import com.protone.database.room.dao.NoteDAO
import com.protone.database.room.dao.NoteTypeDAO
import com.protone.database.room.entity.Music
import com.protone.database.room.entity.MusicBucket
import com.protone.database.room.entity.Note
import com.protone.database.room.entity.NoteType
import java.lang.ref.SoftReference

@Database(entities = [Note::class,NoteType::class,MusicBucket::class,Music::class], version = 1, exportSchema = false)
abstract class SeennDataBase : RoomDatabase() {
    abstract fun getNoteDAO() : NoteDAO
    abstract fun getNoteTypeDAO(): NoteTypeDAO
    abstract fun getMusicBucketDAO() : MusicBucketDAO
    abstract fun getMusicDAO() : MusicDAO

    companion object {
        @JvmStatic
        val database : SeennDataBase
            @Synchronized get() {
                return databaseImpl?.get() ?: init().apply {
                    databaseImpl = SoftReference(this)
                }
            }

        private var databaseImpl: SoftReference<SeennDataBase>? = null

        private fun init(): SeennDataBase {
            return Room.databaseBuilder(
                Global.application,
                SeennDataBase::class.java,
                "SeennDB"
            ).build()
        }
    }

}