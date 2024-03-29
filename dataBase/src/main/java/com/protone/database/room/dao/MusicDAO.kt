package com.protone.database.room.dao

import android.net.Uri
import androidx.room.*
import com.protone.api.converters.UriTypeConverter
import com.protone.api.entity.Music


@Dao
@TypeConverters(UriTypeConverter::class)
interface MusicDAO {

    @Insert
    fun insertMusic(music: Music)

    @Query("SELECT * FROM Music ORDER BY year DESC")
    fun getAllMusic(): List<Music>?

    @Delete
    fun deleteMusic(music: Music)

    @Update(entity = Music::class)
    fun updateMusic(music: Music): Int

    @Query("SELECT * FROM Music WHERE uri LIKE :uri")
    fun getMusicByUri(uri: Uri): Music?

}