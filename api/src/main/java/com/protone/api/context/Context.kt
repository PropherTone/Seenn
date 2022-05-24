package com.protone.api.context

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

val Context.currentHeight: Int
    get() = if (this is Activity) {
        val display = windowManager.defaultDisplay
        val outPoint = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            display.getRealSize(outPoint)
            outPoint.y
        }
    } else 0

val Context.getHeight: Int
    get() = resources.displayMetrics.heightPixels

val Context.hasNavigationBar: Boolean
    get() = currentHeight > getHeight

inline fun Context.onUiThread(crossinline function: () -> Unit) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        function.invoke()
        return
    }
    when (this) {
        is Activity -> runOnUiThread { function.invoke() }
        else -> CoroutineScope(Dispatchers.Main).launch { function.invoke() }
    }
}

inline fun onBackground(crossinline function: () -> Unit) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        Thread {
            function.invoke()
        }.start()
    } else function.invoke()
}
