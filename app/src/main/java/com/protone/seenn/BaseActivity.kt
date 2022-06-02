package com.protone.seenn

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.protone.api.ActivityLifecycleOwner
import com.protone.api.context.intent
import com.protone.api.context.musicIntentFilter
import com.protone.database.sp.config.UserConfig
import com.protone.seen.Seen
import com.protone.seenn.broadcast.MusicReceiver
import com.protone.seenn.service.MusicService
import com.protone.seenn.theme.ThemeProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class BaseActivity<S : Seen<*>> : AppCompatActivity(),
    CoroutineScope by MainScope() {

    var musicReceiver: MusicReceiver? = null
        set(value) {
            value?.let { registerReceiver(it, musicIntentFilter) }
            field = value
        }

    private var serviceConnection: ServiceConnection? = null

    protected val userConfig by lazy { UserConfig(this) }

    lateinit var binder: MusicService.MusicBinder

    enum class Event {
        OnStart,
        OnResume,
        OnStop,
        OnDestroy
    }

    @Suppress("PropertyName")
    protected val TAG = "TAG"

    protected val event = Channel<Event>(Channel.UNLIMITED)
    private val themeProvider by lazy { ThemeProvider(this) }
    private var onFinish: suspend () -> Unit = {}
    private val increaseInteger = AtomicInteger(0)
    protected var seen: S? = null
        private set(value) {
            field = value
            if (value != null) {
                setContentView(value.viewRoot)
            } else {
                setContentView(View(this))
            }
        }

    fun doOnFinish(block: suspend () -> Unit) {
        this.onFinish = block
    }

    abstract suspend fun main()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (themeProvider.isCustomTheme) {
            //
        }

        launch {
            main()

            finish()
        }
    }

    suspend fun <I, O> startActivityForResult(
        contract: ActivityResultContract<I, O>,
        input: I
    ): O = withContext(Dispatchers.Main) {
        val requestCode = increaseInteger.getAndIncrement().toString()

        ActivityLifecycleOwner().use { lifecycle, start ->
            suspendCoroutine { co ->
                activityResultRegistry.register(requestCode, lifecycle, contract) {
                    co.resumeWith(Result.success(it))
                }.apply { start() }.launch(input)
            }
        }
    }

    suspend fun setContentSeen(seen: S) {
        suspendCoroutine<Unit> {
            window.decorView.post {
                this.seen = seen
                it.resume(Unit)
            }
        }
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
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun finish() {
        try {

            launch {
                onFinish()
            }

        } finally {
            super.finish()
        }
    }

    override fun onStart() {
        super.onStart()
        event.offer(Event.OnStart)
    }

    override fun onResume() {
        super.onResume()
        event.offer(Event.OnResume)
    }

    override fun onStop() {
        super.onStop()
        event.offer(Event.OnStop)
    }

    override fun onDestroy() {
        seen?.cancel()
        cancel()
        serviceConnection?.let { unbindService(it) }
        musicReceiver?.let { unregisterReceiver(it) }
        Glide.get(this).clearMemory()
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Glide.get(this).clearMemory()
        }
        Glide.get(this).onTrimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).clearMemory()
    }
}