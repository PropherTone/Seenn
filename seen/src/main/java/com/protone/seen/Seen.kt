package com.protone.seen

import android.content.Context
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel

abstract class Seen<C>(val context: Context) : CoroutineScope by CoroutineScope(Dispatchers.Unconfined){

    abstract val viewRoot : View

    val viewEvent = Channel<C>(Channel.UNLIMITED)
}