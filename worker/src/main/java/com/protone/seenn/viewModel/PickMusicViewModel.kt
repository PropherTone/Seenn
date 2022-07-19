package com.protone.seenn.viewModel

import androidx.lifecycle.ViewModel
import com.protone.api.entity.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class PickMusicViewModel : ViewModel() {

    companion object {
        const val BUCKET_NAME = "BUCKET"
        const val MODE = "MODE"

        const val ADD_BUCKET = "ADD"
        const val PICK_MUSIC = "PICK"
    }

    val data: MutableList<Music> = mutableListOf()

    suspend fun filterData(input: String) = withContext(Dispatchers.IO) {
        data.asFlow().filter {
            it.displayName?.contains(input, true) == true || it.album?.contains(
                input,
                true
            ) == true
        }.buffer().toList() as MutableList<Music>
    }
}