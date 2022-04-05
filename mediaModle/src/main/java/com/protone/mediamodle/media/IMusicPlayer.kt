package com.protone.mediamodle.media

import androidx.lifecycle.MutableLiveData
import com.protone.database.room.entity.Music
import com.protone.mediamodle.MusicState

interface IMusicPlayer {
    fun setDate(list: MutableList<Music>)
    fun play()
    fun pause()
    fun next()
    fun previous()
    fun getPosition(): MutableLiveData<Long>
    fun getMusicDetail(): Music
    fun getData(): MusicState
    fun setMusicPosition(position: Int)
    fun seekTo(position: Long)
    fun getPlayState(): MutableLiveData<Boolean>
    fun getPlayPosition(): Int
}