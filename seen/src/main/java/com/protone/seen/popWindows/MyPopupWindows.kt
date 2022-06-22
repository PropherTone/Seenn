package com.protone.seen.popWindows

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.api.context.setSoftInputStatuesListener
import com.protone.seen.databinding.LoginPopLayoutBinding
import com.protone.seen.databinding.RegPopLayoutBinding
import java.lang.ref.WeakReference


class UserPops(context: Context) {

    private val weakContext = WeakReference(context)

    fun startLoginPopUp(isReg:Boolean,loginCall: (String, String) -> Boolean, regClk: ()->Boolean) =
        weakContext.get()?.let { context ->
            val binding = LoginPopLayoutBinding.inflate(context.layoutInflater, context.root, false)
            if (isReg) binding.btnReg.isGone = true
            val log = AlertDialog.Builder(context).setView(binding.root).create()
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
            binding.btnReg.setOnClickListener{
                binding.btnReg.isGone = regClk.invoke()
            }
            log.show()
            val attributes = log.window?.attributes
            val oldY = attributes?.y
            if (context is Activity) {
                context.setSoftInputStatuesListener { i, b ->
                    if (b) {
                        attributes?.y = attributes?.y?.minus(binding.root.measuredHeight / 2)
                        log.onWindowAttributesChanged(attributes)
                    } else {
                        attributes?.y = oldY
                        log.onWindowAttributesChanged(attributes)
                    }
                }
            }
        }

    fun startRegPopUp(confirmCall: (String, String) -> Boolean) =
        weakContext.get()?.let { context ->
            val binding = RegPopLayoutBinding.inflate(context.layoutInflater, context.root, false)
            val log = AlertDialog.Builder(context).setView(binding.root).create()
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
            if (context is Activity) {
                context.setSoftInputStatuesListener { i, b ->
                    if (b) {
                        attributes?.y = attributes?.y?.minus(binding.root.measuredHeight / 2)
                        log.onWindowAttributesChanged(attributes)
                    } else {
                        attributes?.y = oldY
                        log.onWindowAttributesChanged(attributes)
                    }
                }
            }
        }
}
