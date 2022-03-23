package com.protone.api.json

import android.net.Uri
import android.util.Log
import com.google.gson.*
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
        Log.d(TAG, "deserialize: ${json?.asJsonObject}")
        return Uri.parse("json?.asString")
    }
}
