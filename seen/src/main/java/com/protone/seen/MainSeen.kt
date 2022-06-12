package com.protone.seen

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.api.toDrawable
import com.protone.api.todayDate
import com.protone.seen.adapter.MainModelListAdapter
import com.protone.seen.databinding.MainLayoutBinding
import com.protone.seen.itemDecoration.ModelListItemDecoration

class MainSeen(context: Context) : Seen<MainSeen.Touch>(context),
    ViewTreeObserver.OnGlobalLayoutListener {

    enum class Touch {
        MUSIC,
        NOTE,
        GALLEY,
        ConfigUser
    }

    private val binding = MainLayoutBinding.inflate(context.layoutInflater, context.root, false)

    val musicController = binding.musicPlayer

    var userName: String = ""
        set(value) {
            binding.userWelcome.text =
                if (value == "") context.getString(R.string.welcome_msg) else value
            binding.userDate.text = todayDate("yyyy/MM/dd")
            field = value
        }
        get() = binding.userWelcome.text.toString()

    var userIcon: String = ""
        set(value) {
            value.toDrawable(context) {
                if (it != null) {
                    binding.userIcon.background = it
                }
            }
            field = value
        }

    val group: ViewGroup
        get() = binding.mainGroup

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.mainGroup

    private var btnY = 0f

    private val btnH = context.resources.getDimensionPixelSize(R.dimen.action_icon_p)

    init {
        binding.apply {
            self = this@MainSeen
            root.viewTreeObserver.addOnGlobalLayoutListener(this@MainSeen)
            musicPlayer.apply {
                duration
            }
        }

    }

    fun refreshModelList() {
        binding.modelList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MainModelListAdapter(context)
            addItemDecoration(ModelListItemDecoration(context.resources.getDimensionPixelSize(R.dimen.main_margin)))
        }
    }

    override fun offer(event: Touch) {
        viewEvent.offer(event)
    }

    override fun onGlobalLayout() {
        binding.apply {
            actionBtnContainer.also {
                it.y = it.y + btnH * 2
                btnY = it.y
            }
            toolbar.addOnOffsetChangedListener(
                AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                    binding.toolMotion.progress =
                        -verticalOffset / appBarLayout.totalScrollRange.toFloat().also {
                            binding.musicPlayer.isVisible = it > 0.7f
                            binding.actionBtnContainer.also { btn ->
                                btn.y = btnY - (btnH * binding.toolMotion.progress) * 2
                            }
                        }
                })
            root.viewTreeObserver.removeOnGlobalLayoutListener(this@MainSeen)
        }
    }


}