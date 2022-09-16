package com.protone.ui.customView.richText.note.spans

interface ISpanForUse : ISpan {
    fun setSize()
    fun setColor()
    fun insertImage()
    fun insertVideo()
    fun insertMusic()
}