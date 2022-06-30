package com.protone.seenn.activity

import android.view.View
import androidx.core.view.isGone
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import com.protone.api.context.*
import com.protone.api.getFileMimeType
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.GalleySeen
import com.protone.seen.databinding.GalleyOptionPopBinding
import com.protone.seen.dialog.CateDialog
import com.protone.seen.dialog.TitleDialog
import com.protone.seen.popWindows.ColorfulPopWindow
import com.protone.seen.popWindows.GalleyOptionPop
import com.protone.seenn.GalleyActivity
import com.protone.seenn.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseMediaActivity<VB : ViewDataBinding, VM : ViewModel> : BaseActivity<VB, VM>(),
    View.OnClickListener {

    var popLayout: GalleyOptionPopBinding? = null

    private var pop: GalleyOptionPop? = null

    abstract fun popDelete()
    abstract fun popMoveTo()
    abstract fun popRename()
    abstract fun popSelectAll()
    abstract fun popSetCate()
    abstract fun popIntoBox()

    fun initPop(){
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

    fun showPop(anchor: View, isSelect: Boolean) {
        popLayout?.apply {
            galleyDelete.isGone = isSelect
            galleyMoveTo.isGone = isSelect
            galleyRename.isGone = isSelect
            galleySetCate.isGone = isSelect
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
            launch(Dispatchers.IO) {
                startActivityForResult(
                    GalleyActivity::class.intent.also { intent ->
                        intent.putExtra(
                            GalleyActivity.CHOOSE_MODE,
                            GalleySeen.CHOOSE_PHOTO
                        )
                    }
                ) { result ->
                    val uri = result?.data?.getStringExtra(GalleyActivity.URI)
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
    ) = withContext(Dispatchers.Main){
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