package com.protone.api.context

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService
import java.util.*
import java.util.concurrent.LinkedBlockingDeque

object ActivityHolder {
    private val activities = Stack<Activity>()

    fun add(activity: Activity) {
        activities.add(activity)
    }

    fun remove(activity: Activity) {
        if (activities.contains(activity)) {
            activities.remove(activity)
        }
    }

    fun finish() {
        activities.onEach {
            it.finish()
        }
        activities.clear()
    }
}