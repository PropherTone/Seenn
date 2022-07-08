package com.protone.seenn.activity

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import com.protone.api.context.*
import com.protone.seenn.broadcast.MusicReceiver
import com.protone.seenn.service.MusicService
import com.protone.seenn.viewModel.IntentDataHolder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseActivity<VB : ViewDataBinding, VM : ViewModel>(handleEven: Boolean) :
    AppCompatActivity(),
    CoroutineScope by MainScope() {
    protected abstract val viewModel: VM
    protected lateinit var binding: VB

    abstract fun initView()
    abstract suspend fun VM.init()
    abstract suspend fun onViewEvent(event: String)
    private var viewEvent: Channel<String>? = null
    private var viewEventTask: Job? = null
    protected var onFinish: (suspend () -> Unit)? = null
    protected var onResume: (suspend () -> Unit)? = null
    protected var onRestart: (suspend () -> Unit)? = null
    protected var onPause: (suspend () -> Unit)? = null
    protected var onStop: (suspend () -> Unit)? = null

    init {
        if (handleEven) {
            viewEvent = Channel(Channel.UNLIMITED)
            viewEventTask = launch(Dispatchers.Main) {
                while (isActive) {
                    select<Unit> {
                        viewEvent?.onReceive {
                            onViewEvent(it)
                        }
                    }
                }
            }
        }
    }

    val code = AtomicInteger(0)

    var musicReceiver: MusicReceiver? = null
        set(value) {
            value?.let { registerReceiver(it, musicIntentFilter) }
            field = value
        }

    private val activityOperationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTIVITY_FINISH -> finish()
                ACTIVITY_RESTART -> {
                    startActivity(SplashActivity::class.intent)
                }
            }
        }
    }

    private var serviceConnection: ServiceConnection? = null

    lateinit var binder: MusicService.MusicBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityOperationBroadcast.registerReceiver(
            activityOperationReceiver,
            IntentFilter(ACTIVITY_FINISH)
        )
        initView()
        setContentView(binding.root)
        launch {
            viewEventTask?.start()
            viewModel.init()
        }
    }


    fun <T> startActivityWithGainData(data: T, intent: Intent?) {
        IntentDataHolder.put(data)
        startActivity(intent)
    }

    inline fun <reified T> getGainData(): T? {
        return IntentDataHolder.get().let {
            if (it is T) {
                val re = it as T
                re
            } else null
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getGainListData(): List<T>? {
        return IntentDataHolder.get().let {
            if (it is List<*> && it.size > 0 && it[0] is T) {
                val list = it as List<T>
                list
            } else null
        }
    }

    suspend inline fun startActivityForResult(
        intent: Intent?,
    ) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<ActivityResult?> { co ->
            activityResultRegistry.register(
                code.incrementAndGet().toString(),
                ActivityResultContracts.StartActivityForResult(),
            ) {
                co.resumeWith(Result.success(it))
            }.launch(intent)
        }
    }

    fun sendViewEvent(event: String) {
        viewEvent?.trySend(event)
    }

    fun closeEvent() {
        viewEventTask?.cancel()
        viewEventTask = null
        viewEvent?.close()
        viewEvent = null
    }

    fun bindMusicService(block: () -> Unit) {
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                binder = (p1 as MusicService.MusicBinder)
                block()
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
            }

        }
        serviceConnection?.let {
            bindService(MusicService::class.intent, it, BIND_AUTO_CREATE)
        }
    }

    protected fun fitStatuesBar(root: View) {
        root.marginTop(statuesBarHeight)
    }

    protected fun fitNavigationBar(root: View) {
        if (hasNavigationBar) root.marginBottom(navigationBarHeight)
    }

    protected fun fitStatuesBarUsePadding(view: View) {
        view.paddingTop(statuesBarHeight)
    }

    protected fun fitNavigationBarUsePadding(view: View) {
        if (hasNavigationBar) view.paddingBottom(navigationBarHeight)
    }

    fun setTranslucentStatues() {
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        controller?.hide(WindowInsetsCompat.Type.statusBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
    }

    open suspend fun doStart() = Unit
    open suspend fun doResume() = Unit
    open suspend fun doRestart() = Unit
    open suspend fun doPause() = Unit
    open suspend fun doStop() = Unit
    open suspend fun doFinish() = Unit

    override fun onStart() {
        try {
            launch {
                doStart()
            }
        } finally {
            super.onStart()
        }
    }

    override fun onResume() {
        try {
            launch {
                onResume?.invoke()
                doResume()
            }
        } finally {
            super.onResume()
        }
    }

    override fun onRestart() {
        try {
            launch {
                onRestart?.invoke()
                doRestart()
            }
        } finally {
            super.onRestart()
        }
    }

    override fun onPause() {
        try {
            launch {
                onPause?.invoke()
                doPause()
            }
        } finally {
            super.onPause()
        }
    }

    override fun onStop() {
        try {
            launch {
                onStop?.invoke()
                doStop()
            }
        } finally {
            super.onStop()
        }
    }

    override fun finish() {
        try {
            launch {
                onFinish?.invoke()
                doFinish()
            }
        } finally {
            super.finish()
        }
    }

    override fun onDestroy() {
        try {
            cancel()
            activityOperationBroadcast.unregisterReceiver(activityOperationReceiver)
            serviceConnection?.let { unbindService(it) }
            musicReceiver?.let { unregisterReceiver(it) }
        } finally {
            super.onDestroy()
        }
    }

}