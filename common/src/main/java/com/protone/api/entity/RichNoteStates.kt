package com.protone.api.entity

import android.os.Parcel
import android.text.TextUtils
import android.text.style.CharacterStyle
import android.util.Base64

data class RichNoteStates(val spanStates: List<SpanStates>) : RichStates(name = "") {
    var text: CharSequence? = ""
        get() {
            val parcel = Parcel.obtain()
            return try {
                val bytes = Base64.decode(field.toString(), Base64.DEFAULT)
                parcel.unmarshall(bytes, 0, bytes.size)
                parcel.setDataPosition(0)
                TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
            } catch (e: Exception) {
                ""
            } finally {
                parcel.recycle()
            }
        }

    constructor(text: CharSequence, spanStates: List<SpanStates>) : this(spanStates) {
        this.text = text
    }
}

data class RichNoteSer(val text: String, val spans: String)

data class SpanStyle(val span: CharacterStyle, val start: Int, val end: Int)