package com.protone.seenn

import android.content.Intent
import androidx.core.content.FileProvider
import com.protone.api.SCrashHandler
import com.protone.api.context.intent
import com.protone.api.getFileName
import com.protone.api.getParentPath
import com.protone.seen.LogSeen
import com.protone.seen.adapter.LogListAdapter
import com.protone.seenn.viewModel.IntentDataHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader

class LogActivity : BaseActivity<LogSeen>() {

    override suspend fun main() {
        val logSeen = LogSeen(this)
        setContentSeen(logSeen)

        logSeen.setLogEvent(object : LogListAdapter.LogEvent {
            override fun shareLog(path: String) {
                startActivity(Intent(Intent.ACTION_SEND).apply {
                    putExtra(
                        Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(
                            this@LogActivity,
                            "com.protone.seenn.fileProvider",
                            File(path)
                        )
                    )
                    type = "text/plain"
                })
            }

            override fun viewLog(path: String) {
                launch(Dispatchers.IO) {
                    try {
                        val file = File(path)
                        val fileReader = FileReader(file)
                        val content = fileReader.readText()
                        val title = path.getFileName()
                        IntentDataHolder.put(content)
                        startActivity(NoteEditActivity::class.intent.apply {
                            putExtra(NoteEditActivity.CONTENT_TITLE,title)
                        })
                    } catch (e: Exception) {
                        toast(getString(R.string.failed_msg))
                    }
                }
            }

        })

        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnStart -> {
                            SCrashHandler.path?.let { path ->
                                withContext(Dispatchers.IO) {
                                    val file = File(path.getParentPath())
                                    val logs = mutableListOf<String>()
                                    file.listFiles()?.forEach { log ->
                                        log?.path?.let { logPath -> logs.add(logPath) }
                                    }
                                    logSeen.initLogList(logs)
                                }
                            }
                        }
                        else -> {}
                    }
                }
                logSeen.viewEvent.onReceive {
                    when (it) {
                        LogSeen.LogEvent.Finish -> finish()
                    }
                }
            }
        }
    }
}