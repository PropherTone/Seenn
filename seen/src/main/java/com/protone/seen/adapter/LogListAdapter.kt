package com.protone.seen.adapter

import android.content.Context
import android.view.ViewGroup
import com.protone.api.context.layoutInflater
import com.protone.api.getFileName
import com.protone.seen.databinding.LogListLayoutBinding

class LogListAdapter(context: Context) : BaseAdapter<LogListLayoutBinding>(context) {

    private val logs = mutableListOf<String>()
    var logEvent: LogEvent? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<LogListLayoutBinding> {
        return Holder(LogListLayoutBinding.inflate(context.layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder<LogListLayoutBinding>, position: Int) {
        holder.binding.apply {
            logName.text = logs[position].getFileName()
            logShare.setOnClickListener {
                logEvent?.shareLog(logs[position])
            }
            logWatch.setOnClickListener {
                logEvent?.viewLog(logs[position])
            }
        }
    }

    override fun getItemCount(): Int = logs.size

    fun initLogs(data: MutableList<String>) {
        data.forEach {
            if (!logs.contains(it)) {
                logs.add(it)
                notifyItemInserted(logs.size)
            }
        }
    }

    interface LogEvent {
        fun shareLog(path: String)
        fun viewLog(path: String)
    }
}