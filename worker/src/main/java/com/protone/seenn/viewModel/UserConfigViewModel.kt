package com.protone.seenn.viewModel

import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.protone.api.SCrashHandler
import com.protone.api.baseType.getParentPath
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.SApplication
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

    private var onClear = false

    suspend fun clearCache() {
        if (onClear) return
        onClear = true
        withContext(Dispatchers.IO) {
            try {
                Glide.get(SApplication.app).apply {
                    clearDiskCache()
                    withContext(Dispatchers.Main) {
                        clearMemory()
                    }
                }
                SCrashHandler.path?.getParentPath()?.let { path ->
                    val file = File(path)
                    if (file.exists() && file.isDirectory) {
                        file.listFiles()?.forEach {
                            it.delete()
                        }
                    }
                }
                R.string.success.getString().toast()
            } catch (e: Exception) {
                R.string.none.getString().toast()
            }
            onClear = false
        }
    }

}