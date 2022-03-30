package com.protone.mediamodle.note.spans

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.util.Log
import com.protone.mediamodle.note.entity.SpanStates

fun CharSequence.indexSpan(spans: List<*>): CharSequence {
    val str = when (this) {
        is Spannable -> this
        else -> SpannableString(this)
    }
    spans.forEach {
        when (it) {
            is SpanStates -> {
                str.apply {
                    setSpan(
                        it.getTargetSpan(),
                        it.start,
                        it.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            else -> {
            }
        }
    }
    return str
}