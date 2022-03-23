package com.protone.api

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ActivityLifecycleOwner : LifecycleOwner{

    private val lifecycle = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle {
        return lifecycle
    }

    init {
        lifecycle.currentState = Lifecycle.State.INITIALIZED
    }

    suspend fun <O> use(block : suspend (lifecycle : LifecycleOwner, start : () -> Unit) -> O) : O{
        return try {
            onCreate()

            block(this,this::onStart)
        }finally {
            withContext(NonCancellable){
                onDestroy()
            }
        }
    }

    private fun onCreate(){
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    private fun onStart(){
        lifecycle.currentState = Lifecycle.State.RESUMED
        lifecycle.currentState = Lifecycle.State.STARTED
    }

    private fun onDestroy(){
        lifecycle.currentState = Lifecycle.State.DESTROYED
    }

}