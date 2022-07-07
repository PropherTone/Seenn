package com.protone.seenn.viewModel

import androidx.lifecycle.ViewModel
import com.protone.api.SCrashHandler
import com.protone.api.getParentPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader

class LogViewModel : ViewModel() {

    fun getLogContent(path:String): String {
        val file = File(path)
        val fileReader = FileReader(file)
        return fileReader.readText()
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