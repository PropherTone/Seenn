package com.protone.seenn

import androidx.activity.result.contract.ActivityResultContracts
import com.protone.api.context.intent
import com.protone.api.json.toEntity
import com.protone.api.json.toUriJson
import com.protone.api.toDrawable
import com.protone.api.toMediaBitmapByteArray
import com.protone.database.room.entity.GalleyMedia
import com.protone.mediamodle.GalleyHelper
import com.protone.seen.GalleySeen
import com.protone.seen.UserConfigSeen
import com.protone.seen.popWindows.UserPops
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

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
                        UserConfigSeen.UserEvent.Note -> {}
                        UserConfigSeen.UserEvent.ShareNote -> {}
                        UserConfigSeen.UserEvent.Data -> {}
                        UserConfigSeen.UserEvent.ShareData -> {}
                        UserConfigSeen.UserEvent.Lock -> {}
                        UserConfigSeen.UserEvent.Unlock -> {}
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

}