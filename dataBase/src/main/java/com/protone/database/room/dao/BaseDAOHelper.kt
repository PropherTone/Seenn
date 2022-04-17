package com.protone.database.room.dao

import android.util.Log
import com.protone.api.TAG
import java.util.concurrent.Executors

abstract class BaseDAOHelper {
    private val executorService by lazy { Executors.newSingleThreadExecutor() }

    private val runnable = Runnable {
        if (runnableFunc != null) {
            runnableFunc?.invoke()
            runnableFunc = null
        }
    }

    var runnableFunc: (() -> Unit)? = null
        set(value) {
            synchronized(this) {
                field = value
                execute()
            }
        }

    fun execute() {
        executorService.execute(runnable)
    }

    fun shutdown() {
        if (executorService.isShutdown) {
            executorService.shutdown()
        }
    }

    fun shutdownNow() {
        if (executorService.isShutdown) {
            executorService.shutdownNow()
        }
    }
}