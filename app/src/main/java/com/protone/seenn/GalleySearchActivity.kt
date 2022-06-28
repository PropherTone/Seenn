package com.protone.seenn

import com.protone.api.SearchModel
import com.protone.api.context.intent
import com.protone.api.json.toJson
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.GalleySearchSeen
import com.protone.seen.adapter.GalleyListAdapter
import com.protone.seenn.viewModel.IntentDataHolder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.selects.select
import java.util.*

class GalleySearchActivity : BaseActivity<GalleySearchSeen>(),
    GalleyListAdapter.OnSelect {

    private val data = mutableListOf<GalleyMedia>()

    @Suppress("UNCHECKED_CAST")
    override suspend fun main() {
        val galleySearchSeen = GalleySearchSeen(this)
        setContentSeen(galleySearchSeen)
        val searchModel = SearchModel(galleySearchSeen.queryText) {
            galleySearchSeen.offer(GalleySearchSeen.SearchEvent.Query)
        }
        IntentDataHolder.get().let {
            if (!(it != null && it is List<*> && it.isNotEmpty() && it[0] is GalleyMedia)) {
                toast(getString(R.string.none))
            }else{
                data.addAll(it as List<GalleyMedia>)
                galleySearchSeen.initList(data[0].isVideo, this)
            }
        }
        while (isActive) {
            select<Unit> {
                galleySearchSeen.viewEvent.onReceive {
                    when (it) {
                        GalleySearchSeen.SearchEvent.Query -> galleySearchSeen.query(searchModel.getInput())
                        GalleySearchSeen.SearchEvent.Finish -> finish()
                    }
                }
            }
        }
    }

    private suspend fun GalleySearchSeen.query(input: String) {
        if (input.isEmpty()) return
        withContext(Dispatchers.IO) {
            val lowercase = input.lowercase(Locale.getDefault())
            launch(Dispatchers.IO) {
                data.asFlow().filter {
                    it.name.contains(input, true)
                }.buffer().toList().let { nameFilterList ->
                    refreshGalleyList(nameFilterList as MutableList<GalleyMedia>)
                    cancel()
                }
            }
            launch(Dispatchers.IO) {
                data.asFlow().filter {
                    it.cate?.contains(input) == true || it.cate?.contains(lowercase) == true
                }.buffer().toList().let { catoFilterList ->
                    refreshCatoList(catoFilterList as MutableList<GalleyMedia>)
                    cancel()
                }
            }
            launch(Dispatchers.IO) {
                data.asFlow().filter {
                    it.notes?.contains(input) == true || it.cate?.contains(lowercase) == true
                }.buffer().toList().let { noteFilterList ->
                    refreshNoteList(noteFilterList as MutableList<GalleyMedia>)
                    cancel()
                }
            }
        }
    }

    override fun select(galleyMedia: MutableList<GalleyMedia>) {}

    override fun openView(galleyMedia: GalleyMedia) {
        startActivity(GalleyViewActivity::class.intent.apply {
            putExtra(GalleyViewActivity.MEDIA, galleyMedia.toJson())
            putExtra(GalleyViewActivity.TYPE, galleyMedia.isVideo)
            putExtra(
                GalleyViewActivity.GALLEY,
                intent.getStringExtra("GALLEY") ?: getString(R.string.all_galley)
            )
        })
    }
}