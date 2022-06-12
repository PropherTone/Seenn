package com.protone.seenn.broadcast

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import com.protone.api.context.UPDATE_GALLEY
import com.protone.api.context.UPDATE_MUSIC

class MediaContentObserver(mHandler: Handler) : ContentObserver(mHandler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        val uriString = uri.toString()
        when {
            uriString.contains("audio") -> {
                workLocalBroadCast.sendBroadcast(Intent().apply {
                    if (isUpdate(uri ?: Uri.EMPTY)) putExtra("uri", uriString)
                    action = UPDATE_MUSIC
                })
            }
            else -> {
                workLocalBroadCast.sendBroadcast(Intent().apply {
                    if (isUpdate(uri ?: Uri.EMPTY)) putExtra("uri", uriString)
                    action = UPDATE_GALLEY
                })
            }
        }
    }

    private fun isUpdate(uri: Uri): Boolean {
        val split = uri.toString().split("/")
        return if (split.isNotEmpty()) {
            try {
                split[split.size - 1].toInt()
                true
            } catch (e: TypeCastException) {
                false
            } catch (e: NumberFormatException) {
                false
            }
        } else false
    }

}