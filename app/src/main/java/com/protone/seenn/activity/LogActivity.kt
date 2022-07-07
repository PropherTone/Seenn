package com.protone.seenn.activity

import android.content.Intent
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.getFileName
import com.protone.seen.adapter.LogListAdapter
import com.protone.seenn.R
import com.protone.seenn.databinding.LogActivityBinding
import com.protone.seenn.viewModel.LogViewModel
import com.protone.seenn.viewModel.NoteEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class LogActivity : BaseActivity<LogActivityBinding, LogViewModel>(false) {
    override val viewModel: LogViewModel by viewModels()

    override fun initView() {
        binding = LogActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
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
                        toast(getString(R.string.failed_msg))
                    }
                }
            }

        })
    }

    override suspend fun onViewEvent(event: String) = Unit

    override suspend fun doStart() {
        viewModel.initLogList()
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