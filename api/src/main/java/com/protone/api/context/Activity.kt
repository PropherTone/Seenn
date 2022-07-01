package com.protone.api.context

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.protone.api.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

val activityOperationBroadcast : LocalBroadcastManager = LocalBroadcastManager.getInstance(APP.app)

private fun Activity.observeChange(uri: Uri, targetName: String): Boolean {
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

fun Activity.showFailedToast() = runOnUiThread {
    Toast.makeText(
        this,
        R.string.failed_msg,
        Toast.LENGTH_SHORT
    ).show()
}