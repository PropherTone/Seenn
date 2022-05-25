package com.protone.seenn.broadcast

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import com.protone.api.context.UPDATE_GALLEY
import com.protone.api.context.UPDATE_MUSIC
import com.protone.mediamodle.workLocalBroadCast

class MediaContentObserver(mHandler: Handler) : ContentObserver(mHandler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        val uriString = uri.toString()
        when {
            uriString.contains("audio") -> {
                workLocalBroadCast.sendBroadcast(Intent().apply {
                    action = UPDATE_MUSIC
                    if (isUpdate(uri ?: Uri.EMPTY)) data = uri
                })
            }
            else -> {
                workLocalBroadCast.sendBroadcast(Intent().apply {
                    action = UPDATE_GALLEY
                    if (isUpdate(uri ?: Uri.EMPTY)) data = uri
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
            }
        } else false
    }

}