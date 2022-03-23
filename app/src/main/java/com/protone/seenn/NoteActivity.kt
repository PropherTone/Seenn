package com.protone.seenn

import com.protone.seen.NoteSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class NoteActivity : BaseActivity<NoteSeen>() {
    override suspend fun main() {
        val noteSeen = NoteSeen(this)
        while (isActive){
            select<Unit> {
                event.onReceive{

                }
                noteSeen.viewEvent.onReceive{

                }
            }
        }
    }
}