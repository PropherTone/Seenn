package com.protone.seenn.viewModel

import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.protone.api.context.SApplication
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.seenn.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UserConfigViewModel : ViewModel() {

    enum class ViewEvent {
        Login,
        Icon,
        Name,
        PassWord,
        ShareNote,
        ShareData,
        Lock,
        Unlock,
        Refresh,
        ClearCache,
        Log
    }

    enum class DisplayMode {
        UnRegis,
        Locked,
        Normal
    }

    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                Glide.get(SApplication.app).apply {
                    clearDiskCache()
                    withContext(Dispatchers.Main) {
                        clearMemory()
                    }
                }
                val file = File("${SApplication.app.externalCacheDir?.path}/CrashLog")
                if (file.exists() && file.isDirectory) {
                    file.listFiles()?.forEach {
                        it.delete()
                    }
                }
                R.string.success.getString().toast()
            } catch (e: Exception) {
                R.string.none.getString().toast()
            }
        }
    }

}