package com.protone.worker.note.spans

interface ISpanForUse : ISpan {
    fun setSize()
    fun setColor()
    fun insertImage()
    fun insertVideo()
    fun insertMusic()
}