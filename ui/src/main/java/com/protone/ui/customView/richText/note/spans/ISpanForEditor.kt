package com.protone.ui.customView.richText.note.spans

import com.protone.api.entity.RichMusicStates
import com.protone.api.entity.RichPhotoStates
import com.protone.api.entity.RichVideoStates

interface ISpanForEditor : ISpan {
    fun setSize(size: Int)
    fun setColor(color: Any)
    fun insertImage(photo: RichPhotoStates)
    fun insertVideo(video: RichVideoStates)
    fun insertMusic(music: RichMusicStates)
}