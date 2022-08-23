package com.protone.worker.media

import androidx.lifecycle.MutableLiveData
import com.protone.api.entity.Music
import com.protone.worker.MusicState

interface IMusicPlayer {
    fun setDate(list: MutableList<Music>) {}
    fun play()
    fun play(music: Music, progress: Long)
    fun pause()
    fun next()
    fun previous()
    fun getPosition(): MutableLiveData<Long>
    fun getMusicDetail(): Music
    fun getData(): MusicState
    fun setPlayMusicPosition(position: Int)
    fun seekTo(position: Long)
    fun getPlayState(): MutableLiveData<Boolean>
    fun getPlayPosition(): Int
}