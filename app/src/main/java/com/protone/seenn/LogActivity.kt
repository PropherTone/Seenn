package com.protone.seenn

import android.content.Intent
import androidx.core.content.FileProvider
import com.protone.api.SCrashHandler
import com.protone.api.getParentPath
import com.protone.seen.LogSeen
import com.protone.seen.adapter.LogListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.File

class LogActivity : BaseActivity<LogSeen>() {

    override suspend fun main() {
        val logSeen = LogSeen(this)
        setContentSeen(logSeen)

        while (isActive) {
            select<Unit> {
                event.onReceive {
                    when (it) {
                        Event.OnStart -> {
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

                                }

                            })
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