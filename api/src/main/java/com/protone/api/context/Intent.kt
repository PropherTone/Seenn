package com.protone.api.context

import android.content.Intent
import android.content.IntentFilter
import kotlin.reflect.KClass

const val MUSIC_PLAY = "PlayMusic"
const val MUSIC_PAUSE = "PauseMusic"
const val MUSIC_NEXT = "NextMusic"
const val MUSIC_PREVIOUS = "PreviousMusic"
const val MUSIC_FINISH = "FinishMusic"

val musicIntentFilter: IntentFilter
    get() {
        return IntentFilter().apply {
            addAction(MUSIC_PLAY)
            addAction(MUSIC_PAUSE)
            addAction(MUSIC_FINISH)
            addAction(MUSIC_NEXT)
            addAction(MUSIC_PREVIOUS)
        }
    }

val KClass<*>.intent
    get() = Intent(Global.application, this.java)