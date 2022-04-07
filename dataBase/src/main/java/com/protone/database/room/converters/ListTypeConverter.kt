package com.protone.database.room.converters

import android.util.Log
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.protone.api.TAG
import com.protone.api.json.jsonToList
import com.protone.api.json.listToJson
import com.protone.api.json.toJson

class ListTypeConverter {

    @TypeConverter
    fun stringToObject(value: String?): List<String>? {
        Log.d(TAG, "stringToObject: $value")
        return Gson().fromJson<List<String>>(value, object : TypeToken<List<String>>() {}.type)
    }

    @TypeConverter
    fun objectToString(list: List<String>?): String? {
        return Gson().toJson(list)
    }
}