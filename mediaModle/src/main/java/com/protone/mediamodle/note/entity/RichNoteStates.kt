package com.protone.mediamodle.note.entity

import com.protone.mediamodle.note.spans.indexSpan

data class RichNoteStates(val spanStates: List<SpanStates>?) {
    var text: CharSequence? = ""
        get() = if (spanStates != null) spanStates.let { field?.indexSpan(it) } else field

    constructor(text: CharSequence, spanStates: List<SpanStates>?) : this(spanStates) {
        this.text = text
    }
}