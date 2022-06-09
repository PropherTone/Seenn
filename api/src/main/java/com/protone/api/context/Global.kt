package com.protone.api.context

import android.app.Application
import android.content.Context.WINDOW_SERVICE
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

object Global {

    val application: Application
        get() = application_

    private lateinit var application_: Application

    fun init(application: Application) {
        application_ = application
    }

    fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (application.getSystemService(WINDOW_SERVICE) as WindowManager)
                .currentWindowMetrics
                .bounds
                .height()
        } else {
            val displayMetrics = DisplayMetrics()
            (application.getSystemService(WINDOW_SERVICE) as WindowManager)
                .defaultDisplay
                .getRealMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }
}