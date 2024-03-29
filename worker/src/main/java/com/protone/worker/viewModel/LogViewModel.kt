package com.protone.worker.viewModel

import com.protone.api.SCrashHandler
import com.protone.api.baseType.getParentPath
import com.protone.api.spans.toBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader

class LogViewModel : BaseViewModel() {

    fun getLogContent(path:String): String {
        val file = File(path)
        val fileReader = FileReader(file)
        return fileReader.readText().toBase64()
    }

    suspend fun getLogs(): MutableList<String>? {
       return SCrashHandler.path?.let { path ->
            withContext(Dispatchers.IO) {
                val file = File(path.getParentPath())
                val logs = mutableListOf<String>()
                file.listFiles()?.forEach { log ->
                    log?.path?.let { logPath -> logs.add(logPath) }
                }
                logs
            }
        }
    }
}