package com.protone.database.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.protone.database.room.entity.GalleyBucket

@Dao
interface GalleyBucketDAO {

    @Insert
    fun insertGalleyBucket(galleyBucket: GalleyBucket)

    @Query("SELECT * FROM GalleyBucket WHERE type LIKE :name")
    fun getGalleyBucket(name: String): GalleyBucket?

    @Delete
    fun deleteGalleyBucket(galleyBucket: GalleyBucket)

    @Query("SELECT * FROM GalleyBucket")
    fun getALLGalleyBucket(): List<GalleyBucket>?
}