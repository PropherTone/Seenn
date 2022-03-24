package com.protone.database.room.converters

import android.net.Uri
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.json.toUri
import com.protone.database.room.entity.Music

class MusicTypeConverter{

    @TypeConverter
    fun stringToObject(value: String): Uri {
        return value.toUri()
    }

    @TypeConverter
    fun objectToString(uri: Uri): String {
        return uri.toJson()
    }
}