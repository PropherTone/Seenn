package com.protone.api.spans

interface ISpanForUse : ISpan {
    fun setSize()
    fun setColor(isBackGround: Boolean)
    fun insertImage()
    fun insertVideo()
    fun insertMusic()
}