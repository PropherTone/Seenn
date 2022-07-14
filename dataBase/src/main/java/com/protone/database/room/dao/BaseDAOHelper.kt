package com.protone.database.room.dao

import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.isInDebug
import com.protone.database.R
import kotlinx.coroutines.*

abstract class BaseDAOHelper {
    val executorService by lazy { CoroutineScope(Dispatchers.IO) }

    inline fun execute(crossinline runnable: suspend () -> Unit) {
        executorService.launch(Dispatchers.IO) {
            try {
                runnable.invoke()
            } catch (e: Exception) {
                if (isInDebug()) e.printStackTrace()
                R.string.unknown_error.getString().toast()
            } finally {
                cancel()
            }
        }
    }

    suspend inline fun <T> onResult(crossinline runnable: (CancellableContinuation<T>) -> Unit) =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine<T> {
                try {
                    runnable.invoke(it)
                } catch (e: Exception) {
                    if (isInDebug()) e.printStackTrace()
                    R.string.unknown_error.getString().toast()
                }
            }
        }

    fun shutdownNow() {
        if (executorService.isActive) {
            executorService.cancel()
        }
    }
}