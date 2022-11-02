package com.protone.api.baseType

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

inline fun CoroutineScope.launchDefault(crossinline func: suspend CoroutineScope.() -> Unit) {
    launch(Dispatchers.Default) {
        func.invoke(this)
    }
}

inline fun CoroutineScope.launchIO(crossinline func: suspend CoroutineScope.() -> Unit) {
    launch(Dispatchers.IO) {
        func.invoke(this)
    }
}

inline fun CoroutineScope.launchMain(crossinline func: suspend CoroutineScope.() -> Unit) {
    launch(Dispatchers.Main) {
        func.invoke(this)
    }
}

suspend inline fun <T> withMainContext(crossinline func: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Main) {
        func.invoke(this)
    }

suspend inline fun <T> withIOContext(crossinline func: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.IO) {
        func.invoke(this)
    }

suspend inline fun <T> withDefaultContext(crossinline func: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Default) {
        func.invoke(this)
    }