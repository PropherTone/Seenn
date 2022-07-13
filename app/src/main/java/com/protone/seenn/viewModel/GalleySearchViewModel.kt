package com.protone.seenn.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.util.*
import kotlin.streams.toList

class GalleySearchViewModel : ViewModel() {

    val data = mutableListOf<GalleyMedia>()

    var onQueryListener: OnQuery? = null

    var selectList: MutableList<GalleyMedia> = mutableListOf()

    fun query(input: String) {
        if (input.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val lowercase = input.lowercase(Locale.getDefault())
            val uppercase = input.uppercase(Locale.getDefault())
            launch(Dispatchers.IO) {
                data.asFlow().filter {
                    it.name.contains(input, true)
                }.buffer().toList().let { nameFilterList ->
                    onQueryListener?.onGalleyResult(nameFilterList as MutableList<GalleyMedia>)
                    cancel()
                }
            }
            launch(Dispatchers.IO) {
                data.asFlow().filter {
                    it.cate?.contains(input) == true
                            || it.cate?.contains(lowercase) == true
                            || it.cate?.contains(uppercase) == true
                            || it.cate?.stream()
                        ?.anyMatch { name -> name.contains(input, true) } == true
                }.buffer().toList().let { catoFilterList ->
                    onQueryListener?.onCatoResult(catoFilterList as MutableList<GalleyMedia>)
                    cancel()
                }
            }
            launch(Dispatchers.IO) {
                data.asFlow().filter {
                    val notesWithGalley =
                        DataBaseDAOHelper.getNotesWithGalley(it.mediaId ?: 0).stream().map { note ->
                            note.title
                        }.toList()
                    notesWithGalley.contains(input)
                            || notesWithGalley.contains(lowercase)
                            || notesWithGalley.contains(uppercase)
                            || notesWithGalley.stream()
                        .anyMatch { name -> name.contains(input, true) }
                }.buffer().toList().let { noteFilterList ->
                    onQueryListener?.onNoteResult(noteFilterList as MutableList<GalleyMedia>)
                    cancel()
                }
            }
        }
    }

    interface OnQuery {
        fun onGalleyResult(list: MutableList<GalleyMedia>)
        fun onCatoResult(list: MutableList<GalleyMedia>)
        fun onNoteResult(list: MutableList<GalleyMedia>)
    }
}