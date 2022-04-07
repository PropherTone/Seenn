package com.protone.api.json

import android.net.Uri
import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.protone.api.TAG
import com.protone.api.imageContent
import com.protone.api.musicContent
import com.protone.api.videoContent
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

fun Any.toJson(): String {
    return GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriSerializer())
        .create()
        .toJson(this,this::class.java)
}

fun Uri.toJson():String{
    return this.toString()
}

fun <C> String.toEntity(clazz: Class<C>?): C {
    return GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriDeserializer())
        .create()
        .fromJson(this, clazz)
}

fun String.toUri(): Uri {
    return Uri.parse(this)
}

fun asdasd(){
    val lls = arrayListOf("asdasd","asdasd","344r3efwfed")
    val toJson = Gson().toJson(lls)
    Log.d(TAG, "asdasd: $toJson")
    val fromJson = Gson().fromJson<List<String>>(toJson, object : TypeToken<List<String>>() {}.type)
    Log.d(TAG, "asdasd: from  $fromJson")
}

fun <T> List<*>.listToJson(clazz: Class<T>):String{
    return GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriDeserializer())
        .create()
        .toJson(this, ParameterizedTypeImp(List::class.java, arrayOf(clazz)))
}

fun <T> String.jsonToList(clazz: Class<T>): List<T> {
    Log.d(TAG, "jsonToList: $this")
    return GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriDeserializer())
        .create()
        .fromJson(this, ParameterizedTypeImp(List::class.java, arrayOf(clazz)))
}

private class UriSerializer : JsonSerializer<Uri> {
    override fun serialize(
        src: Uri?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        Log.d("TAG", "serialize: ${src.toString()}")
        return JsonPrimitive(src?.toString())
    }
}

private class UriDeserializer : JsonDeserializer<Uri?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Uri? {
        return json?.asString?.let { uri ->
            if (uri.contains("content://")) {
                when (uri.contains("content://")) {
                    uri.contains("images") -> {
                        uri.split("/").let {
                            Uri.withAppendedPath(imageContent, it[it.size - 1])
                        }
                    }
                    uri.contains("videos") -> {
                        uri.split("/").let {
                            Uri.withAppendedPath(videoContent, it[it.size - 1])
                        }
                    }
                    uri.contains("music") -> {
                        uri.split("/").let {
                            Uri.withAppendedPath(musicContent, it[it.size - 1])
                        }
                    }
                    else -> uri.toUri()
                }
            } else {
                uri.toUri()
            }
        }
    }
}
