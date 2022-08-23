package com.protone.seenn.activity

import android.view.View
import androidx.core.view.isGone
import androidx.databinding.ViewDataBinding
import com.protone.api.baseType.getFileMimeType
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.context.*
import com.protone.api.entity.GalleyMedia
import com.protone.ui.databinding.GalleyOptionPopBinding
import com.protone.ui.dialog.cateDialog
import com.protone.ui.dialog.checkListDialog
import com.protone.ui.dialog.titleDialog
import com.protone.ui.popWindows.ColorfulPopWindow
import com.protone.ui.popWindows.GalleyOptionPop
import com.protone.worker.R
import com.protone.worker.database.DatabaseHelper
import com.protone.worker.viewModel.BaseViewModel
import com.protone.worker.viewModel.GalleyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.streams.toList

abstract class BaseMediaActivity<VB : ViewDataBinding, VM : BaseViewModel>(handleEvent: Boolean) :
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

    fun tryRename(gm: List<GalleyMedia>) {
        when {
            gm.size == 1 -> rename(gm[0])
            gm.size > 1 -> renameMulti(gm)
        }
    }

    fun tryDelete(
        gm: List<GalleyMedia>,
        callBack: (List<GalleyMedia>) -> Unit
    ) {
        when {
            gm.size == 1 -> delete(gm[0], callBack)
            gm.size > 1 -> deleteMulti(gm,callBack)
        }
    }

    private fun rename(gm: GalleyMedia) {
        val mimeType = gm.name.getFileMimeType()
        titleDialog(
            getString(R.string.rename),
            gm.name.replace(mimeType, "")
        ) { name ->
            val result = renameMedia(name + mimeType, gm.uri)
            if (result) {
                gm.name = name + mimeType
                R.string.success.getString().toast()
            } else R.string.not_supported.getString().toast()
        }
    }

    private fun renameMulti(gm: List<GalleyMedia>) {
        val reList = arrayListOf<String>()
        titleDialog(
            getString(R.string.rename),
            ""
        ) { name ->
            launch(Dispatchers.IO) {
                gm.forEach {
                    val result = funcForMultiRename(
                        "$name(${gm.indexOf(it)}).${it.name.getFileMimeType()}",
                        it.uri,
                    )
                    if (result != null) {
                        it.name = result
                    } else {
                        reList.add(it.name)
                    }
                }
                withContext(Dispatchers.Main) {
                    checkListDialog(R.string.this_file_op_failed.getString(), reList)
                }
            }
        }
    }

    private fun delete(
        gm: GalleyMedia,
        callBack: (List<GalleyMedia>) -> Unit
    ) {
        launch(Dispatchers.IO) {
            val result = deleteMedia(gm.uri)
            if (result) {
                DatabaseHelper.instance.signedGalleyDAOBridge.deleteSignedMedia(gm)
                callBack.invoke(mutableListOf(gm))
                R.string.success.getString().toast()
            } else R.string.not_supported.getString().toast()
        }
    }

    private fun deleteMulti(
        gm: List<GalleyMedia>,
        callBack: (List<GalleyMedia>) -> Unit
    ) {
        launch(Dispatchers.IO) {
            val reList = arrayListOf<GalleyMedia>()
            gm.forEach {
                val result = multiDeleteMedia(it.uri)
                if (result) {
                    DatabaseHelper.instance.signedGalleyDAOBridge.deleteSignedMedia(it)
                } else {
                    reList.add(it)
                }
            }
            callBack.invoke(reList)
            if (reList.size > 0) {
                reList.stream().map { it.name }.toList().let {
                    withContext(Dispatchers.Main) {
                        checkListDialog(
                            R.string.this_file_op_failed.getString(),
                            it as MutableList<String>
                        )
                    }
                }
            }
        }
    }

    fun addCate(gms: MutableList<GalleyMedia>) {
        cateDialog({
            titleDialog(R.string.addCate.getString(), "") { re ->
                if (re.isEmpty()) {
                    "请输入内容".toast()
                    return@titleDialog
                }
                addCate(re, gms)
            }
        }) {
            launch(Dispatchers.IO) {
                startActivityForResult(
                    GalleyActivity::class.intent.also { intent ->
                        intent.putExtra(
                            GalleyViewModel.CHOOSE_MODE,
                            GalleyViewModel.CHOOSE_PHOTO
                        )
                    }
                ).let { result ->
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
            DatabaseHelper.instance.signedGalleyDAOBridge.updateMediaMultiAsync(list)
        }
    }

    fun moveTo(
        anchor: View,
        isVideo: Boolean,
        gms: MutableList<GalleyMedia>,
        callback: (String, MutableList<GalleyMedia>) -> Unit
    ) = launch(Dispatchers.Main) {
        val pop = ColorfulPopWindow(this@BaseMediaActivity)
        pop.startListPopup(
           anchor =  anchor,
            dataList = withContext(Dispatchers.IO) {
                val list = mutableListOf<String>()
                DatabaseHelper.instance.galleyBucketDAOBridge.getALLGalleyBucket(isVideo)
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
                        else "${it.name}已存在${it.type}中".toast()
                    }
                    DatabaseHelper.instance.signedGalleyDAOBridge.updateMediaMultiAsync(list)
                    callback.invoke(re, list)
                }
            } else R.string.none.getString().toast()
            pop.dismiss()
        }
    }

}