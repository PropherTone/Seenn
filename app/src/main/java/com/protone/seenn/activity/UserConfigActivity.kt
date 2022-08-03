package com.protone.seenn.activity

import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import com.protone.api.baseType.getString
import com.protone.api.baseType.toMediaBitmapByteArray
import com.protone.api.baseType.toast
import com.protone.api.context.SApplication
import com.protone.api.context.intent
import com.protone.api.context.root
import com.protone.api.entity.GalleyMedia
import com.protone.api.json.toEntity
import com.protone.seen.databinding.UserConfigItemLayoutBinding
import com.protone.seen.dialog.checkListDialog
import com.protone.seen.dialog.loginDialog
import com.protone.seen.dialog.regDialog
import com.protone.seen.dialog.titleDialog
import com.protone.seenn.GalleyHelper
import com.protone.seenn.R
import com.protone.seenn.database.userConfig
import com.protone.seenn.databinding.UserConfigActivityBinding
import com.protone.seenn.viewModel.BaseViewModel
import com.protone.seenn.viewModel.GalleyViewModel
import com.protone.seenn.viewModel.UserConfigViewModel
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
        chooseMode(
            if (!userConfig.isLogin) {
                UserConfigViewModel.DisplayMode.UnRegis
            } else if (userConfig.lockGalley != "" || userConfig.lockNote != "" || userConfig.lockMusic != "") {
                UserConfigViewModel.DisplayMode.Locked
            } else {
                UserConfigViewModel.DisplayMode.Normal
            }
        )

        onViewEvent {
            when (it) {
                UserConfigViewModel.UserConfigEvent.Login -> startLoginDialog()
                UserConfigViewModel.UserConfigEvent.Icon -> startIconPick()
                UserConfigViewModel.UserConfigEvent.Name -> startNameDialog()
                UserConfigViewModel.UserConfigEvent.PassWord -> startPasswordDialog()
                UserConfigViewModel.UserConfigEvent.ShareNote -> {}
                UserConfigViewModel.UserConfigEvent.ShareData -> {}
                UserConfigViewModel.UserConfigEvent.Lock -> startLockListPop()
                UserConfigViewModel.UserConfigEvent.Unlock -> startUnlockListPop()
                UserConfigViewModel.UserConfigEvent.Refresh -> refreshLayout()
                UserConfigViewModel.UserConfigEvent.ClearCache -> viewModel.clearCache()
                UserConfigViewModel.UserConfigEvent.Log -> startActivity(LogActivity::class.intent)
            }
        }
    }

    private fun clear() {
        binding.userRoot.removeAllViews()
    }

    private fun chooseMode(mode: UserConfigViewModel.DisplayMode) {
        val logView = initModeView("日志", UserConfigViewModel.UserConfigEvent.Log)
        if (mode == UserConfigViewModel.DisplayMode.UnRegis) {
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
            initModeView("修改头像", UserConfigViewModel.UserConfigEvent.Icon),
            initModeView("修改名称", UserConfigViewModel.UserConfigEvent.Name),
            initModeView("修改密码", UserConfigViewModel.UserConfigEvent.PassWord),
            initModeView("笔记共享", UserConfigViewModel.UserConfigEvent.ShareNote),
            initModeView("数据共享", UserConfigViewModel.UserConfigEvent.ShareData),
            initModeView("模块加密", UserConfigViewModel.UserConfigEvent.Lock),
            initModeView("清理缓存", UserConfigViewModel.UserConfigEvent.ClearCache),
            logView
        )
        if (mode == UserConfigViewModel.DisplayMode.Locked) views.add(
            initModeView(
                "模块解锁",
                UserConfigViewModel.UserConfigEvent.Unlock
            )
        )
        launch {
            views.forEach {
                TransitionManager.beginDelayedTransition(binding.userRoot)
                binding.userRoot.addView(it)
                delay(180)
            }
        }
    }

    private fun initModeView(name: String, event: BaseViewModel.ViewEvent) =
        UserConfigItemLayoutBinding.inflate(layoutInflater, root, false)
            .apply {
                itemName.text = name
                itemName.setOnClickListener { sendViewEvent(event) }
            }.root

    private suspend fun startIconPick() {
        startActivityForResult(
            GalleyActivity::class.intent.apply {
                putExtra(
                    GalleyViewModel.CHOOSE_MODE,
                    GalleyViewModel.CHOOSE_PHOTO
                )
            }).let { re ->
            if (re != null) {
                val toEntity = re.data?.getStringExtra(GalleyViewModel.GALLEY_DATA)
                    ?.toEntity(GalleyMedia::class.java)
                if (toEntity != null) {
                    GalleyHelper.saveIconToLocal(
                        toEntity.name,
                        toEntity.uri.toMediaBitmapByteArray()
                    ) { s ->
                        if (!s.isNullOrEmpty()) {
                            if (userConfig.userIcon.isNotEmpty()) {
                                viewModel.deleteOldIcon(userConfig.userIcon)
                            }
                            userConfig.userIcon = s
                        } else {
                            R.string.failed_upload_image.getString().toast()
                        }
                    }
                } else {
                    R.string.come_up_unknown_error.getString().toast()
                }
            } else {
                R.string.come_up_unknown_error.getString().toast()
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
            if (userConfig.lockGalley != "" || userConfig.lockNote != "" || userConfig.lockMusic != "") {
                UserConfigViewModel.DisplayMode.Locked
            } else {
                UserConfigViewModel.DisplayMode.Normal
            }
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