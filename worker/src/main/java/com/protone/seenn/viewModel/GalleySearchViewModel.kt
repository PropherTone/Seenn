package com.protone.seenn.viewModel

import androidx.lifecycle.viewModelScope
import com.protone.api.entity.GalleyMedia
import com.protone.seenn.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.streams.toList

class GalleySearchViewModel : BaseViewModel() {

    val data = mutableListOf<GalleyMedia>()

    var onQueryListener: OnQuery? = null

    var selectList: MutableList<GalleyMedia> = mutableListOf()

    fun isVideo(): Boolean {
        return if (data.size > 0) {
            data[0].isVideo
        } else {
            false
        }
    }

    fun query(input: String) {
        if (input.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val lowercase = input.lowercase(Locale.getDefault())
            val uppercase = input.uppercase(Locale.getDefault())
            launch(Dispatchers.IO) {
                data.stream().filter {
                    it.name.contains(input, true)
                }.toList().let { nameFilterList ->
                    onQueryListener?.onGalleyResult(nameFilterList as MutableList<GalleyMedia>)
                    cancel()
                }
            }
            launch(Dispatchers.IO) {
                data.stream().filter {
                    it.cate?.contains(input) == true
                            || it.cate?.contains(lowercase) == true
                            || it.cate?.contains(uppercase) == true
                            || it.cate?.stream()
                        ?.anyMatch { name -> name.contains(input, true) } == true
                }.toList().let { catoFilterList ->
                    onQueryListener?.onCatoResult(catoFilterList as MutableList<GalleyMedia>)
                    cancel()
                }
            }
            launch(Dispatchers.IO) {
                data.stream().filter {
                    val notesWithGalley =
                        DatabaseHelper.instance
                            .galleriesWithNotesDAOBridge.getNotesWithGalley(it.uri)
                            .stream().map { note ->
                                note.title
                            }.toList()
                    notesWithGalley.contains(input)
                            || notesWithGalley.contains(lowercase)
                            || notesWithGalley.contains(uppercase)
                            || notesWithGalley.stream()
                        .anyMatch { name -> name.contains(input, true) }
                }.toList().let { noteFilterList ->
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