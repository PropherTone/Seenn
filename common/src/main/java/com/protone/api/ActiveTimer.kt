package com.protone.api

import android.os.Handler
import android.os.Looper

class ActiveTimer(private val delay: Long = 500L) {
    private var timerHandler: Handler? = Handler(Looper.getMainLooper()) {
        funcMap[it.what]?.invoke()
        false
    }

    private val funcMap = mutableMapOf<Int, () -> Unit>()

    fun addFunction(key: Int, func: () -> Unit) {
        funcMap[key] = func
    }

    fun block(key: Int) {
        timerHandler?.removeMessages(key)
    }

    fun active(key: Int) {
        block(key)
        timerHandler?.sendEmptyMessageDelayed(key, delay)
    }

    fun destroy(){
        timerHandler?.removeCallbacksAndMessages(null)
        timerHandler = null
    }
}