package com.protone.seen

import android.content.Context
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
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
        Note,
        ShareNote,
        Data,
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
        binding.root.removeAllViews()
    }

    fun chooseMode(mode: DisplayMode) {
        TransitionManager.beginDelayedTransition(binding.root)
        if (mode == DisplayMode.UnRegis) {
            UserConfigItemLayoutBinding.inflate(context.layoutInflater, context.root, false)
                .apply {
                    itemName.text = context.getString(R.string.login)
                    itemName.setOnClickListener { offer(UserEvent.Login) }
                }.root.run {
                    layoutParams = ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    binding.root.addView(this)
                }
        }
        val views = mutableListOf<View>(
            initModeView("修改头像", UserEvent.Icon),
            initModeView("修改名称", UserEvent.Name),
            initModeView("修改密码", UserEvent.PassWord),
            initModeView("笔记", UserEvent.Note),
            initModeView("分享笔记", UserEvent.ShareNote),
            initModeView("数据", UserEvent.Data),
            initModeView("分享数据", UserEvent.ShareData),
            initModeView("模块加密", UserEvent.Lock)
        )
        if (mode == DisplayMode.Locked) views.add(initModeView("模块解锁", UserEvent.Unlock))
        views.forEach {
            binding.root.addView(it)
        }
    }

    private fun initModeView(name: String, event: UserEvent) =
        UserConfigItemLayoutBinding.inflate(context.layoutInflater, context.root, false)
            .apply {
                itemName.text = name
                itemName.setOnClickListener { offer(event) }
            }.root

}