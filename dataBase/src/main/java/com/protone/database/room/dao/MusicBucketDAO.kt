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

    @Query("UPDATE MusicBucket SET name = :name WHERE name LIKE :oldName")
    fun updateMusicBucketName(oldName: String, name: String)

    @Query("UPDATE MusicBucket SET icon = :icon WHERE name LIKE :bucketName")
    fun updateMusicBucketIcon(bucketName: String, icon: ByteArray)

}