package com.protone.database.room.converters

import androidx.room.TypeConverter

@Suppress("RedundantNullableReturnType")
class ListTypeConverter {

    @TypeConverter
    fun stringToObject(value: String?): List<String>? {
        val mutableList = mutableListOf<String>()
        value?.split("|")?.forEach { mutableList.add(it) }
        return mutableList
    }

    @TypeConverter
    fun objectToString(list: List<String>?): String? {
        val sb = StringBuilder()
        list?.stream()?.forEach { sb.append("$it|") }
        return sb.toString()
    }
}