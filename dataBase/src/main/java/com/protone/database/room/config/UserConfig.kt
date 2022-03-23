package com.protone.database.room.config

import android.content.Context
import android.content.SharedPreferences
import com.protone.database.sp.SpTool
import com.protone.database.sp.toSpProvider

class UserConfig(context: Context) {
    private val config =
        SpTool(context.getSharedPreferences("USER_CONFIG", Context.MODE_PRIVATE).toSpProvider())

    var isFirstBoot by config.boolean("FIRST_BOOT",false)

    var userName by config.string("USER_NAME", "NO NAME")

    var userPassword by config.string("USER_PASSWORD","")
}