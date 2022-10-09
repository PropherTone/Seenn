package com.protone.ui.customView.richText.note.spans

import android.graphics.Bitmap
import android.view.View
import com.protone.api.entity.RichMusicStates
import com.protone.api.entity.RichNoteStates
import com.protone.api.entity.RichPhotoStates
import com.protone.api.entity.RichVideoStates

interface ISpanForEditor : ISpan {
    fun setSize(size: Int)
    fun setColor(color: Any)
    fun insertText(note: RichNoteStates)
    fun insertImage(photo: RichPhotoStates,ba: Bitmap?)
    fun insertVideo(video: RichVideoStates)
    fun insertMusic(music: RichMusicStates)

    fun getChild(index:Int) : View?
}