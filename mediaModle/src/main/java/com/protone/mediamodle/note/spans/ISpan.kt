package com.protone.mediamodle.note.spans

import android.net.Uri

interface ISpan {
    fun setBold()
    fun setItalic()
    fun setSize(size: Int) {}
    fun setSize() {}
    fun setUnderlined()
    fun setStrikethrough()
    fun setColor(color: Any) {}
    fun setColor() {}
    fun setImage(uri: Uri, link: String?, name: String, date: String?) {}
    fun setImage() {}
}