package com.protone.database.room.dao

import androidx.room.*
import com.protone.database.room.entity.Music


@Dao
interface MusicDAO {

    @Insert
    fun insertMusic(music: Music)

    @Query("SELECT * FROM Music")
    fun getAllMusic() : List<Music>?

    @Delete
    fun deleteMusic(music: Music)

    @Update
    fun updateMusic(music: Music) : Int

}