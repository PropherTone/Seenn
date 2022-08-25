package com.protone.worker.viewModel

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import com.protone.api.TAG
import com.protone.api.entity.GalleyMedia
import com.protone.worker.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TestViewModel : BaseViewModel() {

    fun sqlTest() {
        viewModelScope.launch(Dispatchers.IO) {
            val galleyMedia = GalleyMedia(
                Uri.parse("123123"),
                "123123.jpg",
                "/asd/asd/123123.jpg",
                "asd",
                3111,
                null,
                null,
                41313,
                Uri.EMPTY,
                0,
                false
            )
            log(DatabaseHelper.instance.signedGalleyDAOBridge.insertSignedMedia(galleyMedia).toString())
        }
    }

    fun sqlTest2(){
        viewModelScope.launch(Dispatchers.IO) {
            val galleyMedia2 = GalleyMedia(
                Uri.parse("12311123"),
                "123123.jpg",
                "/asd/assd/123123.jpg",
                "asd",
                3111,
                null,
                null,
                41313,
                Uri.EMPTY,
                0,
                false
            )
            log(DatabaseHelper.instance.signedGalleyDAOBridge.insertSignedMedia(galleyMedia2).toString())
        }
    }

    fun sqlTest3() {
        viewModelScope.launch(Dispatchers.IO) {
            val galleyMedia = GalleyMedia(
                Uri.parse("123123"),
                "123aa123.jpg",
                "/asd/asd/123123.jpg",
                "asd",
                3111,
                null,
                null,
                41313,
                Uri.EMPTY,
                0,
                false
            )
           log(DatabaseHelper.instance.signedGalleyDAOBridge.insertSignedMedia(galleyMedia).toString())
        }
    }

    fun sqlTest4(){
        viewModelScope.launch(Dispatchers.IO) {
            val galleyMedia2 = GalleyMedia(
                Uri.parse("12311123"),
                "1231bb23.jpg",
                "/asd/assd/123123.jpg",
                "asd",
                3111,
                null,
                null,
                41313,
                Uri.EMPTY,
                0,
                false
            )
            log(DatabaseHelper.instance.signedGalleyDAOBridge.insertSignedMedia(galleyMedia2).toString())
        }
    }

    @SuppressLint("StaticFieldLeak")
    private lateinit var logText: TextView
    val stringBuilder = StringBuilder()

    fun setLogView(textView: TextView) {
        logText = textView
    }

    fun log(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            stringBuilder.append("$msg\n")
            logText.text = stringBuilder.toString()
            Log.d(TAG, msg)
        }
    }

    private suspend fun runningTime(name: String, block: suspend () -> Unit) {
        val start = System.currentTimeMillis()
        try {
            log("$name start running")
            block.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        log("$name running time: ${System.currentTimeMillis() - start}\n")
    }

    private fun runningTimeNoSuspend(block: () -> Unit) {
        val start = System.currentTimeMillis()
        try {
            log("function start running")
            block.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        log("function running time: ${System.currentTimeMillis() - start}\n")
    }
}