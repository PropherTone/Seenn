package com.protone.database.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.protone.database.room.entity.Music
import com.protone.database.room.entity.MusicBucket
import com.protone.database.room.entity.MusicWithMusicBucket

@Dao
interface MusicWithMusicBucketDAO {

    @Insert
    fun insertMusicWithMusicBucket(musicWithMusicBucket: MusicWithMusicBucket): Long?

    @Query("DELETE FROM MusicWithMusicBucket WHERE musicBaseId LIKE :musicID")
    fun deleteMusicWithMusicBucket(musicID: Long)

    @Query("SELECT * FROM Music INNER JOIN MusicWithMusicBucket ON Music.musicBaseId = MusicWithMusicBucket.musicBaseId WHERE MusicWithMusicBucket.musicBucketId LIKE:musicBucketId")
    fun getMusicWithMusicBucket(musicBucketId: Long): List<Music>?

    @Query("SELECT * FROM MusicBucket INNER JOIN MusicWithMusicBucket ON MusicBucket.musicBucketId = MusicWithMusicBucket.musicBucketId WHERE MusicWithMusicBucket.musicBaseId LIKE:musicID")
    fun getMusicBucketWithMusic(musicID: Long): List<MusicBucket>?
}