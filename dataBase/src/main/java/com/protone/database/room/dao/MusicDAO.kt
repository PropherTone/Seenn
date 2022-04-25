package com.protone.database.room.dao

import android.net.Uri
import androidx.room.*
import com.protone.database.room.converters.UriTypeConverter
import com.protone.database.room.entity.Music


@Dao
@TypeConverters(UriTypeConverter::class)
interface MusicDAO {

    @Insert
    fun insertMusic(music: Music)

    @Query("SELECT * FROM Music")
    fun getAllMusic(): List<Music>?

    @Delete
    fun deleteMusic(music: Music)

    @Update(entity = Music::class)
    fun updateMusic(music: Music): Int

    @Query("UPDATE Music SET myBucket = :bucket WHERE title == :name")
    fun updateMusicMyBucket(name: String, bucket: List<String>): Int

    @Query("SELECT * FROM Music WHERE uri LIKE :uri")
    fun getMusicByUri(uri: Uri): Music?

}