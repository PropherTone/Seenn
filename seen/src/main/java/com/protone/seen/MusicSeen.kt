package com.protone.seen

import android.animation.ObjectAnimator
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewTreeObserver
import com.protone.api.context.layoutInflater
import com.protone.api.context.root
import com.protone.seen.customView.StateImageView
import com.protone.seen.databinding.MusicLayoutBinding
import org.w3c.dom.Text

class MusicSeen(context: Context) : Seen<MusicSeen.Event>(context), StateImageView.StateListener,
    ViewTreeObserver.OnGlobalLayoutListener {

    enum class Event {
        AddBucket
    }

    private var containerAnimator: ObjectAnimator? = null
    val binding = MusicLayoutBinding.inflate(context.layoutInflater, context.root, true)

    override val viewRoot: View
        get() = binding.root

    var musicName: String = ""
        set(value) {
            binding.mySmallMusicPlayer.name = value
            field = value
        }


    var icon: Uri = Uri.parse("")
        set(value) {
            binding.mySmallMusicPlayer.icon = value
            field = value
        }

    init {
        binding
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    fun offer(event: Event){
        viewEvent.offer(event)
    }

    override fun onActive() {
        containerAnimator?.reverse()
    }

    override fun onNegative() {
        containerAnimator?.start()
    }

    override fun onGlobalLayout() {
        binding.musicBucketContainer.height.toFloat().let {
            containerAnimator = ObjectAnimator.ofFloat(
                binding.musicBucketContainer,
                "translationY",
                it - binding.mySmallMusicPlayer.height
            )
        }
        binding.musicShowBucket.setOnStateListener(this)

        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
}