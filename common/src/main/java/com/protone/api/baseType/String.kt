package com.protone.api.baseType

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.widget.Toast
import com.protone.api.context.SApplication
import com.protone.api.context.onUiThread
import com.protone.api.entity.SpanStates

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

fun String.getFileName(): String {
    return this.split("/").run { this[this.size - 1] }
}

fun String.getFileMimeType(): String {
    return "." + this.split(".").let { it[it.size - 1] }
}

fun String.getParentPath(): String {
    return this.substring(0, lastIndexOf("/"))
}

fun String.toast() {
    SApplication.app.onUiThread {
        Toast.makeText(SApplication.app, this, Toast.LENGTH_SHORT).show()
    }
}