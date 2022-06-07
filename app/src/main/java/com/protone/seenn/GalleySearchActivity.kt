package com.protone.seenn

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import com.protone.api.context.intent
import com.protone.api.json.toJson
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.GalleySearchSeen
import com.protone.seen.adapter.GalleyListAdapter
import com.protone.seenn.viewModel.IntentDataHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class GalleySearchActivity : BaseActivity<GalleySearchSeen>(), TextWatcher,
    GalleyListAdapter.OnSelect {

    private lateinit var timerHandler: Handler

    private val delayed = 500L

    private var input = ""

    private val data = mutableListOf<GalleyMedia>()

    @Suppress("UNCHECKED_CAST")
    override suspend fun main() {
        val galleySearchSeen = GalleySearchSeen(this)
        setContentSeen(galleySearchSeen)
        timerHandler = Handler(Looper.getMainLooper()) {
            if (it.what == 0) {
                galleySearchSeen.offer(GalleySearchSeen.SearchEvent.Query)
            }
            false
        }
        IntentDataHolder.get().let {
            if (!(it != null && it is List<*> && it.isNotEmpty() && it[0] is GalleyMedia)) {
                toast(getString(R.string.none))
                finish()
            }
            data.addAll(it as List<GalleyMedia>)
            galleySearchSeen.initList(data[0].isVideo, this)
        }
        galleySearchSeen.queryText.addTextChangedListener(this)
        while (isActive) {
            select<Unit> {
                galleySearchSeen.viewEvent.onReceive {
                    when (it) {
                        GalleySearchSeen.SearchEvent.Query -> galleySearchSeen.query(input)
                        GalleySearchSeen.SearchEvent.Finish -> finish()
                    }
                }
            }
        }
    }

    private suspend fun GalleySearchSeen.query(input: String) = withContext(Dispatchers.IO) {
        val nameFilter = async {
            data.asFlow().filter {
                it.name.contains(input)
            }.buffer().toList()
        }
        val catoFilter = async {
            data.asFlow().filter {
                it.cate?.contains(input) == true
            }.buffer().toList()
        }
        val noteFilter = async {
            data.asFlow().filter {
                it.notes?.contains(input) == true
            }.buffer().toList()
        }
        val nameFilterList = nameFilter.await()
        val catoFilterList = catoFilter.await()
        val noteFilterList = noteFilter.await()
        refreshGalleyList(nameFilterList as MutableList<GalleyMedia>)
        refreshCatoList(catoFilterList as MutableList<GalleyMedia>)
        refreshNoteList(noteFilterList as MutableList<GalleyMedia>)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        timerHandler.removeCallbacksAndMessages(null)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        timerHandler.removeCallbacksAndMessages(null)
    }

    override fun afterTextChanged(s: Editable?) {
        input = s.toString()
        timerHandler.sendEmptyMessageDelayed(0, delayed)
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