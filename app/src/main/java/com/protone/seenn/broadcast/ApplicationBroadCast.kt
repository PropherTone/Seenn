package com.protone.seenn.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.protone.api.context.FINISH
import com.protone.api.context.MUSIC
import kotlin.system.exitProcess

abstract class ApplicationBroadCast : BroadcastReceiver() {

    abstract fun finish()
    abstract fun music()

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            FINISH -> finish()
            MUSIC -> music()
        }
    }
}