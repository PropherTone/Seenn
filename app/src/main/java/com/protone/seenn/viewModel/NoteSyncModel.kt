package com.protone.seenn.viewModel

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import com.protone.cloud.service.NoteSyncService

class NoteSyncModel : ViewModel(){

    var syncBinder : NoteSyncService.SyncBinder? = null

    val conn = object  : ServiceConnection{
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            syncBinder = p1 as NoteSyncService.SyncBinder
        }

        override fun onServiceDisconnected(p0: ComponentName?) { }

    }


}