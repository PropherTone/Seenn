package com.protone.seen

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.adapter.LogListAdapter
import com.protone.seen.databinding.LogLayoutBinding

class LogSeen(context: Context) : Seen<LogSeen.LogEvent>(context) {

    enum class LogEvent {
        Finish
    }

    private val binding = LogLayoutBinding.inflate(context.layoutInflater, context.root, false)

    override val viewRoot: View = binding.root

    override fun getToolBar(): View = binding.root

    init {
        initToolBar()
        binding.self = this
    }

    override fun offer(event: LogEvent) {
        viewEvent.trySend(event)
    }

    fun initLogList(logs: MutableList<String>) {
        (binding.logList.adapter as LogListAdapter).initLogs(logs)
    }

    fun setLogEvent(event : LogListAdapter.LogEvent){
        binding.logList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = LogListAdapter(context)
        }
        (binding.logList.adapter as LogListAdapter).logEvent = event
    }

}