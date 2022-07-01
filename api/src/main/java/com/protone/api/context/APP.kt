package com.protone.api.context

import android.app.Application

object APP {

    val app: Application get() = application

    var screenHeight: Int = 0
    var screenWidth: Int = 0

    private lateinit var application: Application

    fun init(application: Application) {
        this.application = application
    }
}