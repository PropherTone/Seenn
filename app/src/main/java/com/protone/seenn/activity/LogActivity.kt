package com.protone.seenn.activity

import android.content.Intent
import android.util.Log
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.TAG
import com.protone.api.baseType.getFileName
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.seenn.R
import com.protone.seenn.databinding.LogActivityBinding
import com.protone.ui.adapter.LogListAdapter
import com.protone.ui.dialog.titleDialog
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.viewModel.BaseViewModel
import com.protone.worker.viewModel.LogViewModel
import com.protone.worker.viewModel.NoteEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class LogActivity : BaseActivity<LogActivityBinding, LogViewModel, BaseViewModel.ViewEvent>(false) {
    override val viewModel: LogViewModel by viewModels()

    override fun createView(): LogActivityBinding {
        return LogActivityBinding.inflate(layoutInflater, root, false).apply {
            activity = this@LogActivity
            fitStatuesBar(binding.root)
        }
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

    override suspend fun doStart() {
        viewModel.initLogList()
    }

    fun action() {
        val randomCode = (0..10000).random().toString()
        Log.d(TAG, "--=====<randomCode here: $randomCode>=====--")
        titleDialog(R.string.password.getString(), "") {
            if (it == randomCode) {
                DatabaseHelper.instance.showDataBase(this)
            } else {
                R.string.wrong_password.getString().toast()
            }
        }
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