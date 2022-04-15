package com.protone.mediamodle.note.spans

import android.net.Uri
import com.protone.mediamodle.note.entity.RichMusicStates
import com.protone.mediamodle.note.entity.RichVideoStates

interface ISpanForUse : ISpan {
    fun setSize()
    fun setColor()
    fun insertImage()
    fun insertVideo()
    fun insertMusic()
}