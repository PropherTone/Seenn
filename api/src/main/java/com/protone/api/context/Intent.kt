package com.protone.api.context

import android.content.Intent
import android.content.IntentFilter
import kotlin.reflect.KClass

const val MUSIC_PLAY = "ControlMusic"
const val MUSIC_PAUSE = "PauseMusic"
const val MUSIC_NEXT = "NextMusic"
const val MUSIC_PREVIOUS = "PreviousMusic"
const val MUSIC_FINISH = "FinishMusic"

const val FINISH = "FINISH"
const val MUSIC = "MUSIC"

const val UPDATE_MUSIC_BUCKET = "UPDATE_MUSIC_BUCKET"
const val UPDATE_MUSIC = "UPDATE_MUSIC"
const val UPDATE_PHOTO = "UPDATE_PHOTO"
const val UPDATE_VIDEO = "UPDATE_VIDEO"

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

val workIntentFilter: IntentFilter
    get() {
        return IntentFilter().apply {
            addAction(UPDATE_MUSIC_BUCKET)
            addAction(UPDATE_MUSIC)
            addAction(UPDATE_PHOTO)
            addAction(UPDATE_VIDEO)
        }
    }

val appIntentFilter: IntentFilter
    get() {
        return IntentFilter().apply {
            addAction(FINISH)
            addAction(MUSIC)
        }
    }

val KClass<*>.intent
    get() = Intent(Global.application, this.java)