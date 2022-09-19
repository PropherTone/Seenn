package com.protone.worker.viewModel

import androidx.lifecycle.viewModelScope
import com.protone.api.entity.GalleyMedia
import com.protone.worker.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*

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
        viewModelScope.launch(Dispatchers.Default) {
            val lowercase = input.lowercase(Locale.getDefault())
            val uppercase = input.uppercase(Locale.getDefault())
            launch(Dispatchers.Default) {
                data.filter {
                    it.name.contains(input, true)
                }.let { nameFilterList ->
                    onQueryListener?.onGalleyResult(nameFilterList as MutableList<GalleyMedia>)
                    cancel()
                }
            }
            launch(Dispatchers.Default) {
                data.filter {
                    it.cate?.any { name -> name.contains(input, true) } == true
                }.let { catoFilterList ->
                    onQueryListener?.onCatoResult(catoFilterList as MutableList<GalleyMedia>)
                    cancel()
                }
            }
            launch(Dispatchers.Default) {
                data.filter {
                    val notesWithGalley =
                        DatabaseHelper
                            .instance
                            .galleriesWithNotesDAOBridge.getNotesWithGalley(it.uri)
                            .map { note -> note.title }
                    notesWithGalley.any { name -> name.contains(input, true) }
                }.let { noteFilterList ->
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