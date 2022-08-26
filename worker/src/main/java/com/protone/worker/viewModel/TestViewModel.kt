package com.protone.worker.viewModel

import android.annotation.SuppressLint
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import com.protone.api.TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect

class TestViewModel : BaseViewModel() {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val flow = MutableSharedFlow<Int>()

    fun fun1() {
        coroutineScope.launch {
            flow.buffer().collect {
                Log.d(TAG, "collect: $it")
            }
        }
    }

    fun fun2() {
        viewModelScope.launch {
            while (isActive) {
                (0..100).random().let {
                    Log.d(TAG, "emit: $it")
                    flow.emit(it)
                    delay(500L)
                }
            }
        }
    }

    fun fun3() {
        coroutineScope.cancel()
    }

    fun fun4() {

    }

    fun func5() {

    }

    fun func6() {}

    fun func7() {}

    fun func8() {}

    fun func9() {}

    @SuppressLint("StaticFieldLeak")
    private lateinit var logText: TextView
    val stringBuilder = StringBuilder()

    fun setLogView(textView: TextView) {
        logText = textView
    }

    fun log(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            stringBuilder.append("$msg\n")
            logText.text = stringBuilder.toString()
            Log.d(TAG, msg)
        }
    }

    private suspend fun runningTime(name: String, block: suspend () -> Unit) {
        val start = System.currentTimeMillis()
        try {
            log("$name start running")
            block.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        log("$name running time: ${System.currentTimeMillis() - start}\n")
    }

    private fun runningTimeNoSuspend(block: () -> Unit) {
        val start = System.currentTimeMillis()
        try {
            log("function start running")
            block.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        log("function running time: ${System.currentTimeMillis() - start}\n")
    }
}