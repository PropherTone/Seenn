package com.protone.seenn

import com.protone.seen.NoteEditSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class NoteEditActivity : BaseActivity<NoteEditSeen>() {
    override suspend fun main() {
        val noteEditSeen = NoteEditSeen(this)

        while (isActive){
            select<Unit> {
                event.onReceive{

                }
                noteEditSeen.viewEvent.onReceive{

                }
            }
        }
    }
}