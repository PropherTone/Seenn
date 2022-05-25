package com.protone.database.room.dao

import java.util.concurrent.Executors

abstract class BaseDAOHelper {
    private val executorService by lazy { Executors.newSingleThreadExecutor() }

    fun execute(runnable: Runnable) = executorService.execute(runnable)

    fun shutdownNow() {
        if (executorService.isShutdown) {
            executorService.shutdownNow()
        }
    }
}