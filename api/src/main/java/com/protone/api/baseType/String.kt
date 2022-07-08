package com.protone.api.baseType

import android.widget.Toast
import com.protone.api.context.SApplication
import com.protone.api.context.onUiThread

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