package com.protone.seenn.activity

import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import com.protone.api.baseType.getString
import com.protone.api.baseType.saveToFile
import com.protone.api.baseType.toast
import com.protone.api.context.SApplication
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.entity.GalleyMedia
import com.protone.api.json.toEntity
import com.protone.seenn.R
import com.protone.seenn.databinding.UserConfigActivityBinding
import com.protone.ui.databinding.UserConfigItemLayoutBinding
import com.protone.ui.dialog.checkListDialog
import com.protone.ui.dialog.loginDialog
import com.protone.ui.dialog.regDialog
import com.protone.ui.dialog.titleDialog
import com.protone.worker.database.userConfig
import com.protone.worker.viewModel.BaseViewModel
import com.protone.worker.viewModel.GalleyViewModel
import com.protone.worker.viewModel.UserConfigViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UserConfigActivity : BaseActivity<UserConfigActivityBinding, UserConfigViewModel>(true) {
    override val viewModel: UserConfigViewModel by viewModels()

    override fun createView(): View {
        binding = UserConfigActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        return binding.root
    }

    override suspend fun UserConfigViewModel.init() {
        refreshLayout()
        onViewEvent {
            when (it) {
                UserConfigViewModel.UserConfigEvent.Login -> startLoginDialog()
                UserConfigViewModel.UserConfigEvent.Icon -> startIconPick()
                UserConfigViewModel.UserConfigEvent.Name -> startNameDialog()
                UserConfigViewModel.UserConfigEvent.PassWord -> startPasswordDialog()
                UserConfigViewModel.UserConfigEvent.Share -> {}
                UserConfigViewModel.UserConfigEvent.Lock -> startLockListPop()
                UserConfigViewModel.UserConfigEvent.Unlock -> startUnlockListPop()
                UserConfigViewModel.UserConfigEvent.Refresh -> refreshLayout()
                UserConfigViewModel.UserConfigEvent.ClearCache -> viewModel.clearCache()
                UserConfigViewModel.UserConfigEvent.Log -> startActivity(LogActivity::class.intent)
                UserConfigViewModel.UserConfigEvent.CombineGalley -> userConfig.combineGalley = true
                UserConfigViewModel.UserConfigEvent.DispatchGalley -> userConfig.combineGalley =
                    false
            }
        }
    }

    private fun clear() {
        binding.userRoot.removeAllViews()
    }

    private fun chooseMode(
        loginMode: UserConfigViewModel.DisplayMode,
        lockMode: UserConfigViewModel.DisplayMode,
        galleyMode: UserConfigViewModel.DisplayMode
    ) {
        val logView = initModeView(R.string.log.getString()) {
            UserConfigViewModel.UserConfigEvent.Log
        }
        if (loginMode == UserConfigViewModel.DisplayMode.UnRegis) {
            UserConfigItemLayoutBinding.inflate(layoutInflater, root, false)
                .apply {
                    itemName.text = R.string.login.getString()
                    itemName.setOnClickListener { startLoginDialog() }
                }.root.run {
                    layoutParams = ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        SApplication.screenHeight - resources.getDimensionPixelSize(R.dimen.user_icon)
                    )
                    binding.userRoot.addView(this)
                }
            binding.userRoot.addView(logView)
            return
        }
        val views = mutableListOf<View>(
            initModeView(
                R.string.change_icon.getString()
            ) { UserConfigViewModel.UserConfigEvent.Icon },
            initModeView(
                R.string.change_name.getString()
            ) { UserConfigViewModel.UserConfigEvent.Name },
            initModeView(
                R.string.change_password.getString()
            ) { UserConfigViewModel.UserConfigEvent.PassWord },
            initModeView(
                R.string.share.getString()
            ) { UserConfigViewModel.UserConfigEvent.Share },
            initModeView(
                if (lockMode != UserConfigViewModel.DisplayMode.Locked)
                    R.string.encryp_model.getString() else R.string.unlock_model.getString()
            ) {
                if (lockMode != UserConfigViewModel.DisplayMode.Locked)
                    UserConfigViewModel.UserConfigEvent.Lock else UserConfigViewModel.UserConfigEvent.Unlock
            },
            initModeView(
                if (galleyMode == UserConfigViewModel.DisplayMode.CombineGalley)
                    R.string.dispatch_galley.getString() else R.string.combine_galley.getString()
            ) {
                if (it.itemName.text.equals(R.string.dispatch_galley.getString())) {
                    it.itemName.text = R.string.combine_galley.getString()
                    UserConfigViewModel.UserConfigEvent.DispatchGalley
                } else {
                    it.itemName.text = R.string.dispatch_galley.getString()
                    UserConfigViewModel.UserConfigEvent.CombineGalley
                }
            },
            initModeView(
                R.string.clear_cache.getString()
            ) { UserConfigViewModel.UserConfigEvent.ClearCache },
            logView
        )
        launch {
            views.forEach {
                TransitionManager.beginDelayedTransition(binding.userRoot)
                binding.userRoot.addView(it)
                delay(180)
            }
        }
    }

    private fun initModeView(
        name: String,
        onClick: (UserConfigItemLayoutBinding) -> BaseViewModel.ViewEvent
    ) =
        UserConfigItemLayoutBinding.inflate(layoutInflater, root, false)
            .apply {
                itemName.text = name
                itemName.setOnClickListener {
                    sendViewEvent(onClick.invoke(this))
                }
            }.root

    private suspend fun startIconPick() {
        startActivityForResult(
            GalleyActivity::class.intent.apply {
                putExtra(
                    GalleyViewModel.CHOOSE_MODE,
                    GalleyViewModel.CHOOSE_PHOTO
                )
            }).let { re ->
            if (re == null) {
                R.string.come_up_unknown_error.getString().toast()
                return@let
            }
            val toEntity = re.data?.getStringExtra(GalleyViewModel.GALLEY_DATA)
                ?.toEntity(GalleyMedia::class.java)
            if (toEntity == null) {
                R.string.come_up_unknown_error.getString().toast()
                return@let
            }
            toEntity.uri.saveToFile(toEntity.name).let { s ->
                if (!s.isNullOrEmpty()) {
                    if (userConfig.userIcon.isNotEmpty()) {
                        viewModel.deleteOldIcon(userConfig.userIcon)
                    }
                    userConfig.userIcon = s
                } else {
                    R.string.failed_upload_image.getString().toast()
                }
            }
        }
    }

    private fun startLoginDialog() {
        loginDialog(
            userConfig.userName != "",
            { name, password ->
                return@loginDialog when {
                    userConfig.userName == "" -> false
                    name != userConfig.userName -> false
                    password != userConfig.userPassword -> false
                    else -> {
                        refreshLayout()
                        userConfig.isLogin = true
                        true
                    }
                }
            },
            {
                var result = false
                regDialog { s, s2 ->
                    userConfig.userName = s
                    userConfig.userPassword = s2
                    userConfig.isLogin = true
                    result = true
                    result
                }
                result
            })
    }

    private fun startNameDialog() {
        titleDialog(R.string.user_name.getString(), "") {
            if (it.isNotEmpty()) {
                userConfig.userName = it
            }
        }
    }

    private fun startPasswordDialog() {
        titleDialog(R.string.password.getString(), "") {
            if (it == userConfig.userPassword) {
                userConfig.userPassword = it
                R.string.success.getString().toast()
            } else R.string.wrong_password.getString().toast()
        }
    }

    private fun refreshLayout() {
        clear()
        chooseMode(
            if (!userConfig.isLogin) {
                UserConfigViewModel.DisplayMode.UnRegis
            } else UserConfigViewModel.DisplayMode.Normal,
            if (userConfig.lockGalley != "" || userConfig.lockNote != "" || userConfig.lockMusic != "") {
                UserConfigViewModel.DisplayMode.Locked
            } else UserConfigViewModel.DisplayMode.Normal,
            if (userConfig.combineGalley) {
                UserConfigViewModel.DisplayMode.CombineGalley
            } else UserConfigViewModel.DisplayMode.Normal
        )
    }

    private fun startLockListPop() {
        checkListDialog(
            R.string.choose.getString(),
            mutableListOf(
                R.string.model_noteBook.getString(),
                R.string.model_music.getString(),
                R.string.model_Galley.getString()
            )
        ) {
            if (!it.isNullOrEmpty()) {
                titleDialog(R.string.choose.getString(), "") { lock ->
                    if (lock.isEmpty()) {
                        R.string.none.getString().toast()
                        return@titleDialog
                    }
                    when (it) {
                        R.string.model_noteBook.getString() -> userConfig.lockNote = lock
                        R.string.model_music.getString() -> userConfig.lockNote = lock
                        else -> userConfig.lockGalley = lock
                    }
                }
            } else {
                R.string.none.getString().toast()
            }
        }
    }

    private fun startUnlockListPop() {
        checkListDialog(
            R.string.choose.getString(),
            mutableListOf(
                R.string.model_noteBook.getString(),
                R.string.model_music.getString(),
                R.string.model_Galley.getString()
            )
        ) {
            if (!it.isNullOrEmpty()) {
                titleDialog(R.string.password.getString(), "") { lock ->
                    if (lock.isEmpty()) {
                        R.string.none.getString().toast()
                        return@titleDialog
                    }
                    if (when (it) {
                            R.string.model_noteBook.getString() -> {
                                if (userConfig.lockNote == lock) {
                                    userConfig.lockNote = ""
                                    true
                                } else false
                            }
                            R.string.model_music.getString() -> {
                                if (userConfig.lockNote == lock) {
                                    userConfig.lockNote = ""
                                    true
                                } else false
                            }
                            else -> {
                                if (userConfig.lockGalley == lock) {
                                    userConfig.lockGalley = ""
                                    true
                                } else false
                            }
                        }
                    ) R.string.success.getString().toast()
                    else R.string.wrong_password.getString().toast()
                }
            } else {
                R.string.none.getString().toast()
            }
        }
    }
}