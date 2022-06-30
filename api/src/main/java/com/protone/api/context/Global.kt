package com.protone.api.context

import android.app.Application
import android.content.Context.WINDOW_SERVICE
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

object Global {

    val app: Application
        get() = application

    private lateinit var application: Application

    fun init(application: Application) {
        this.application = application
    }

    fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (app.getSystemService(WINDOW_SERVICE) as WindowManager)
                .currentWindowMetrics
                .bounds
                .height()
        } else {
            val displayMetrics = DisplayMetrics()
            (app.getSystemService(WINDOW_SERVICE) as WindowManager)
                .defaultDisplay
                .getRealMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }
}