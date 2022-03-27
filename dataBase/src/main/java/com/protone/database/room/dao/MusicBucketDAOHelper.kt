package com.protone.database.room.dao

import com.protone.database.room.entity.Music
import com.protone.database.room.entity.MusicBucket
import com.protone.database.room.getMusicBucketDAO

object MusicBucketDAOHelper : BaseDAOHelper() {

    private var musicBucketDAO: MusicBucketDAO? = null

    init {
        musicBucketDAO()
    }

    private fun musicBucketDAO() {
        if (musicBucketDAO == null) {


            musicBucketDAO = getMusicBucketDAO()

        }
    }

    fun getAllMusicBucket(callBack: (result: List<MusicBucket>?) -> Unit) {
        runnableFunc = {
            musicBucketDAO?.getAllMusicBucket()?.apply {
                callBack(this)
            }
        }
    }

    fun addMusicBucket(musicBucket: MusicBucket) {
        runnableFunc = {
            musicBucketDAO?.addMusicBucket(musicBucket)
        }
    }

    fun addMusicBucketWithCallBack(musicBucket: MusicBucket, callBack: (result: Boolean) -> Unit) {
        runnableFunc = {
            musicBucketDAO?.apply {
                var count = 0
                val name = musicBucket.name
                getAllMusicBucket()?.forEach {
                    if (it.name == musicBucket.name) {
                        musicBucket.name = "${name}(${++count})"
                    }
                }
                musicBucket.name = name
                addMusicBucket(musicBucket)
                callBack(getMusicBucketByName(musicBucket.name) == null)
            }
        }
    }

    fun updateMusicBucketName(oldName: String, name: String) {
        runnableFunc = {
            musicBucketDAO?.updateMusicBucketName(oldName, name)
        }
    }

    fun updateMusicBucketIcon(bucketName: String, icon: String) {
        runnableFunc = {
            musicBucketDAO?.updateMusicBucketIcon(bucketName, icon)
        }
    }

}