package com.protone.seenn.activity

import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.appbar.AppBarLayout
import com.protone.api.animation.AnimationHelper
import com.protone.api.baseType.*
import com.protone.api.context.*
import com.protone.api.json.listToJson
import com.protone.api.json.toEntity
import com.protone.api.json.toJson
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.database.room.entity.GalleyMedia
import com.protone.database.room.entity.Note
import com.protone.mediamodle.note.entity.*
import com.protone.mediamodle.note.spans.ISpanForUse
import com.protone.seen.popWindows.ColorfulPopWindow
import com.protone.seenn.R
import com.protone.seenn.databinding.NoteEditActivityBinding
import com.protone.seenn.viewModel.GalleyViewModel
import com.protone.seenn.viewModel.NoteEditViewModel
import com.protone.seenn.viewModel.NoteViewViewModel
import com.protone.seenn.viewModel.PickMusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteEditActivity : BaseActivity<NoteEditActivityBinding, NoteEditViewModel>(true),
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

    override fun initView() {
        binding = NoteEditActivityBinding.inflate(layoutInflater, root, false)
        binding.activity = this
        fitNavigationBarUsePadding(binding.root)
        binding.noteEditTitle.requestFocus()
        binding.noteEditToolbar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                binding.toolbar.progress =
                    -verticalOffset / appBarLayout.totalScrollRange.toFloat()
            })
        setSoftInputStatuesListener { height, isShow ->
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
                    title = n.title
                    initEditor(n.getRichCode(), n.getText())
                    onEdit = true
                    n
                }
            }
        }

    }

    override suspend fun onViewEvent(event: String) {
        when (event) {
            NoteEditViewModel.ViewEvent.Confirm.name -> confirm()
            NoteEditViewModel.ViewEvent.PickIcon.name -> pickIcon()
            NoteEditViewModel.ViewEvent.PickImage.name -> pickImage()
            NoteEditViewModel.ViewEvent.PickVideo.name -> pickVideo()
            NoteEditViewModel.ViewEvent.PickMusic.name -> pickMusic()
        }
    }

    fun sendConfirm() {
        sendViewEvent(NoteEditViewModel.ViewEvent.Confirm.name)
    }

    fun sendPickIcon() {
        sendViewEvent(NoteEditViewModel.ViewEvent.PickIcon.name)
    }

    fun sendPickImage() {
        sendViewEvent(NoteEditViewModel.ViewEvent.PickImage.name)
    }

    fun sendPickVideo() {
        sendViewEvent(NoteEditViewModel.ViewEvent.PickVideo.name)
    }

    fun sendPickMusic() {
        sendViewEvent(NoteEditViewModel.ViewEvent.PickMusic.name)
    }

    private suspend fun pickIcon() = viewModel.apply {
        startGalleyPick(true)?.let { re ->
            iconUri = re.uri
            setNoteIconCache(re.uri)
            showProgress(true)
            savedIconPath = if (iconUri != null) {
                val saveIcon = saveIcon(title) {
                    setNoteIcon(savedIconPath)
                }
                showProgress(false)
                saveIcon
            } else ""
        }
    }

    private suspend fun pickVideo() = viewModel.apply {
        startGalleyPick(false)?.let { re ->
            if (allNote == null) allNote = getAllNote()
            insertVideo(re.uri, allNote!!)
        }
    }

    private suspend fun pickMusic() = viewModel.apply {
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

    private suspend fun pickImage() = viewModel.apply {
        startGalleyPick(true)?.let { re ->
            insertImage(
                RichPhotoStates(
                    re.uri, re.name, null,
                    re.date.toDateString().toString()
                )
            )
            re.apply {
                if (notes == null) notes = mutableListOf()
                (notes as MutableList<String>).add(title)
            }
            withContext(Dispatchers.IO) {
                DataBaseDAOHelper.updateSignedMedia(re)
            }
        }
    }

    private suspend fun confirm() = viewModel.apply {
        if (title.isEmpty()) {
            R.string.enter_title.getString().toast()
            return@apply
        }
        val indexedRichNote = indexRichNote()
        showProgress(true)
        val note = Note(
            title,
            indexedRichNote.second,
            savedIconPath,
            System.currentTimeMillis(),
            mutableListOf(intent.getStringExtra(NoteEditViewModel.NOTE_TYPE)),
            indexedRichNote.first
        )
        if (onEdit) {
            if (intent.getStringExtra(NoteEditViewModel.NOTE) == null) {
                setResult(RESULT_CANCELED)
                finish()
                return@apply
            }
            val inNote = noteByName
            if (inNote == null) {
                setResult(RESULT_CANCELED)
                finish()
                return@apply
            }
            copyNote(inNote, note)
            val re = updateNote(inNote)
            if (re == null && re == -1) {
                insertNote(inNote).let { result ->
                    if (result) {
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(NoteViewViewModel.NOTE_NAME, inNote.title)
                        )
                        finish()
                    } else R.string.failed_msg.getString().toast()
                }
            } else {
                setResult(RESULT_OK)
                finish()
            }
        } else {
            if (insertNote(note)) finish()
            else R.string.failed_msg.getString().toast()
        }
    }

    private suspend fun startGalleyPick(isPhoto: Boolean) = withContext(Dispatchers.IO) {
        startActivityForResult(GalleyActivity::class.intent.apply {
            putExtra(
                GalleyViewModel.CHOOSE_MODE,
                if (isPhoto) GalleyViewModel.CHOOSE_PHOTO else GalleyViewModel.CHOOSE_VIDEO
            )
        })?.let { re ->
            re.data?.getStringExtra(GalleyViewModel.GALLEY_DATA)?.toEntity(GalleyMedia::class.java)
        }
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

    private fun insertVideo(uri: Uri, list: MutableList<String>) {
        if (listPopWindow != null) {
            listPopWindow?.dismiss()
            listPopWindow = null
        } else ColorfulPopWindow(this).also {
            listPopWindow = it
            it.setOnDismissListener { listPopWindow = null }
        }.startListPopup(binding.noteEditTool, list) {
            listPopWindow?.dismiss()
            binding.noteEditRichNote.insertVideo(RichVideoStates(uri, it, name = ""))
        }

    }

    private fun insertMusic(uri: Uri, list: MutableList<String>, title: String) {
        if (listPopWindow != null) {
            listPopWindow?.dismiss()
            listPopWindow = null
        } else ColorfulPopWindow(this).also {
            listPopWindow = it
            it.setOnDismissListener { listPopWindow = null }
        }.startListPopup(binding.noteEditTool, list) {
            listPopWindow?.dismiss()
            binding.noteEditRichNote.insertMusic(RichMusicStates(uri, it, title))
        }
    }

    private suspend fun indexRichNote(): Pair<Int, String> =
        binding.noteEditRichNote.indexRichNote(title)

    private fun setNoteIcon(path: String) {
        Glide.with(this)
            .asDrawable()
            .load(path)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding.noteEditIcon)
    }

    private fun setNoteIconCache(uri: Uri?) {
        Glide.with(this).asDrawable().load(uri).into(binding.noteEditIcon)
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