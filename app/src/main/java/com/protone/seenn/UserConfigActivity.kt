package com.protone.seenn

import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.intent
import com.protone.api.json.toEntity
import com.protone.api.toMediaBitmapByteArray
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.mediamodle.GalleyHelper
import com.protone.seen.GalleySeen
import com.protone.seen.UserConfigSeen
import com.protone.seen.dialog.CheckListDialog
import com.protone.seen.dialog.TitleDialog
import com.protone.seen.popWindows.UserPops
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class UserConfigActivity : BaseActivity<UserConfigSeen>() {

    override suspend fun main() {
        val userConfigSeen = UserConfigSeen(this)
        setContentSeen(userConfigSeen)

        userConfigSeen.chooseMode(
            if (!userConfig.isLogin) {
                UserConfigSeen.DisplayMode.UnRegis
            } else if (userConfig.lockGalley != "" || userConfig.lockNote != "" || userConfig.lockMusic != "") {
                UserConfigSeen.DisplayMode.Locked
            } else {
                UserConfigSeen.DisplayMode.Normal
            }
        )

        while (isActive) {
            select<Unit> {
                event.onReceive {}
                userConfigSeen.viewEvent.onReceive {
                    when (it) {
                        UserConfigSeen.UserEvent.Login -> UserPops(this@UserConfigActivity).startLoginPopUp(
                            { name, password ->
                                return@startLoginPopUp when {
                                    userConfig.userName == "" -> false
                                    name != userConfig.userName -> false
                                    password != userConfig.userPassword -> false
                                    else -> {
                                        userConfigSeen.refreshLayout()
                                        userConfig.isLogin = true
                                        true
                                    }
                                }
                            },
                            {
                                UserPops(this@UserConfigActivity).startRegPopUp { s, s2 ->
                                    userConfig.userName = s
                                    userConfig.userPassword = s2
                                    userConfig.isLogin = true
                                    true
                                }
                            })
                        UserConfigSeen.UserEvent.Icon -> startActivityForResult(
                            ActivityResultContracts.StartActivityForResult(),
                            GalleyActivity::class.intent.apply {
                                putExtra(
                                    GalleyActivity.CHOOSE_MODE,
                                    GalleySeen.CHOOSE_PHOTO
                                )
                            }).let { re ->
                            if (re != null) {
                                val toEntity = re.data?.getStringExtra("GalleyData")
                                    ?.toEntity(GalleyMedia::class.java)
                                if (toEntity != null) {
                                    GalleyHelper.saveIconToLocal(
                                        toEntity.name,
                                        toEntity.uri.toMediaBitmapByteArray()
                                    ) { s ->
                                        if (!s.isNullOrEmpty()) {
                                            userConfig.userIcon = s
                                        } else {
                                            toast(getString(R.string.failed_upload_image))
                                        }
                                    }
                                } else {
                                    toast(getString(R.string.come_up_unknown_error))
                                }
                            } else {
                                toast(getString(R.string.come_up_unknown_error))
                            }
                        }
                        UserConfigSeen.UserEvent.Name -> {}
                        UserConfigSeen.UserEvent.PassWord -> {}
                        UserConfigSeen.UserEvent.ShareNote -> {}
                        UserConfigSeen.UserEvent.ShareData -> {}
                        UserConfigSeen.UserEvent.Lock -> startLockListPop()
                        UserConfigSeen.UserEvent.Unlock -> startUnlockListPop()
                    }
                }
            }
        }
    }

    private fun UserConfigSeen.refreshLayout() {
        clear()
        chooseMode(
            if (userConfig.lockGalley != "" || userConfig.lockNote != "" || userConfig.lockMusic != "") {
                UserConfigSeen.DisplayMode.Locked
            } else {
                UserConfigSeen.DisplayMode.Normal
            }
        )
    }

    private suspend fun startLockListPop() {
        CheckListDialog(this@UserConfigActivity, withContext(Dispatchers.IO) {
            val allGalleyBucket = DataBaseDAOHelper.getALLGalleyBucket()
            val list =
                mutableListOf(getString(R.string.model_noteBook), getString(R.string.model_music))
            allGalleyBucket?.forEach {
                list.add(it.type)
            }
            list
        }) {
            if (!it.isNullOrEmpty()) {
                TitleDialog(this@UserConfigActivity, "选择", "") { lock ->
                    if (lock.isEmpty()) {
                        toast(getString(R.string.none))
                        return@TitleDialog
                    }
                    when (it) {
                        getString(R.string.model_noteBook) -> userConfig.lockNote = lock
                        getString(R.string.model_music) -> userConfig.lockNote = lock
                        else -> userConfig.lockGalley = lock
                    }
                }
            } else {
                toast(getString(R.string.none))
            }
        }
    }

    private suspend fun startUnlockListPop() {
        CheckListDialog(this@UserConfigActivity, withContext(Dispatchers.IO) {
            val allGalleyBucket = DataBaseDAOHelper.getALLGalleyBucket()
            val list =
                mutableListOf(getString(R.string.model_noteBook), getString(R.string.model_music))
            allGalleyBucket?.forEach {
                list.add(it.type)
            }
            list
        }) {
            if (!it.isNullOrEmpty()) {
                TitleDialog(this@UserConfigActivity, "选择", "") { lock ->
                    if (lock.isEmpty()) {
                        toast(getString(R.string.none))
                        return@TitleDialog
                    }
                    when (it) {
                        getString(R.string.model_noteBook) -> userConfig.lockNote = lock
                        getString(R.string.model_music) -> userConfig.lockNote = lock
                        else -> userConfig.lockGalley = lock
                    }
                }
            } else {
                toast(getString(R.string.none))
            }
        }
    }
}