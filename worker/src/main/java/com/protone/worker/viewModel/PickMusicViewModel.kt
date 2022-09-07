package com.protone.worker.viewModel

import com.protone.api.entity.Music
import com.protone.worker.database.DatabaseHelper
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList

class PickMusicViewModel : BaseViewModel() {

    companion object {
        const val BUCKET_NAME = "BUCKET"
        const val MODE = "MODE"

        const val ADD_BUCKET = "ADD"
        const val PICK_MUSIC = "PICK"
    }

    val data: MutableList<Music> = mutableListOf()

    suspend fun getMusics() = DatabaseHelper.instance.musicDAOBridge.getAllMusicRs() as MutableList<Music>

    suspend fun filterData(input: String) = data.asFlow().filter {
        it.displayName?.contains(input, true) == true || it.album?.contains(
            input,
            true
        ) == true
    }.buffer().toList() as MutableList<Music>

}