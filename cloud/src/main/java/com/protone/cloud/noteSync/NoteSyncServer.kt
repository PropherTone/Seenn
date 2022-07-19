package com.protone.cloud.noteSync

import com.protone.api.json.toJson
import com.protone.api.entity.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.*

open class NoteSyncServer : CoroutineScope by CoroutineScope(Dispatchers.IO), BaseSync<String>() {

    var serverSocket : ServerSocket? = null

    fun connect(port: Int, cloudStates: CloudStates<String>?) {
        launch(Dispatchers.IO) {
            try {
                onSyncListener = cloudStates
                serverSocket = ServerSocket(port).apply {
                    mSocket = accept()
                    onSyncListener?.success()
                }
            } catch (e: IOException) {
                onSyncListener?.failed(e.message.toString())
                serverSocket?.close()
                serverSocket = null
            }
        }
    }

    fun startSendFrequency(notes: List<Note>) {
        launch(Dispatchers.IO) {
            try {
                mSocket?.apply {
                    if (isConnected) {
                        val dataOutputStream = DataOutputStream(getOutputStream())
                        for (note in notes) {
                            dataOutputStream.writeUTF(note.toJson())
                            onSyncListener?.successMsg(note.getTitle())
                        }
                    }
                }
            } catch (e: IOException) {
                onSyncListener?.failed(e.message.toString())
            } finally {
                closeSocket()
            }
        }

    }

}