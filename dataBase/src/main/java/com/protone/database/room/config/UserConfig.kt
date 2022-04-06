package com.protone.database.room.config

import android.content.Context
import android.content.SharedPreferences
import com.protone.api.context.Global
import com.protone.database.R
import com.protone.database.sp.SpTool
import com.protone.database.sp.toSpProvider

class UserConfig(context: Context) {
    private val config =
        SpTool(context.getSharedPreferences("USER_CONFIG", Context.MODE_PRIVATE).toSpProvider())

    var isFirstBoot by config.boolean("FIRST_BOOT",false)

    var userName by config.string("USER_NAME", "NO NAME")

    var userPassword by config.string("USER_PASSWORD","")

    var playedMusicBucket by config.string("PLAYED_MUSIC_BUCKET", Global.application.getString(R.string.all_music))

    var playedMusicPosition by config.int("MUSIC_POSITION",0)
}