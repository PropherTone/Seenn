package com.protone.seen

import android.content.Context
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.protone.api.Config
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.UserConfigItemLayoutBinding
import com.protone.seen.databinding.UserConfigLayoutBinding

class UserConfigSeen(context: Context) : Seen<UserConfigSeen.UserEvent>(context) {

    enum class UserEvent {
        Login,
        Icon,
        Name,
        PassWord,
        ShareNote,
        ShareData,
        Lock,
        Unlock
    }

    enum class DisplayMode {
        UnRegis,
        Locked,
        Normal
    }

    private val binding =
        UserConfigLayoutBinding.inflate(context.layoutInflater, context.root, false)

    override val viewRoot: View get() = binding.root

    override fun getToolBar(): View? = null

    override fun offer(event: UserEvent) {
        viewEvent.offer(event)
    }

    init {
        setNavigation()
        binding.self = this
    }

    fun clear(){
        binding.userRoot.removeAllViews()
    }

    fun chooseMode(mode: DisplayMode) {
        TransitionManager.beginDelayedTransition(binding.userRoot)
        if (mode == DisplayMode.UnRegis) {
            UserConfigItemLayoutBinding.inflate(context.layoutInflater, context.root, false)
                .apply {
                    itemName.text = context.getString(R.string.login)
                    itemName.setOnClickListener { offer(UserEvent.Login) }
                }.root.run {
                    layoutParams = ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Config.screenHeight
                    )
                    binding.userRoot.addView(this)
                }
            return
        }
        val views = mutableListOf<View>(
            initModeView("修改头像", UserEvent.Icon),
            initModeView("修改名称", UserEvent.Name),
            initModeView("修改密码", UserEvent.PassWord),
            initModeView("笔记共享", UserEvent.ShareNote),
            initModeView("数据共享", UserEvent.ShareData),
            initModeView("模块加密", UserEvent.Lock)
        )
        if (mode == DisplayMode.Locked) views.add(initModeView("模块解锁", UserEvent.Unlock))
        views.forEach {
            binding.userRoot.addView(it)
        }
    }

    private fun initModeView(name: String, event: UserEvent) =
        UserConfigItemLayoutBinding.inflate(context.layoutInflater, context.root, false)
            .apply {
                itemName.text = name
                itemName.setOnClickListener { offer(event) }
            }.root

}