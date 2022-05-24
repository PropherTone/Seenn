package com.protone.seenn.broadcast

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.protone.api.TAG
import com.protone.api.context.UPDATE_GALLEY
import com.protone.mediamodle.workLocalBroadCast

class MediaContentObserver(mHandler: Handler) : ContentObserver(mHandler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        workLocalBroadCast.sendBroadcast(Intent().setAction(UPDATE_GALLEY))
    }

}