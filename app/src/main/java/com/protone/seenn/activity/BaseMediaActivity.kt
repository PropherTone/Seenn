package com.protone.seenn.activity

import android.net.Uri
import android.view.View
import androidx.core.view.isGone
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import com.protone.api.context.*
import com.protone.api.getFileMimeType
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.databinding.GalleyOptionPopBinding
import com.protone.seen.dialog.CateDialog
import com.protone.seen.dialog.TitleDialog
import com.protone.seen.popWindows.ColorfulPopWindow
import com.protone.seen.popWindows.GalleyOptionPop
import com.protone.seenn.R
import com.protone.seenn.viewModel.GalleyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.stream.Collectors

abstract class BaseMediaActivity<VB : ViewDataBinding, VM : ViewModel>(handleEvent: Boolean) :
    BaseActivity<VB, VM>(handleEvent),
    View.OnClickListener {

    var popLayout: GalleyOptionPopBinding? = null

    private var pop: GalleyOptionPop? = null

    abstract fun popDelete()
    abstract fun popMoveTo()
    abstract fun popRename()
    abstract fun popSelectAll()
    abstract fun popSetCate()
    abstract fun popIntoBox()

    fun initPop() {
        if (pop == null) {
            popLayout = GalleyOptionPopBinding.inflate(layoutInflater, root, false).apply {
                pop = GalleyOptionPop(this@BaseMediaActivity, root)
                galleyDelete.setOnClickListener(this@BaseMediaActivity)
                galleyMoveTo.setOnClickListener(this@BaseMediaActivity)
                galleyRename.setOnClickListener(this@BaseMediaActivity)
                galleySelectAll.setOnClickListener(this@BaseMediaActivity)
                galleySetCate.setOnClickListener(this@BaseMediaActivity)
                galleyIntoBox.setOnClickListener(this@BaseMediaActivity)
            }
        }
    }

    fun showPop(anchor: View, onSelect: Boolean) {
        popLayout?.apply {
            galleyDelete.isGone = onSelect
            galleyMoveTo.isGone = onSelect
            galleyRename.isGone = onSelect
            galleySetCate.isGone = onSelect
        }
        pop?.showPop(anchor)
    }

    override fun onClick(v: View?) {
        popLayout?.apply {
            when (v) {
                galleyDelete -> popDelete()
                galleyMoveTo -> popMoveTo()
                galleyRename -> popRename()
                galleySelectAll -> popSelectAll()
                galleySetCate -> popSetCate()
                galleyIntoBox -> popIntoBox()
            }
        }
        pop?.dismiss()
    }

    fun tryRename(gm: List<GalleyMedia>, scope: CoroutineScope) {
        when {
            gm.size == 1 -> rename(gm[0], scope)
            gm.size > 1 -> renameMulti(gm)
        }
    }

    fun tryDelete(
        gm: List<GalleyMedia>,
        scope: CoroutineScope,
        callBack: (GalleyMedia) -> Unit
    ) {
        when {
            gm.size == 1 -> delete(gm[0], scope, callBack)
            gm.size > 1 -> deleteMulti(gm, scope, callBack)
        }
    }

    private fun rename(gm: GalleyMedia, scope: CoroutineScope) {
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

    private fun renameMulti(gm: List<GalleyMedia>) {
        TitleDialog(
            this,
            getString(R.string.rename),
            ""
        ) { name ->
            launch(Dispatchers.IO) {
                gm.forEach {
                    funcForMultiRename(
                        "$name(${gm.indexOf(it)}).${it.name.getFileMimeType()}",
                        it.uri
                    ) { result ->
                        if (result != null) {
                            it.name = result
                            toast(getString(R.string.success))
                        } else toast(getString(R.string.not_supported))
                    }
                }
            }
        }
    }

    private fun delete(
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

    private fun deleteMulti(
        gm: List<GalleyMedia>,
        scope: CoroutineScope,
        callBack: (GalleyMedia) -> Unit
    ) {
        val list = gm.stream().map {
            it.uri
        }.collect(Collectors.toList()) as List<Uri>
        var count = 0
        multiDeleteMedia(list, scope) { result ->
            if (result) {
                DataBaseDAOHelper.deleteSignedMedia(gm[count])
                callBack.invoke(gm[count])
                toast(getString(R.string.success))
            } else toast(getString(R.string.not_supported))
            count++
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
                    GalleyActivity::class.intent.also { intent ->
                        intent.putExtra(
                            GalleyViewModel.CHOOSE_MODE,
                            GalleyViewModel.CHOOSE_PHOTO
                        )
                    }
                ) .let{ result ->
                    val uri = result?.data?.getStringExtra(GalleyViewModel.URI)
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
    ) = withContext(Dispatchers.Main) {
        val pop = ColorfulPopWindow(this@BaseMediaActivity)
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