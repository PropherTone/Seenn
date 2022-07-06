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
import com.protone.seenn.SplashActivity
import com.protone.seenn.broadcast.MusicReceiver
import com.protone.seenn.service.MusicService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseActivity<VB : ViewDataBinding, VM : ViewModel>(handleEven: Boolean) :
    AppCompatActivity(),
    CoroutineScope by MainScope() {
    protected abstract val viewModel: VM
    protected lateinit var binding: VB

    abstract suspend fun initView()
    abstract suspend fun VM.init()
    abstract suspend fun onViewEvent(event: String)
    private var viewEvent: Channel<String>? = null
    private var viewEventTask: Job? = null
    protected var onFinish: (suspend () -> Unit)? = null

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

    protected fun fitStatuesBar(root: View) {
        root.marginTop(statuesBarHeight)
    }

    protected fun fitNavigationBar(root: View) {
        if (hasNavigationBar) root.marginBottom(navigationBarHeight)
    }

    fun fitStatuesBarUsePadding(view: View) {
        view.paddingTop(statuesBarHeight)
    }

    fun fitNavigationBarUsePadding(view: View) {
        if (hasNavigationBar) view.paddingBottom(navigationBarHeight)
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
        runBlocking {
            initView()
            if (::binding.isInitialized) setContentView(binding.root)
            viewEventTask?.start()
            viewModel.init()
        }
    }

    override fun finish() {
        try {
            launch { onFinish?.invoke() }
        } finally {
            super.finish()
        }
    }

    override fun onDestroy() {
        cancel()
        activityOperationBroadcast.unregisterReceiver(activityOperationReceiver)
        serviceConnection?.let { unbindService(it) }
        musicReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }

    fun closeEvent() {
        viewEventTask?.cancel()
        viewEventTask = null
        viewEvent?.close()
        viewEvent = null
    }

    fun sendViewEvent(event: String) {
        viewEvent?.trySend(event)
    }

    inline fun startActivityForResult(
        intent: Intent?,
        crossinline callback: (ActivityResult?) -> Unit
    ) {
        activityResultRegistry.register(
            code.incrementAndGet().toString(),
            ActivityResultContracts.StartActivityForResult(),
        ) {
            callback.invoke(it)
        }.launch(intent)
    }

    fun setTranslucentStatues() {
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        controller?.hide(WindowInsetsCompat.Type.statusBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
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

    fun toast(msg: CharSequence) {
        onUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}