package com.protone.cloud.noteSync

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

const val SUCCESS_MSG = "CONNECTED"
const val FAILED_MSG = "CONNECT FAILED"

interface CloudStates<T> {
    fun success()
    fun failed(msg : String)
    fun successMsg(arg: T)
}

open class IServerConnection : ServiceConnection {
    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {}
    override fun onServiceDisconnected(p0: ComponentName?) {}
}