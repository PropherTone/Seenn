package com.protone.mediamodle.note.entity

import com.protone.mediamodle.note.spans.indexSpan

data class RichNoteStates(val spanStates: List<SpanStates>) {
    var text: CharSequence? = ""
        get() = spanStates.let { field?.indexSpan(it) }

    constructor(text: CharSequence, spanStates: List<SpanStates>) : this(spanStates) {
        this.text = text
    }
}

data class RichNoteSer(val text : String,val spans : String)