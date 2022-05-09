package com.protone.database.room.dao

import androidx.room.*
import com.protone.database.room.entity.MusicBucket

@Dao
interface MusicBucketDAO {

    @Query("SELECT * FROM MusicBucket")
    fun getAllMusicBucket(): List<MusicBucket>?

    @Query("SELECT * FROM MusicBucket WHERE name LIKE :name")
    fun getMusicBucketByName(name: String): MusicBucket?

    @Insert
    fun addMusicBucket(musicBucket: MusicBucket)

    @Update
    fun updateMusicBucket(bucket: MusicBucket)

    @Delete
    fun deleteMusicBucket(bucket: MusicBucket)

}