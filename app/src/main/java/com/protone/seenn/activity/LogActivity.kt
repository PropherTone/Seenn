package com.protone.seenn.activity

import android.content.Intent
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.baseType.getFileName
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.seen.adapter.LogListAdapter
import com.protone.seenn.R
import com.protone.seenn.database.DatabaseHelper
import com.protone.seenn.databinding.LogActivityBinding
import com.protone.seenn.viewModel.BaseViewModel
import com.protone.seenn.viewModel.LogViewModel
import com.protone.seenn.viewModel.NoteEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class LogActivity : BaseActivity<LogActivityBinding, LogViewModel>(false) {
    override val viewModel: LogViewModel by viewModels()

    override fun createView() {
        binding = LogActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitStatuesBar(binding.root)
    }

    override suspend fun LogViewModel.init() {
        setLogEvent(object : LogListAdapter.LogEvent {
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
                        startActivityWithGainData(
                            getLogContent(path),
                            NoteEditActivity::class.intent.apply {
                                putExtra(NoteEditViewModel.CONTENT_TITLE, path.getFileName())
                            })
                    } catch (e: Exception) {
                        R.string.failed_msg.getString().toast()
                    }
                }
            }

        })
    }

    override suspend fun onViewEvent(event: BaseViewModel.ViewEvent) = Unit

    override suspend fun doStart() {
        viewModel.initLogList()
    }

    fun action(){
        DatabaseHelper.instance.showDataBase(this)
    }

    private suspend fun LogViewModel.initLogList() {
        getLogs()?.let { (binding.logList.adapter as LogListAdapter).initLogs(it) }
    }

    private fun setLogEvent(event: LogListAdapter.LogEvent) {
        binding.logList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = LogListAdapter(context)
        }
        (binding.logList.adapter as LogListAdapter).logEvent = event
    }
}