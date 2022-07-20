package com.protone.seen.dialog

import android.app.Activity
import android.net.Uri
import android.text.Editable
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.protone.api.context.*
import com.protone.api.onResult
import com.protone.seen.R
import com.protone.seen.adapter.BaseAdapter
import com.protone.seen.adapter.CheckListAdapter
import com.protone.seen.databinding.*
import kotlinx.coroutines.Dispatchers

private var create: AlertDialog? = null
    set(value) {
        field = value
        if (value?.ownerActivity != null) {
            value.setOnShowListener {
                val attributes = value.window?.attributes
                val height =
                    SApplication.screenHeight / 2 - ((value.window?.decorView?.height ?: 0) / 2)
                (value.ownerActivity as Activity).setSoftInputStatusListener { h, b ->
                    if (b && h + value.context.navigationBarHeight > height) {
                        attributes?.y = attributes?.y?.minus(h - height)
                            ?.minus(value.context.navigationBarHeight)
                        value.onWindowAttributesChanged(attributes)
                    } else {
                        attributes?.y = 0
                        value.onWindowAttributesChanged(attributes)
                    }
                }
            }
            value.setOnDismissListener {
                (value.ownerActivity as Activity).removeSoftInputStatusListener()
                create = null
            }
        }
    }

fun Activity.loginDialog(
    isReg: Boolean,
    loginCall: (String, String) -> Boolean,
    regClk: () -> Boolean
) {
    val binding = LoginPopLayoutBinding.inflate(layoutInflater, root, false)
    if (isReg) binding.btnReg.isGone = true
    create = AlertDialog.Builder(this).setView(binding.root).create().also {
        it.setOwnerActivity(this)
    }
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
                create?.dismiss()
            }
        }
    }
    binding.btnReg.setOnClickListener {
        create?.dismiss()
        regClk.invoke()
    }
    create?.show()
}

fun Activity.regDialog(confirmCall: (String, String) -> Boolean) {
    val binding = RegPopLayoutBinding.inflate(layoutInflater, root, false)
    create = AlertDialog.Builder(this).setView(binding.root).create().also {
        it.setOwnerActivity(this)
    }
    binding.btnLogin.setOnClickListener {
        confirmCall.invoke(
            binding.userName.text.toString(),
            binding.userPassword.text.toString()
        ).let { re ->
            if (re) create?.dismiss()
        }
    }
    create?.show()

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

    AlertDialog.Builder(this).also {
        it.setView(binding.root)
        it.setPositiveButton(
            R.string.confirm
        ) { p0, _ ->
            callBack(binding.renameInput.text.toString())
            p0?.dismiss()
        }
        it.setNegativeButton(R.string.cancel) { p0, _ ->
            p0?.dismiss()
        }
    }.create().also {
        it.setOwnerActivity(this)
        create = it
    }.show()
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
) = onResult(Dispatchers.Main) { co ->
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