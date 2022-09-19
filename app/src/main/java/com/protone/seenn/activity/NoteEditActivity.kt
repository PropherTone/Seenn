package com.protone.seenn.activity

import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.protone.api.animation.AnimationHelper
import com.protone.api.baseType.*
import com.protone.api.context.*
import com.protone.api.entity.*
import com.protone.api.json.listToJson
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.api.json.toUri
import com.protone.seenn.R
import com.protone.seenn.databinding.NoteEditActivityBinding
import com.protone.ui.customView.richText.note.spans.ISpanForUse
import com.protone.ui.dialog.imageListDialog
import com.protone.ui.popWindows.ColorfulPopWindow
import com.protone.worker.viewModel.GalleyViewModel
import com.protone.worker.viewModel.NoteEditViewModel
import com.protone.worker.viewModel.NoteViewViewModel
import com.protone.worker.viewModel.PickMusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class NoteEditActivity :
    BaseActivity<NoteEditActivityBinding, NoteEditViewModel, NoteEditViewModel.NoteEvent>(true),
    ISpanForUse {
    override val viewModel: NoteEditViewModel by viewModels()

    private var listPopWindow: ColorfulPopWindow? = null
    private var numberPopWindow: ColorfulPopWindow? = null
    private var colorPopWindow: ColorfulPopWindow? = null

    private var title: String
        set(value) {
            binding.noteEditTitle.setText(value)
        }
        get() = binding.noteEditTitle.text.toString()

    override fun createView(): View {
        binding = NoteEditActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        binding.noteEditToolbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.toolbar.progress =
                -verticalOffset / appBarLayout.totalScrollRange.toFloat()
        }
        setSoftInputStatusListener { height, isShow ->
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
        return binding.root
    }

    override suspend fun NoteEditViewModel.init() {
        val contentTitle = intent.getStringExtra(NoteEditViewModel.CONTENT_TITLE)
        noteName = intent.getStringExtra(NoteEditViewModel.NOTE)
        if (contentTitle != null) {
            title = contentTitle
            initEditor(
                1,
                mutableListOf(
                    RichNoteSer(
                        try {
                            getGainData<String>()
                        } catch (e: Exception) {
                            null
                        } ?: R.string.none.getString(),
                        arrayListOf<SpanStates>().listToJson(SpanStates::class.java)
                    ).toJson()
                ).listToJson(String::class.java)
            )
        } else {
            noteByName = noteName?.let {
                getNoteByName(it)?.let { n ->
                    withContext(Dispatchers.IO) {
                        if (n.imagePath == null) return@withContext
                        val file = File(n.imagePath)
                        if (file.isFile) {
                            setNoteIcon(n.imagePath)
                        } else {
                            iconUri = n.imagePath.toUri()
                            setNoteIcon(iconUri)
                        }
                    }
                    title = n.title
                    initEditor(n.getRichCode(), n.getText())
                    onEdit = true
                    n
                }
            }
        }

        onViewEvent {
            when (it) {
                NoteEditViewModel.NoteEvent.Confirm -> confirm()
                NoteEditViewModel.NoteEvent.PickIcon -> pickIcon()
                NoteEditViewModel.NoteEvent.PickImage -> pickImage()
                NoteEditViewModel.NoteEvent.PickVideo -> pickVideo()
                NoteEditViewModel.NoteEvent.PickMusic -> pickMusic()
            }
        }
    }

    override suspend fun doResume() {
        binding.root.marginBottom(0)
        isKeyBroadShow = false
    }

    private suspend fun NoteEditViewModel.pickIcon() {
        startGalleyPick(true)?.let { re ->
            iconUri = re.uri
            setNoteIcon(re.uri)
        }
    }

    private suspend fun pickVideo() {
        startGalleyPick(false)?.let { re ->
            insertVideo(re.uri)
        }
    }

    private suspend fun NoteEditViewModel.pickMusic() {
        startActivityForResult(
            PickMusicActivity::class.intent.apply {
                putExtra(PickMusicViewModel.MODE, PickMusicViewModel.PICK_MUSIC)
            }
        )?.also { re ->
            re.data?.data?.let { uri ->
                if (allNote == null) allNote = getAllNote()
                insertMusic(uri, allNote!!, getMusicTitle(uri))
            }
        }
    }

    private suspend fun NoteEditViewModel.pickImage() {
        startGalleyPick(true)?.let { re ->
            insertImage(
                RichPhotoStates(
                    re.uri, re.name, null,
                    re.date.toDateString().toString()
                )
            )
            medias.add(re)
        }
    }

    private suspend fun NoteEditViewModel.confirm() {
        if (title.isEmpty()) {
            R.string.enter_title.getString().toast()
            return
        }
        showProgress(true)
        val checkedTitle = if (onEdit) title else checkNoteTitle(title)
        val indexedRichNote = binding.noteEditRichNote.indexRichNote(checkedTitle) {
            if (it.size <= 0) return@indexRichNote true
            return@indexRichNote imageListDialog(it)
        }
        val note = Note(
            checkedTitle,
            indexedRichNote.second,
            null,
            System.currentTimeMillis(),
            indexedRichNote.first
        )
        if (onEdit) {
            if (intent.getStringExtra(NoteEditViewModel.NOTE) == null) {
                setResult(RESULT_CANCELED)
                finish()
                return
            }
            val inNote = noteByName
            if (inNote == null) {
                setResult(RESULT_CANCELED)
                finish()
                return
            }
            copyNote(inNote, note)
            inNote.imagePath = if (iconUri != null) saveIcon(checkedTitle) else inNote.imagePath
            val re = updateNote(inNote)
            if (re == null && re == -1) {
                insertNote(
                    inNote,
                    intent.getStringExtra(NoteEditViewModel.NOTE_DIR)
                ).let { result ->
                    if (result) {
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(NoteViewViewModel.NOTE_NAME, inNote.title)
                        )
                        finish()
                    } else R.string.failed_msg.getString().toast()
                }
            } else {
                showProgress(false)
                setResult(RESULT_OK)
                finish()
            }
        } else if (insertNote(
                note.apply { saveIcon(checkedTitle)?.let { imagePath = it } },
                intent.getStringExtra(NoteEditViewModel.NOTE_DIR)
            )
        ) {
            finish()
        } else R.string.failed_msg.getString().toast()
    }

    private suspend fun startGalleyPick(isPhoto: Boolean) =
        startActivityForResult(GalleyActivity::class.intent.apply {
            putExtra(
                GalleyViewModel.CHOOSE_MODE,
                if (isPhoto) GalleyViewModel.CHOOSE_PHOTO else GalleyViewModel.CHOOSE_VIDEO
            )
        })?.let { re ->
            re.data?.getStringExtra(GalleyViewModel.GALLEY_DATA)?.toEntity(GalleyMedia::class.java)
        }


    private suspend fun initEditor(richCode: Int, text: String) = withContext(Dispatchers.Main) {
        binding.noteEditRichNote.setRichList(richCode, text)
    }

    override fun setBold() = binding.noteEditRichNote.setBold()

    override fun setItalic() = binding.noteEditRichNote.setItalic()

    override fun setSize() {
        if (numberPopWindow != null) {
            numberPopWindow?.dismiss()
            numberPopWindow = null
        } else ColorfulPopWindow(this).also {
            numberPopWindow = it
            it.setOnDismissListener { numberPopWindow = null }
        }.startNumberPickerPopup(binding.noteEditTool) { binding.noteEditRichNote.setSize(it) }
    }

    private fun insertImage(photo: RichPhotoStates) = binding.noteEditRichNote.insertImage(photo)

    private fun insertVideo(uri: Uri) {
        binding.noteEditRichNote.insertVideo(RichVideoStates(uri, null, name = ""))
    }

    private fun insertMusic(uri: Uri, list: MutableList<String>, title: String) {
        if (listPopWindow != null) {
            listPopWindow?.dismiss()
            listPopWindow = null
        } else ColorfulPopWindow(this).also {
            listPopWindow = it
            it.setOnDismissListener { listPopWindow = null }
        }.startListPopup(R.string.pick_note.getString(), binding.noteEditTool, list) {
            listPopWindow?.dismiss()
            binding.noteEditRichNote.insertMusic(RichMusicStates(uri, it, title))
        }
    }

    private suspend fun setNoteIcon(uri: Uri?) = withContext(Dispatchers.Main) {
        Glide.with(this@NoteEditActivity).asDrawable().load(uri).into(binding.noteEditIcon)
    }

    private suspend fun setNoteIcon(path: String?) = withContext(Dispatchers.Main) {
        Glide.with(this@NoteEditActivity).asDrawable().load(path).into(binding.noteEditIcon)
    }

    private fun showProgress(isShow: Boolean) = onUiThread {
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

    private fun changeIconAni(view: ImageView) = onUiThread {
        AnimationHelper.apply {
            animatorSet(scaleX(view, 0f), scaleY(view, 0f), doOnEnd = {
                view.setImageDrawable(R.drawable.ic_baseline_check_24.getDrawable())
                animatorSet(scaleX(view, 1f), scaleY(view, 1f), play = true, doOnEnd = {
                    alpha(view, 0f, play = true, doOnEnd = { view.isVisible = false })
                })
            }, play = true)
        }
    }

    override fun setUnderlined() = binding.noteEditRichNote.setUnderlined()
    override fun setStrikethrough() = binding.noteEditRichNote.setStrikethrough()
    override fun insertImage() = Unit
    override fun insertVideo() = Unit
    override fun insertMusic() = Unit
    override fun setColor() {
        if (colorPopWindow != null) {
            colorPopWindow?.dismiss()
            colorPopWindow = null
        } else ColorfulPopWindow(this).also {
            colorPopWindow = it
            it.setOnDismissListener { colorPopWindow = null }
        }.startColorPickerPopup(binding.noteEditTool) {
            binding.noteEditRichNote.setColor(it.toHexColor())
        }
    }
}