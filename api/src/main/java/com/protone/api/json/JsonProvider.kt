package com.protone.api.json

import android.net.Uri
import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.protone.api.TAG
import java.lang.reflect.Type
import kotlin.reflect.KClass

fun Any.toJson(): String {
    return GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriSerializer())
        .create()
        .toJson(this)
}

fun <C> String.toEntity(clazz: Class<C>): C {
    return GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriDeserializer())
        .create()
        .fromJson(this, clazz)
}

fun String.toUri(): Uri{
    return Uri.parse(this)
}

fun <T> Gson.jsonToList(json: String) : List<T> =
    this.fromJson(json, object : TypeToken<List<T>>() {}.type)

private class UriSerializer : JsonSerializer<Uri> {
    override fun serialize(
        src: Uri?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.path)
    }
}

private class UriDeserializer : JsonDeserializer<Uri> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Uri {
        return Uri.parse(json?.asString)
    }
}
