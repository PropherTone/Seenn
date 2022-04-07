package com.protone.database.room.dao

import android.util.Log
import com.protone.database.room.SeennDataBase
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Music
import com.protone.database.room.entity.MusicBucket
import com.protone.database.room.getMusicBucketDAO

object DataBaseDAOHelper : BaseDAOHelper(), MusicBucketDAO, MusicDAO, SignedGalleyDAO {

    //MusicBucket
    private var musicBucketDAO: MusicBucketDAO? = null

    init {
        musicBucketDAO()
    }

    private fun musicBucketDAO() {
        if (musicBucketDAO == null) {
            musicBucketDAO = getMusicBucketDAO()
        }
    }

    inline fun getAllMusicBucket(crossinline callBack: (result: List<MusicBucket>?) -> Unit) {
        runnableFunc = {
            getAllMusicBucket()?.apply {
                callBack(this)
            }
        }
    }

    override fun getAllMusicBucket(): List<MusicBucket>? {
        return musicBucketDAO?.getAllMusicBucket()
    }

    override fun getMusicBucketByName(name: String): MusicBucket? {
        return musicBucketDAO?.getMusicBucketByName(name)
    }

    override fun addMusicBucket(musicBucket: MusicBucket) {
        runnableFunc = {
            musicBucketDAO?.addMusicBucket(musicBucket)
        }
    }

    inline fun addMusicBucketWithCallBack(
        musicBucket: MusicBucket,
        crossinline callBack: (result: Boolean, name: String) -> Unit
    ) {
        runnableFunc = {
            var count = 0
            val name = musicBucket.name
            val names = mutableMapOf<String, Int>()
            getAllMusicBucket()?.forEach {
                names[it.name] = 1
                if (it.name == musicBucket.name) {
                    musicBucket.name = "${name}(${++count})"
                }
            }
            while (names[musicBucket.name] != null) {
                musicBucket.name = "${name}(${++count})"
            }
            addMusicBucket(musicBucket)
            callBack(getMusicBucketByName(musicBucket.name) == null, musicBucket.name)
        }

    }

    override fun updateMusicBucketName(oldName: String, name: String) {
        runnableFunc = {
            musicBucketDAO?.updateMusicBucketName(oldName, name)
        }
    }

    override fun updateMusicBucketIcon(bucketName: String, icon: ByteArray) {
        runnableFunc = {
            musicBucketDAO?.updateMusicBucketIcon(bucketName, icon)
        }
    }


    //Music
    private var musicDAO: MusicDAO? = null

    init {
        if (musicDAO == null) {
            musicDAO = SeennDataBase.database.getMusicDAO()
        }
    }

    fun insertMusicMulti(music: List<Music>) {
        Log.d("TAG", "insertMusicMulti: ${music.size}")
        runnableFunc = {
            music.forEach {
                musicDAO?.insertMusic(it)
            }
        }
    }

    fun deleteMusicMulti(music: List<Music>) {
        runnableFunc = {
            music.forEach {
                musicDAO?.deleteMusic(it)
            }
        }
    }


    inline fun getAllMusic(crossinline callBack: (List<Music>) -> Unit) {
        runnableFunc = {
            getAllMusic()?.let {
                callBack(it)
            }
        }
    }

    override fun insertMusic(music: Music) {
        runnableFunc = {
            musicDAO?.insertMusic(music)
        }
    }

    override fun getAllMusic(): List<Music>? = musicDAO?.getAllMusic()

    override fun deleteMusic(music: Music) {
        runnableFunc = {
            musicDAO?.deleteMusic(music)
        }
    }

    override fun updateMusic(music: Music): Int = musicDAO?.updateMusic(music) ?: -1

    override fun updateMusicMyBucket(name: String, bucket: List<String>): Int =
        musicDAO?.updateMusicMyBucket(name, bucket) ?: -1

    inline fun updateMusicCB(music: Music, crossinline callBack: (Int) -> Unit) {
        runnableFunc = {
            callBack(updateMusic(music))
        }
    }

    inline fun updateMusicMyBucketCB(
        name: String,
        bucket: List<String>,
        crossinline callBack: (Int) -> Unit
    ) {
        runnableFunc = {
            callBack(updateMusicMyBucket(name, bucket))
        }
    }

    //Galley
    private var signedGalleyDAO: SignedGalleyDAO? = null

    init {
        if (signedGalleyDAO == null) {
            signedGalleyDAO = SeennDataBase.database.getGalleyDAO()
        }
    }

    inline fun getAllSignedMedia(crossinline callBack: (List<GalleyMedia>) -> Unit) {
        runnableFunc = {
            getAllSignedMedia()?.let {
                callBack(it)
            }
        }
    }

    override fun getAllSignedMedia(): List<GalleyMedia>? = signedGalleyDAO?.getAllSignedMedia()

    override fun deleteSignedMedia(media: GalleyMedia) {
        runnableFunc = {
            signedGalleyDAO?.deleteSignedMedia(media)
        }
    }

    override fun insertSignedMedia(media: GalleyMedia) {
        runnableFunc = {
            signedGalleyDAO?.insertSignedMedia(media)
        }
    }

}