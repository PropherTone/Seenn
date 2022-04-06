package com.protone.seen.customView

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.protone.api.context.layoutInflater
import com.protone.seen.R
import com.protone.seen.databinding.SmallMusicPlayerLayoutBinding

class MySmallMusicPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var oldX = 0f

    private val binding = SmallMusicPlayerLayoutBinding.inflate(context.layoutInflater, this, true)
    var isPlaying = false
        set(value) {
            Log.d("TAG", ": $value")
            if (value) binding.smallMusicStart.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_baseline_pause_24,
                    null
                )
            ) else binding.smallMusicStart.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_baseline_play_arrow_24,
                    null
                )
            )
            field = value
        }

    var name: String = ""
        get() {
            return binding.smallMusicName.text.toString()
        }
        set(value) {
            binding.smallMusicName.apply {
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
                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(context, value)
                loadAlbum(mediaMetadataRetriever.embeddedPicture)
                field = value
            }
        }

    init {
        binding.smallMusicStart.setOnClickListener {
            if (icon == Uri.EMPTY) return@setOnClickListener
            if (!isPlaying) playMusic() else pauseMusic()
            isPlaying = !isPlaying
        }

        loadAlbum(null)
    }

    var playMusic: () -> Unit = {}
    var pauseMusic: () -> Unit = {}

    private fun loadAlbum(embeddedPicture: ByteArray?) {
        Glide.with(binding.smallMusicIcon.context).load(embeddedPicture)
            .error(R.drawable.ic_baseline_music_note_24)
            .circleCrop().transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.smallMusicIcon)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                oldX = event.x
            }

            MotionEvent.ACTION_MOVE -> {

            }

            MotionEvent.ACTION_UP -> {
                oldX = event.x
            }
        }


        return super.onTouchEvent(event)
    }
}