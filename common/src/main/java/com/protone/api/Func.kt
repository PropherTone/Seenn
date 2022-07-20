package com.protone.api

import android.content.pm.ApplicationInfo
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.SApplication
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

fun isInDebug(): Boolean {
    return try {
        val info: ApplicationInfo = SApplication.app.applicationInfo
        (info.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    } catch (e: java.lang.Exception) {
        false
    }
}

fun todayDate(format: String): String = SimpleDateFormat(
    format,
    Locale.getDefault()
).format(Calendar.getInstance(Locale.getDefault()).apply {
    timeInMillis = System.currentTimeMillis()
}.time)

fun tryWithRecording(func: () -> Unit) {
    try {
        func.invoke()
    } catch (e: Exception) {
        if (SCrashHandler.path != null) {
            SCrashHandler.writeLog(e)
        }
    }
}

suspend inline fun <T> onResult(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    crossinline runnable: CoroutineScope.(CancellableContinuation<T>) -> Unit
) = withContext(dispatcher) {
    suspendCancellableCoroutine {
        try {
            runnable.invoke(this, it)
        } catch (e: Exception) {
            if (isInDebug()) e.printStackTrace()
            R.string.unknown_error.getString().toast()
        }
    }
}