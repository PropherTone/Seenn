package com.protone.api.converters

import android.net.Uri
import androidx.room.TypeConverter
import com.protone.api.json.toUri
import com.protone.api.json.toUriJson

class UriTypeConverter{

    @TypeConverter
    fun stringToObject(value: String): Uri {
        return value.toUri()
    }

    @TypeConverter
    fun objectToString(uri: Uri): String {
        return uri.toUriJson()
    }
}