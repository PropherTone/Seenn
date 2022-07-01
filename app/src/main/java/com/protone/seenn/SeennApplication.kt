package com.protone.seenn

import android.app.Application
import android.content.Context
import com.protone.api.SCrashHandler
import com.protone.api.context.APP
import com.protone.api.todayDate
import java.io.File

@Suppress("unused")
class SeennApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        APP.init(this)
        val file = File("${base?.externalCacheDir?.path}/CrashLog")
        val result = if (!file.exists()) {
            file.mkdirs()
        } else true
        val todayDate = todayDate("yyyy_MM_dd_HH_mm_ss")
        SCrashHandler.path =
            if (result) "${base?.externalCacheDir?.path}/CrashLog/s_crash_log_${todayDate}.txt"
            else "${base?.externalCacheDir?.path}/s_crash_log_${todayDate}.txt"

    }
}