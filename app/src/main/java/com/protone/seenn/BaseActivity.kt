package com.protone.seenn

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.protone.api.ActivityLifecycleOwner
import com.protone.api.context.*
import com.protone.api.getFileMimeType
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.GalleySeen
import com.protone.seen.Seen
import com.protone.seen.dialog.CateDialog
import com.protone.seen.dialog.TitleDialog
import com.protone.seen.popWindows.ColorfulPopWindow
import com.protone.seenn.broadcast.MusicReceiver
import com.protone.seenn.service.MusicService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class BaseActivity<S : Seen<*>> : AppCompatActivity(),
    CoroutineScope by MainScope() {

    companion object{
        const val FINISH = "ACTIVITY_FINISH"
    }

    var musicReceiver: MusicReceiver? = null
        set(value) {
            value?.let { registerReceiver(it, musicIntentFilter) }
            field = value
        }

    private val activityOperationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FINISH) {
                finish()
            }
        }
    }

    private var serviceConnection: ServiceConnection? = null

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
        activityOperationBroadcast.registerReceiver(activityOperationReceiver,
            IntentFilter(FINISH)
        )
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

    fun setTranslucentStatues() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        var systemUiVisibility = window.decorView.systemUiVisibility
        systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = systemUiVisibility
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
        event.trySend(Event.OnStart)
    }

    override fun onResume() {
        super.onResume()
        event.trySend(Event.OnResume)
    }

    override fun onStop() {
        super.onStop()
        event.trySend(Event.OnStop)
    }

    override fun onDestroy() {
        seen?.cancel()
        cancel()
        activityOperationBroadcast.unregisterReceiver(activityOperationReceiver)
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

    fun rename(gm: GalleyMedia, scope: CoroutineScope) {
        val mimeType = gm.name.getFileMimeType()
        TitleDialog(
            this,
            getString(R.string.rename),
            gm.name.replace(mimeType, "")
        ) { name ->
            renameMedia(name + mimeType, gm.uri, scope) { result ->
                if (result) {
                    gm.name = name + mimeType
                    toast(getString(R.string.success))
                } else toast(getString(R.string.not_supported))
            }
        }
    }

    fun delete(
        gm: GalleyMedia,
        scope: CoroutineScope,
        callBack: (GalleyMedia) -> Unit
    ) {
        deleteMedia(gm.uri, scope) { result ->
            if (result) {
                DataBaseDAOHelper.deleteSignedMedia(gm)
                callBack.invoke(gm)
                toast(getString(R.string.success))
            } else toast(getString(R.string.not_supported))
        }
    }

    fun addCate(gms: MutableList<GalleyMedia>) {
        CateDialog(this, {
            TitleDialog(this, getString(R.string.addCate), "") { re ->
                if (re.isEmpty()) {
                    toast("请输入内容")
                    return@TitleDialog
                }
                addCate(re, gms)
            }
        }) {
            launch {
                startActivityForResult(
                    ActivityResultContracts.StartActivityForResult(),
                    GalleyActivity::class.intent.also { intent ->
                        intent.putExtra(
                            GalleyActivity.CHOOSE_MODE,
                            GalleySeen.CHOOSE_PHOTO
                        )
                    }
                )?.let { result ->
                    val uri = result.data?.getStringExtra(GalleyActivity.URI)
                    if (uri != null) {
                        addCate(uri, gms)
                    } else showFailedToast()
                }
            }
        }
    }

    private fun addCate(cate: String, gms: MutableList<GalleyMedia>) {
        gms.let { list ->
            list.forEach { gm ->
                gm.also { g ->
                    if (g.cate == null) g.cate = mutableListOf()
                    (g.cate as MutableList).add(cate)
                }
            }
            DataBaseDAOHelper.updateMediaMulti(list)
        }
    }

    suspend fun moveTo(
        anchor: View,
        isVideo: Boolean,
        gms: MutableList<GalleyMedia>,
        callback: (String, MutableList<GalleyMedia>) -> Unit
    ) {
        val pop = ColorfulPopWindow(this)
        pop.startListPopup(
            anchor,
            withContext(Dispatchers.IO) {
                val list = mutableListOf<String>()
                DataBaseDAOHelper.getALLGalleyBucket(isVideo)
                    ?.forEach {
                        list.add(it.type)
                    }
                list
            }) { re ->
            if (re != null) {
                gms.let { list ->
                    list.forEach {
                        if (it.type == null) it.type = mutableListOf()
                        if (it.type?.contains(re) == false)
                            (it.type as MutableList<String>).add(re)
                        else toast("${it.name}已存在${it.type}中")
                    }
                    DataBaseDAOHelper.updateMediaMulti(list)
                    callback.invoke(re, list)
                }
            } else toast(getString(R.string.none))
            pop.dismiss()
        }
    }
}