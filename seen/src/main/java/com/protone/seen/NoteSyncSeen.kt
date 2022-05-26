package com.protone.seen

import android.content.Context
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.databinding.AutoMusicPlayerLayoutBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NoteSyncSeen(context: Context) : Seen<NoteSyncSeen.NoteSync>(context) {

    enum class NoteSync{
        Send,
        Receive
    }

    private val binding = AutoMusicPlayerLayoutBinding.inflate(context.layoutInflater,context.root,true)

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.root

    init {
        initToolBar()
        binding.musicControl.setOnClickListener {
            launch {
                repeat(100) {
                    (binding.root as MotionLayout).progress = it.toFloat() / 100f
                    delay(100)
                }
            }
        }
//        binding.self = this
    }

    override fun offer(event : NoteSync){
        viewEvent.offer(event)
    }
}