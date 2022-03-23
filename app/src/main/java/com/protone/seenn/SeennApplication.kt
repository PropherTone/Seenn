package com.protone.seenn

import android.app.Application
import android.content.Context
import com.protone.api.Config
import com.protone.api.context.Global

@Suppress("unused")
class SeennApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Global.init(this)
    }

    override fun onCreate() {
        super.onCreate()
        Config.statusBarHeight = resources.getDimensionPixelSize(
            resources.getIdentifier("status_bar_height", "dimen", "android")
        )
    }
}