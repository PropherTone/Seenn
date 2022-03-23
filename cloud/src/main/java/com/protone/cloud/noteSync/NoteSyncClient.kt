package com.protone.cloud.noteSync

import android.util.Log
import com.protone.api.json.toEntity
import com.protone.database.room.entity.Note
import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

class NoteSyncClient : CoroutineScope by CoroutineScope(Dispatchers.IO) , BaseSync<Note>(){

    fun connect(hostName: String, port: Int,cloudStates: CloudStates<Note>?) {
        launch(Dispatchers.IO) {
            try {
                Log.d("TAG", "connect: ")
                mSocket = Socket()
                onSyncListener = cloudStates
                val inetSocketAddress : SocketAddress = InetSocketAddress(hostName, port)
                mSocket?.connect(inetSocketAddress,5000)
                if (mSocket?.isConnected == true) {
                    onSyncListener?.success()
                } else {
                    mSocket?.close()
                    onSyncListener?.failed(FAILED_MSG)
                }
            } catch (ignored: IOException) {
                onSyncListener?.failed(FAILED_MSG)
            }
        }
    }

    fun startReceiveFrequency() {
        launch(Dispatchers.IO) {
            try {
                mSocket?.apply {
                    if (isConnected) {
                        val inputStream = DataInputStream(getInputStream())
                        var msg : String
                        while (isConnected) {
                            msg = inputStream.readUTF()
                            when (msg) {
                                SUCCESS_MSG -> onSyncListener?.success()
                                else -> {
                                    if (msg.isNotEmpty()) {
                                        onSyncListener?.successMsg(msg.toEntity(Note::class.java))
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                onSyncListener?.failed(e.message.toString())
            }finally {
                closeSocket()
            }
        }
    }
}