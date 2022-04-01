package com.protone.seen.customView

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.TextUtils
import android.transition.TransitionManager
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.protone.api.context.layoutInflater
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.json.toUri
import com.protone.api.toBitmapByteArray
import com.protone.seen.Progress
import com.protone.seen.R
import com.protone.seen.customView.DragBar.DragBar
import com.protone.seen.databinding.MusicplayerLayoutBinding

class MyMusicPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = MusicplayerLayoutBinding.inflate(context.layoutInflater, this, true)
    var isPlaying = false
        set(value) {
            if (value) binding.musicControl.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_baseline_pause_24,
                    null
                )
            ) else binding.musicControl.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_baseline_play_arrow_24,
                    null
                )
            )
            field = value
        }

    var seekTo: Progress? = null
        set(value) {
            binding.musicProgress.progressListener = value
            field = value
        }

    var progress: Long = 0
        set(value) {
            binding.musicProgress.barSeekTo(value)
            field = value
        }

    var duration: Long = 0
        set(value) {
            binding.musicProgress.barDuration = value
            field = value
        }

    var name: String = ""
        get() {
            return binding.musicName.text.toString()
        }
        set(value) {
            binding.musicName.apply {
                text = value
                ellipsize = TextUtils.TruncateAt.MARQUEE
                isSingleLine = true
                isSelected = true
                isFocusable = true
                isFocusableInTouchMode = true
            }
            field = value
        }

    var icon: Uri = Uri.EMPTY
        set(value) {
            if (value != Uri.EMPTY) {
                loadAlbum(value.toBitmapByteArray())
                field = value
            }
        }

    init {
        binding.apply {
            musicControl.setOnClickListener {
                if (icon == Uri.EMPTY) return@setOnClickListener
                if (!isPlaying) playMusic() else pauseMusic()
                isPlaying = !isPlaying
            }
            musicPrevious.setOnClickListener {
                if (icon == Uri.EMPTY) return@setOnClickListener
                isPlaying = true
                musicPrevious()
            }
            musicNext.setOnClickListener {
                if (icon == Uri.EMPTY) return@setOnClickListener
                isPlaying = true
                musicNext()
            }
        }
        loadAlbum(null)
    }

    var playMusic: () -> Unit = {}
    var pauseMusic: () -> Unit = {}
    var musicPrevious: () -> Unit = {}
    var musicNext: () -> Unit = {}

    private fun loadAlbum(embeddedPicture: ByteArray?) {
        Glide.with(binding.musicIcon.context).load(embeddedPicture)
            .error(R.drawable.ic_baseline_music_note_24)
            .circleCrop().transition(withCrossFade())
            .into(binding.musicIcon)
    }
}