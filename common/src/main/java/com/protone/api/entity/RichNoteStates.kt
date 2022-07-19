package com.protone.api.entity

import com.protone.api.baseType.indexSpan

data class RichNoteStates(val spanStates: List<SpanStates>) : RichStates(name = "") {
    var text: CharSequence? = ""
        get() = spanStates.let { field?.indexSpan(it) }

    constructor(text: CharSequence, spanStates: List<SpanStates>) : this(spanStates) {
        this.text = text
    }
}

data class RichNoteSer(val text : String,val spans : String)