package com.protone.seenn

import com.protone.seen.NoteListSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class NoteListActivity : BaseActivity<NoteListSeen>() {
    override suspend fun main() {
        val noteListSeen = NoteListSeen(this)
        while (isActive){
            select<Unit> {
                event.onReceive{

                }
                noteListSeen.viewEvent.onReceive{

                }
            }
        }
    }
}