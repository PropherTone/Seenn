package com.protone.seen

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Animatable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.appbar.AppBarLayout
import com.protone.api.animation.AnimationHelper
import com.protone.api.context.*
import com.protone.mediamodle.note.entity.RichMusicStates
import com.protone.mediamodle.note.entity.RichNoteStates
import com.protone.mediamodle.note.entity.RichPhotoStates
import com.protone.mediamodle.note.entity.RichVideoStates
import com.protone.mediamodle.note.spans.ISpanForUse
import com.protone.seen.databinding.NoteEditLayoutBinding
import com.protone.seen.popWindows.ColorfulPopWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteEditSeen(context: Context) : Seen<NoteEditSeen.NoteEditEvent>(context), ISpanForUse {

    enum class NoteEditEvent {
        Confirm,
        Finish,
        PickImage,
        PickVideo,
        PickMusic,
        PickIcon
    }

    private var listPopWindow: ColorfulPopWindow? = null
    private var numberPopWindow: ColorfulPopWindow? = null
    private var colorPopWindow: ColorfulPopWindow? = null

    var title: String
        set(value) {
            binding.noteEditTitle.setText(value)
        }
        get() = binding.noteEditTitle.text.toString()

    private val binding = NoteEditLayoutBinding.inflate(context.layoutInflater, context.root, true)

    override val viewRoot: View
        get() = binding.root

    override fun getToolBar(): View = binding.toolbar

    init {
        setNavigation()
        binding.noteEditTitle.requestFocus()
        binding.noteEditToolbar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                binding.toolbar.progress =
                    -verticalOffset / appBarLayout.totalScrollRange.toFloat()
            })
        binding.self = this
        (context as Activity).setSoftInputStatuesListener { height, isShow ->
            if (isShow) {
                binding.root.marginBottom(height)
            } else {
                binding.root.marginBottom(0)
            }
        }
        binding.noteEditRichNote.apply {
            isEditable = true
            setRichList(listOf(RichNoteStates("", arrayListOf())))
        }
    }

    suspend fun initEditor(richCode: Int, text: String) = withContext(Dispatchers.Main) {
        binding.noteEditRichNote.setRichList(richCode, text)
    }

    override fun offer(event: NoteEditEvent) {
        viewEvent.trySend(event)
    }

    override fun setBold() = binding.noteEditRichNote.setBold()

    override fun setItalic() = binding.noteEditRichNote.setItalic()

    override fun setSize() {
        if (numberPopWindow != null) {
            numberPopWindow?.dismiss()
            numberPopWindow = null
        } else ColorfulPopWindow(context).also {
            numberPopWindow = it
            it.setOnDismissListener { numberPopWindow = null }
        }.startNumberPickerPopup(binding.noteEditTool) { binding.noteEditRichNote.setSize(it) }
    }

    override fun setUnderlined() = binding.noteEditRichNote.setUnderlined()

    override fun setStrikethrough() = binding.noteEditRichNote.setStrikethrough()

    override fun insertImage() = offer(NoteEditEvent.PickImage)

    fun insertImage(photo: RichPhotoStates) = binding.noteEditRichNote.insertImage(photo)

    override fun insertVideo() = offer(NoteEditEvent.PickVideo)

    fun insertVideo(uri: Uri, list: MutableList<String>) {
        if (listPopWindow != null) {
            listPopWindow?.dismiss()
            listPopWindow = null
        } else ColorfulPopWindow(context).also {
            listPopWindow = it
            it.setOnDismissListener { listPopWindow = null }
        }.startListPopup(binding.noteEditTool, list) {
            listPopWindow?.dismiss()
            binding.noteEditRichNote.insertVideo(RichVideoStates(uri, it, name = ""))
        }

    }

    override fun insertMusic() = offer(NoteEditEvent.PickMusic)

    fun insertMusic(uri: Uri, list: MutableList<String>, title: String) {
        if (listPopWindow != null) {
            listPopWindow?.dismiss()
            listPopWindow = null
        } else ColorfulPopWindow(context).also {
            listPopWindow = it
            it.setOnDismissListener { listPopWindow = null }
        }.startListPopup(binding.noteEditTool, list) {
            listPopWindow?.dismiss()
            binding.noteEditRichNote.insertMusic(RichMusicStates(uri, it, title))
        }
    }

    suspend fun indexRichNote(): Pair<Int, String> = binding.noteEditRichNote.indexRichNote(title)

    override fun setColor() {
        if (colorPopWindow != null) {
            colorPopWindow?.dismiss()
            colorPopWindow = null
        } else ColorfulPopWindow(context).also {
            colorPopWindow = it
            it.setOnDismissListener { colorPopWindow = null }
        }.startColorPickerPopup(binding.noteEditTool) {
            binding.noteEditRichNote.setColor(String.format("#%06X", 0xFFFFFF and it))
        }
    }

    fun setNoteIcon(path:String) {
        Glide.with(context)
            .asDrawable()
            .load(path)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding.noteEditIcon)
    }

    fun setNoteIconCache(uri: Uri?) {
        Glide.with(context).asDrawable().load(uri).into(binding.noteEditIcon)
    }

    fun showProgress(isShow: Boolean) = context.onUiThread {
        (binding.toolbar.getViewById(R.id.noteEdit_progress) as ImageView?)?.apply {
            drawable.let {
                when (it) {
                    is Animatable ->
                        if (isShow) it.start().also { isVisible = true }
                        else it.stop().also { changeIconAni(binding.noteEditProgress) }
                }
            }
        }
    }

    private fun changeIconAni(view: ImageView) = context.onUiThread {
        AnimationHelper.apply {
            animatorSet(scaleX(view, 0f), scaleY(view, 0f), doOnEnd = {
                view.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.ic_baseline_check_24,
                        null
                    )
                )
                animatorSet(scaleX(view, 1f), scaleY(view, 1f), play = true, doOnEnd = {
                    alpha(view, 0f, play = true, doOnEnd = { view.isVisible = false })
                })
            }, play = true)
        }
    }

}