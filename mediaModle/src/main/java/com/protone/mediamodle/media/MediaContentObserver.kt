package com.protone.mediamodle.media

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.protone.api.TAG

class MediaContentObserver(mHandler: Handler) : ContentObserver(mHandler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d(TAG, "onChange: $uri")
    }

}