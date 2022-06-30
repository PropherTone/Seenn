package com.protone.database.room.dao

import kotlinx.coroutines.*

abstract class BaseDAOHelper {
    val executorService by lazy { CoroutineScope(Dispatchers.IO) }

    inline fun execute(crossinline runnable: suspend () -> Unit) {
        executorService.launch(Dispatchers.IO) {
            runnable.invoke()
        }
    }

    suspend inline fun <T> onResult(crossinline runnable: (CancellableContinuation<T>) -> Unit) =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine<T> {
                runnable.invoke(it)
            }
        }

    fun shutdownNow() {
        if (executorService.isActive) {
            executorService.cancel()
        }
    }
}