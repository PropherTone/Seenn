package com.protone.api.context

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.protone.api.R
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.isInDebug
import kotlinx.coroutines.delay

val activityOperationBroadcast: LocalBroadcastManager =
    LocalBroadcastManager.getInstance(SApplication.app)

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
): Boolean {
    return try {
        if (observeChange(uri, name)) {
            return true
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
        true
    } catch (e: Exception) {
        if (isInDebug()) e.printStackTrace()
        false
    }
}

fun Activity.funcForMultiRename(
    name: String,
    uri: Uri,
): String? {
    return try {
        if (observeChange(uri, name)) {
            return name
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
        name
    } catch (e: Exception) {
        null
    }
}

suspend fun Activity.deleteMedia(uri: Uri): Boolean {
    return try {
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
        delay(200)
        true
    } catch (e: Exception) {
        false
    }

}

fun Activity.multiDeleteMedia(uri: Uri): Boolean {
    return try {
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
        true
    } catch (e: Exception) {
        false
    }
}

fun Activity.showFailedToast() = runOnUiThread {
    R.string.failed_msg.getString().toast()
}