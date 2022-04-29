package com.protone.seenn

import com.protone.mediamodle.Galley
import com.protone.seen.UserConfigSeen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class UserConfigActivity : BaseActivity<UserConfigSeen>() {

    override suspend fun main() {
        val userConfigSeen = UserConfigSeen(this)
        setContentSeen(userConfigSeen)

        userConfigSeen.initList(Galley.music)

        while (isActive) {
            select<Unit> {
                event.onReceive {}
                userConfigSeen.viewEvent.onReceive {}
            }
        }
    }
}