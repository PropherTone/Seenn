package com.protone.worker.viewModel

import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.protone.api.SCrashHandler
import com.protone.api.baseType.getParentPath
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.SApplication
import com.protone.worker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UserConfigViewModel : BaseViewModel() {

    sealed class UserConfigEvent {
        object Login : ViewEvent
        object Icon : ViewEvent
        object Name : ViewEvent
        object PassWord : ViewEvent
        object Share : ViewEvent
        object Lock : ViewEvent
        object Unlock : ViewEvent
        object Refresh : ViewEvent
        object ClearCache : ViewEvent
        object Log : ViewEvent
        object CombineGalley : ViewEvent
        object DispatchGalley : ViewEvent
    }

    enum class DisplayMode {
        UnRegis,
        Locked,
        Normal,
        CombineGalley
    }

    private var onClear = false

    fun deleteOldIcon(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
    }

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