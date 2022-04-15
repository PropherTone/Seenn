package com.protone.mediamodle.note.spans

import com.protone.mediamodle.note.entity.RichMusicStates
import com.protone.mediamodle.note.entity.RichPhotoStates
import com.protone.mediamodle.note.entity.RichVideoStates

interface ISpanForEditor : ISpan {
    fun setSize(size: Int)
    fun setColor(color: Any)
    fun insertImage(photo: RichPhotoStates)
    fun insertVideo(video: RichVideoStates)
    fun insertMusic(music: RichMusicStates)
}