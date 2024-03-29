package com.protone.database.room.dao

import androidx.room.*
import com.protone.api.entity.Music
import com.protone.api.entity.MusicBucket
import com.protone.api.entity.MusicWithMusicBucket

@Dao
@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
interface MusicWithMusicBucketDAO {

    @Insert
    fun insertMusicWithMusicBucket(musicWithMusicBucket: MusicWithMusicBucket): Long?

    @Query("DELETE FROM MusicWithMusicBucket WHERE musicBaseId LIKE :musicID AND musicBucketId LIKE:musicBucketId")
    @RewriteQueriesToDropUnusedColumns
    fun deleteMusicWithMusicBucket(musicID: Long,musicBucketId: Long)

    @Query("SELECT * FROM Music INNER JOIN MusicWithMusicBucket ON Music.musicBaseId = MusicWithMusicBucket.musicBaseId WHERE MusicWithMusicBucket.musicBucketId LIKE:musicBucketId")
    @RewriteQueriesToDropUnusedColumns
    fun getMusicWithMusicBucket(musicBucketId: Long): List<Music>?

    @Query("SELECT * FROM MusicBucket INNER JOIN MusicWithMusicBucket ON MusicBucket.musicBucketId = MusicWithMusicBucket.musicBucketId WHERE MusicWithMusicBucket.musicBaseId LIKE:musicID")
    @RewriteQueriesToDropUnusedColumns
    fun getMusicBucketWithMusic(musicID: Long): List<MusicBucket>?
}