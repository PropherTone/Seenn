package com.protone.seen.dialog

import android.app.Activity
import android.net.Uri
import android.text.Editable
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.protone.api.context.newLayoutInflater
import com.protone.api.context.root
import com.protone.api.context.setSoftInputStatuesListener
import com.protone.seen.R
import com.protone.seen.adapter.BaseAdapter
import com.protone.seen.adapter.CheckListAdapter
import com.protone.seen.databinding.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

fun Activity.loginDialog(
    isReg: Boolean,
    loginCall: (String, String) -> Boolean,
    regClk: () -> Boolean
) {
    val binding = LoginPopLayoutBinding.inflate(layoutInflater, root, false)
    if (isReg) binding.btnReg.isGone = true
    val log = AlertDialog.Builder(this).setView(binding.root).create()
    binding.btnLogin.setOnClickListener {
        loginCall.invoke(
            binding.userName.text.toString(),
            binding.userPassword.text.toString()
        ).let { re ->
            if (!re) {
                binding.userNameLayout.isErrorEnabled = true
                binding.userPasswordLayout.isErrorEnabled = true
                binding.userNameLayout.error = " "
                binding.userPasswordLayout.error = " "
            } else {
                log.dismiss()
            }
        }
    }
    binding.btnReg.setOnClickListener {
        log.dismiss()
        regClk.invoke()
    }
    log.show()
    val attributes = log.window?.attributes
    val oldY = attributes?.y
    setSoftInputStatuesListener { i, b ->
        if (b) {
            attributes?.y = attributes?.y?.minus(binding.root.measuredHeight / 2)
            log.onWindowAttributesChanged(attributes)
        } else {
            attributes?.y = oldY
            log.onWindowAttributesChanged(attributes)
        }
    }
}

fun Activity.regDialog(confirmCall: (String, String) -> Boolean) {
    val binding = RegPopLayoutBinding.inflate(layoutInflater, root, false)
    val log = AlertDialog.Builder(this).setView(binding.root).create()
    binding.btnLogin.setOnClickListener {
        confirmCall.invoke(
            binding.userName.text.toString(),
            binding.userPassword.text.toString()
        ).let { re ->
            if (re) log.dismiss()
        }
    }
    log.show()
    val attributes = log.window?.attributes
    val oldY = attributes?.y
    setSoftInputStatuesListener { i, b ->
        if (b) {
            attributes?.y = attributes?.y?.minus(binding.root.measuredHeight / 2)
            log.onWindowAttributesChanged(attributes)
        } else {
            attributes?.y = oldY
            log.onWindowAttributesChanged(attributes)
        }
    }

}

fun Activity.titleDialog(title: String, name: String, callBack: (String) -> Unit) {

    val binding by lazy {
        RenameDialogLayoutBinding.inflate(
            newLayoutInflater,
            root,
            false
        ).apply {
            this.renameTitle.text = title
            renameInput.text = Editable.Factory.getInstance().newEditable(name)
        }
    }

    var create: AlertDialog?
    AlertDialog.Builder(this).also {
        it.setView(binding.root)
        it.setPositiveButton(
            R.string.confirm
        ) { p0, _ ->
            callBack(binding.renameInput.text.toString())
            p0?.dismiss()
            create = null
        }
        it.setNegativeButton(R.string.cancel) { p0, _ ->
            p0?.dismiss()
            create = null
        }
    }.create().also {
        create = it
    }.show()
    val attributes = create?.window?.attributes
    setSoftInputStatuesListener { _, b ->
        if (b) {
            attributes?.y = attributes?.y?.minus(binding.root.measuredHeight / 2)
            create?.onWindowAttributesChanged(attributes)
        } else {
            attributes?.y = 0
            create?.onWindowAttributesChanged(attributes)
        }
    }

}

fun Activity.cateDialog(
    addCate: () -> Unit,
    addCon: () -> Unit
) {
    val binding by lazy {
        CateDialogLayoutBinding.inflate(
            newLayoutInflater,
            root,
            false
        )
    }

    AlertDialog.Builder(this, R.style.TransparentAlertDialog).setView(binding.root).create()
        .also { dialog ->
            binding.btnAddCate.setOnClickListener {
                dialog.dismiss()
                addCate.invoke()
            }
            binding.btnAddCon.setOnClickListener {
                dialog.dismiss()
                addCon.invoke()
            }
        }.show()

}

fun Activity.checkListDialog(
    title: String,
    dataList: MutableList<String>,
    callBack: ((String?) -> Unit)? = null
) {
    val binding = ListPopWindowsLayoutBinding.inflate(
        newLayoutInflater,
        root,
        false
    )
    val create = AlertDialog.Builder(this).setView(binding.root).create()
    binding.apply {
        listTitle.text = title
        listList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CheckListAdapter(context, dataList)
        }
        listConfirm.setOnClickListener {
            listList.adapter.let {
                if (it is CheckListAdapter)
                    callBack?.invoke(if (it.selectList.size > 0) it.selectList[0] else null)
            }
            create.dismiss()
        }
    }
    create.show()
}

suspend fun Activity.imageListDialog(
    dataList: MutableList<Uri>
) = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine<Boolean> { co ->
        val binding = ImageListDialogLayoutBinding.inflate(
            newLayoutInflater,
            root,
            false
        )
        val create = AlertDialog.Builder(this@imageListDialog).setView(binding.root).create()
        binding.apply {
            listList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = object : BaseAdapter<PhotoCardLayoutBinding>(context) {

                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): Holder<PhotoCardLayoutBinding> {
                        return Holder(PhotoCardLayoutBinding.inflate(layoutInflater, parent, false))
                    }

                    override fun onBindViewHolder(
                        holder: Holder<PhotoCardLayoutBinding>,
                        position: Int
                    ) {
                        Glide.with(context).asDrawable().load(dataList[position])
                            .into(holder.binding.photoCardPhoto)
                        val i = position + 1
                        holder.binding.photoCardTitle.text = i.toString()
                    }

                    override fun getItemCount(): Int = dataList.size

                }
            }
            listConfirm.setOnClickListener {
                co.resumeWith(Result.success(true))
                create.dismiss()
            }
            listDismiss.setOnClickListener {
                co.resumeWith(Result.success(false))
                create.dismiss()
            }
        }
        create.show()
    }

}