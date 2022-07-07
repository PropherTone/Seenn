package com.protone.api.context

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.protone.api.R
import com.protone.api.getString
import com.protone.api.isInDebug
import com.protone.api.toast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

val activityOperationBroadcast: LocalBroadcastManager = LocalBroadcastManager.getInstance(APP.app)

fun Activity.observeChange(uri: Uri, targetName: String): Boolean {
    var name = ""
    contentResolver.query(
        uri,
        arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.also {
        val dn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        while (it.moveToNext()) {
            name = it.getString(dn)
        }
        it.close()
    }
    return name == targetName
}

fun Activity.renameMedia(
    name: String,
    uri: Uri,
    scope: CoroutineScope,
    callBack: (Boolean) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        var observer: ContentObserver? = null
        observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                callBack.invoke(observeChange(uri, name))
                observer?.let { contentResolver.unregisterContentObserver(it) }
            }
        }
        contentResolver.registerContentObserver(uri, true, observer)
        flow {
            try {
                if (observeChange(uri, name)) {
                    emit(true)
                }
                grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                contentResolver.update(
                    uri,
                    ContentValues().apply {
                        put(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            name
                        )
                    },
                    null,
                    null
                )
                cancel()
            } catch (e: Exception) {
                if (isInDebug()) e.printStackTrace()
                emit(false)
            }
        }.collect {
            withContext(Dispatchers.Main) {
                callBack.invoke(it)
            }
            cancel()
        }
    }
}

inline fun Activity.funcForMultiRename(
    name: String,
    uri: Uri,
    callBack: (String?) -> Unit
) {
    try {
        if (observeChange(uri, name)) {
            callBack.invoke(name)
            return
        }
        grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        contentResolver.update(
            uri,
            ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    name
                )
            },
            null,
            null
        )
        callBack.invoke(name)
    } catch (e: Exception) {
        callBack.invoke(null)
    }
}

fun Activity.deleteMedia(
    uri: Uri,
    scope: CoroutineScope,
    callBack: (Boolean) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        flow {
            try {
                grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                contentResolver.delete(
                    uri,
                    null,
                    null
                )
                emit(true)
            } catch (e: Exception) {
                emit(false)
            }
        }.collect {
            withContext(Dispatchers.Main) {
                callBack.invoke(it)
            }
            cancel()
        }
    }
}

fun Activity.multiDeleteMedia(uris: List<Uri>, scope: CoroutineScope, callBack: (Boolean) -> Unit) {
    scope.launch(Dispatchers.IO) {
        uris.asFlow().map {
            try {
                grantUriPermission(
                    packageName,
                    it,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                contentResolver.delete(
                    it,
                    null,
                    null
                )
                true
            } catch (e: Exception) {
                false
            }
        }.collect {
            withContext(Dispatchers.Main) {
                callBack.invoke(it)
            }
            cancel()
        }
    }
}

fun Activity.showFailedToast() = runOnUiThread {
    R.string.failed_msg.getString().toast()
}