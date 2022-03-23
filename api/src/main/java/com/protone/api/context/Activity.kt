package com.protone.api.context

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.protone.api.R
import com.protone.api.TAG

fun Activity.renameMedia(name: String, uri: Uri, callBack: (Boolean) -> Unit) {
    onBackground {
        var changed: String? = null
        try {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
                ?.apply {
                    val dn = getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    while (moveToNext()) {
                        changed = getString(dn)
                    }
                    close()
                }
            if (changed == name) {
                callBack.invoke(true)
                return@onBackground
            }
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    checkUriPermission(uri) {
                        if (it) {
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
                        } else callBack.invoke(false)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    try {
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
                    } catch (e: RecoverableSecurityException) {
                        try {
                            startIntentSenderForResult(
                                e.userAction.actionIntent.intentSender,
                                10,
                                null,
                                0,
                                0,
                                0
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
                        } catch (e: IntentSender.SendIntentException) {
                            showErrorToast()
                        }
                    }
                }
                else -> {
                    contentResolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name) },
                        null,
                        null
                    )
                }
            }
        } catch (e: Exception) {
            callBack.invoke(false)
        } finally {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
                ?.apply {
                    val dn = getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    while (moveToNext()) {
                        changed = getString(dn)
                    }
                    close()
                    callBack.invoke(name == changed)
                }
        }
    }
}

fun Activity.deleteMedia(uri: Uri, callBack: (Boolean) -> Unit) {
    onBackground {
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    checkUriPermission(uri) {
                        if (it) {
                            contentResolver.delete(
                                uri,
                                null,
                                null
                            )
                        } else callBack.invoke(false)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    try {
                        contentResolver.delete(
                            uri,
                            null,
                            null
                        )
                    } catch (e: RecoverableSecurityException) {
                        try {
                            startIntentSenderForResult(
                                e.userAction.actionIntent.intentSender,
                                10,
                                null,
                                0,
                                0,
                                0
                            )
                            contentResolver.delete(
                                uri,
                                null,
                                null
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            showErrorToast()
                        }
                    }
                }
                else -> {
                    contentResolver.delete(
                        uri,
                        null,
                        null
                    )
                }
            }
        } catch (e: Exception) {
            callBack.invoke(false)
        } finally {
            callBack.invoke(true)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
fun Activity.checkUriPermission(uri: Uri, callBack: (Boolean) -> Unit) {
    try {
        val writeRequest = MediaStore.createWriteRequest(contentResolver, listOf(uri)).intentSender
        startIntentSenderForResult(writeRequest, 10, null, 0, 0, 0)
        callBack.invoke(true)
    } catch (e: IntentSender.SendIntentException) {
        showErrorToast()
        callBack.invoke(false)
    }
}

fun Activity.showErrorToast() = runOnUiThread {
    Toast.makeText(this, R.string.no_permission, Toast.LENGTH_SHORT).show()
}

fun Activity.showFailedToast() = runOnUiThread {
    Toast.makeText(
        this,
        R.string.failed_msg,
        Toast.LENGTH_SHORT
    ).show()
}