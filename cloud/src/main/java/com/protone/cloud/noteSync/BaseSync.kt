package com.protone.cloud.noteSync

import java.net.Socket

abstract class BaseSync<T> {
    var mSocket: Socket? = null
    var onSyncListener: CloudStates<T>? = null
    fun closeSocket() {
        mSocket?.apply {
            if (!isClosed) {
                close()
            }
        }
        mSocket = null
        onSyncListener = null
    }
}