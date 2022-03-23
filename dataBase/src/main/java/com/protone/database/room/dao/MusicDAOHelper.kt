package com.protone.database.room.dao

import android.util.Log
import com.protone.api.TAG
import com.protone.database.room.SeennDataBase
import com.protone.database.room.entity.Music

object MusicDAOHelper : BaseDAOHelper() {

    private var musicDAO: MusicDAO? = null

    init {
        if (musicDAO == null) {
            musicDAO = SeennDataBase.database.getMusicDAO()
        }
    }

    fun insertMusicMulti(music: List<Music>) {
        runnableFunc = {
            music.forEach {
                musicDAO?.insertMusic(it)
            }
        }
    }

    fun insertMusic(music: Music) {
        runnableFunc = {
            musicDAO?.insertMusic(music)
        }
    }

    fun getAllMusic(callBack: (List<Music>) -> Unit) {
        runnableFunc = {
            musicDAO?.getAllMusic()?.let {
                callBack(it)
            }
        }
    }

}