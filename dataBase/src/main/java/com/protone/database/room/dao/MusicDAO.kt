package com.protone.database.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.protone.database.room.entity.Music


@Dao
interface MusicDAO {

    @Insert
    fun insertMusic(music: Music)

    @Query("SELECT * FROM Music")
    fun getAllMusic() : List<Music>
}