package com.protone.api.context

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup

val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

val Context.root: ViewGroup?
    get() {
        return when (this) {
            is Activity -> {
                findViewById(android.R.id.content)
            }
            else -> null
        }
    }

val Context.statuesBarHeight: Int
    get() = resources.getDimensionPixelSize(
        resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
        )

    )

val Context.navigationBarHeight: Int
    get() = resources.getDimensionPixelSize(
        resources.getIdentifier(
            "navigation_bar_height",
            "dimen",
            "android"
        )
    )


fun onBackground(function: () -> Unit) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        Thread {
            function()
        }.start()
    } else function()
}
