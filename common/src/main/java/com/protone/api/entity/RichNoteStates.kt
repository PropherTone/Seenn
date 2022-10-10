package com.protone.api.entity

import com.protone.api.spans.base64ToCharSequence

class RichNoteStates(text: CharSequence) : RichStates("") {
    var text: CharSequence? = text
        get() = field?.base64ToCharSequence()
}

data class RichNoteSer(val text: String, val spans: String)

data class SpanStyle(val span: Any, val start: Int, val end: Int)