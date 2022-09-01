package com.protone.worker.database

import android.content.Context
import android.net.Uri
import com.protone.api.baseType.getString
import com.protone.api.baseType.toast
import com.protone.api.entity.*
import com.protone.api.onResult
import com.protone.database.R
import com.protone.database.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.LinkedBlockingDeque

class DatabaseHelper {

    private val executorService by lazy { CoroutineScope(Dispatchers.IO) }
    fun getScope() = executorService

    private val _mediaNotifier = MutableSharedFlow<MediaAction>()
    val mediaNotifier = _mediaNotifier.asSharedFlow()

    companion object {
        @JvmStatic
        val instance: DatabaseHelper
            @Synchronized get() {
                if (helperImpl == null) {
                    synchronized(this::class) {
                        helperImpl = DatabaseHelper()
                    }
                }
                return helperImpl!!
            }

        private var helperImpl: DatabaseHelper? = null
    }

    val musicBucketDAOBridge by lazy { MusicBucketDAOBridge() }
    val musicDAOBridge by lazy { MusicDAOBridge() }
    val signedGalleyDAOBridge by lazy { GalleyDAOBridge() }
    val noteDAOBridge by lazy { NoteDAOBridge() }
    val noteDirDAOBridge by lazy { NoteTypeDAOBridge() }
    val galleyBucketDAOBridge by lazy { GalleyBucketDAOBridge() }
    val galleriesWithNotesDAOBridge by lazy { GalleriesWithNotesDAOBridge() }
    val noteDirWithNoteDAOBridge by lazy { NoteDirWithNoteDAOBridge() }
    val musicWithMusicBucketDAOBridge by lazy { MusicWithMusicBucketDAOBridge() }

    fun showDataBase(context: Context) {
        showRoomDB(context)
    }

    fun shutdownNow() {
        if (executorService.isActive) {
            executorService.cancel()
        }
    }

    suspend fun pollEvent(queue: LinkedBlockingDeque<MediaAction>) {
        while (queue.isNotEmpty()) queue.poll()?.let { _mediaNotifier.emit(it) }
    }

    inline fun execute(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        crossinline runnable: suspend () -> Unit
    ): Job = getScope().launch(dispatcher) {
        try {
            runnable.invoke()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            R.string.unknown_error.getString().toast()
        } finally {
            cancel()
        }
    }

    inner class MusicBucketDAOBridge : BaseMusicBucketDAO() {

        override fun startEvent() {
            dataEvent = this@DatabaseHelper.execute(Dispatchers.Default) {
                observeAllMusicBucket().buffer().collect {
                    this@DatabaseHelper.pollEvent(getDeque())
                }
            }
            dataEvent?.start()
        }

        fun updateMusicBucketAsync(bucket: MusicBucket) {
            execute {
                updateMusicBucket(bucket)
            }
        }

        fun addMusicBucketAsync(musicBucket: MusicBucket) {
            execute {
                addMusicBucket(musicBucket)
            }
        }

        fun deleteMusicBucketAsync(bucket: MusicBucket) {
            execute {
                deleteMusicBucket(bucket)
            }
        }

        suspend fun getMusicBucketByNameRs(name: String): MusicBucket? = onResult {
            it.resumeWith(Result.success(getMusicBucketByName(name)))
        }

        suspend fun getAllMusicBucketRs() = onResult {
            it.resumeWith(Result.success(getAllMusicBucket()))
        }

        suspend fun updateMusicBucketRs(bucket: MusicBucket) = onResult {
            it.resumeWith(Result.success(updateMusicBucket(bucket)))
        }

        suspend fun deleteMusicBucketRs(bucket: MusicBucket) = onResult {
            deleteMusicBucket(bucket)
            it.resumeWith(Result.success(getMusicBucketByName(bucket.name) == null))
        }

        inline fun addMusicBucketWithCallBack(
            musicBucket: MusicBucket,
            crossinline callBack: suspend (result: Boolean, name: String) -> Unit
        ) {
            execute {
                var count = 0
                val tempName = musicBucket.name
                val names = mutableMapOf<String, Int>()
                getAllMusicBucket()?.forEach {
                    names[it.name] = 1
                    if (it.name == musicBucket.name) {
                        musicBucket.name = "${tempName}(${++count})"
                    }
                }
                while (names[musicBucket.name] != null) {
                    musicBucket.name = "${tempName}(${++count})"
                }
                addMusicBucket(musicBucket)
                callBack(getMusicBucketByName(musicBucket.name) != null, musicBucket.name)
            }
        }

    }

    inner class MusicDAOBridge : BaseMusicDAO() {

        override fun startEvent() {

        }

        fun insertMusicMultiAsync(music: List<Music>) {
            execute {
                music.forEach {
                    insertMusic(it)
                }
            }
        }


        fun insertMusicMulti(music: List<Music>) {
            music.forEach {
                insertMusic(it)
            }
        }

        fun deleteMusicMultiAsync(music: List<Music>) {
            if (music.isEmpty()) return
            execute {
                music.forEach {
                    deleteMusic(it)
                }
            }
        }

        fun deleteMusicMulti(music: List<Music>) {
            if (music.isEmpty()) return
            music.forEach {
                deleteMusic(it)
            }
        }

        fun insertMusicCheck(music: Music) = execute {
            getMusicByUri(music.uri).let {
                if (it == null) {
                    insertMusic(music)
                } else {
                    updateMusic(music)
                }
            }
        }

        fun deleteMusicAsync(music: Music) = execute {
            deleteMusic(music)
        }

        suspend fun getAllMusicRs() = onResult {
            execute {
                it.resumeWith(Result.success(getAllMusic()))
            }
        }

        suspend fun updateMusicRs(music: Music) = onResult {
            it.resumeWith(Result.success(updateMusic(music)))

        }

    }

    inner class GalleyDAOBridge : BaseGalleyDAO() {

        override fun startEvent() {
            dataEvent = this@DatabaseHelper.execute(Dispatchers.Default) {
                observeAllSignedMedia().buffer().collect {
                    this@DatabaseHelper.pollEvent(getDeque())
                }
            }
            dataEvent?.start()
        }

        fun deleteSignedMediaMultiAsync(list: MutableList<GalleyMedia>) {
            execute {
                list.forEach {
                    deleteSignedMediaByUri(it.uri)
                }
            }
        }

        fun deleteSignedMediaAsync(media: GalleyMedia) {
            execute {
                deleteSignedMedia(media)
            }
        }

        fun updateMediaMultiAsync(list: MutableList<GalleyMedia>) {
            execute { list.forEach { updateSignedMedia(it) } }
        }

        fun insertSignedMediaChecked(media: GalleyMedia): GalleyMedia? {
            val it = getSignedMedia(media.uri)
            return if (it != null) {
                if (it == media) return null
                updateSignedMedia(it)
                it
            } else {
                insertSignedMedia(media)
                media
            }
        }

        suspend fun getAllSignedMediaRs() = onResult {
            it.resumeWith(Result.success(getAllSignedMedia()))
        }

        suspend fun getAllMediaByTypeRs(isVideo: Boolean) = onResult {
            it.resumeWith(Result.success(getAllMediaByType(isVideo)))
        }

        suspend fun getSignedMediaRs(uri: Uri) = onResult {
            it.resumeWith(Result.success(getSignedMedia(uri)))
        }

    }

    inner class NoteDAOBridge : BaseNoteDAO() {

        override fun startEvent() {
            dataEvent = this@DatabaseHelper.execute(Dispatchers.Default) {
                observeAllNote().buffer().collect {
                    this@DatabaseHelper.pollEvent(getDeque())
                }
            }
            dataEvent?.start()
        }

        fun deleteNoteAsync(note: Note) {
            execute {
                deleteNote(note)
            }
        }

        suspend fun insertNoteRs(note: Note) =
            onResult {
                val id = insertNote(note)
                it.resumeWith(Result.success(Pair(getNoteByName(note.title) != null, id)))
            }
    }

    inner class NoteTypeDAOBridge : BaseNoteTypeDAO() {

        override fun startEvent() {

        }

        suspend fun insertNoteDirRs(
            noteDir: NoteDir,
        ) = onResult<Pair<Boolean, String>> {
            var count = 0
            val tempName = noteDir.name
            val names = mutableMapOf<String, Int>()
            getALLNoteDir()?.forEach { dir ->
                names[dir.name] = 1
                if (dir.name == noteDir.name) {
                    noteDir.name = "${tempName}(${++count})"
                }
            }
            while (names[noteDir.name] != null) {
                noteDir.name = "${tempName}(${++count})"
            }
            insertNoteDir(noteDir)
            it.resumeWith(Result.success(Pair(getNoteDir(noteDir.name) != null, noteDir.name)))
        }

        suspend fun doDeleteNoteDirRs(noteDir: NoteDir) = onResult {
            deleteNoteDir(noteDir)
            it.resumeWith(Result.success(getNoteDir(noteDir.name) != null))
        }

    }

    inner class GalleyBucketDAOBridge : BaseGalleyBucketDAO() {

        override fun startEvent() {

        }

        fun deleteGalleyBucketAsync(galleyBucket: GalleyBucket) {
            execute {
                deleteGalleyBucket(galleyBucket)
            }
        }

        suspend fun getGalleyBucketRs(name: String) = onResult {
            it.resumeWith(Result.success(getGalleyBucket(name)))
        }

        inline fun insertGalleyBucketCB(
            galleyBucket: GalleyBucket,
            crossinline callBack: suspend (Boolean, String) -> Unit
        ) {
            execute {
                var count = 0
                val tempName = galleyBucket.type
                val names = mutableMapOf<String, Int>()
                getALLGalleyBucket(galleyBucket.isImage)?.forEach {
                    names[it.type] = 1
                    if (it.type == galleyBucket.type) {
                        galleyBucket.type = "${tempName}(${++count})"
                    }
                }
                while (names[galleyBucket.type] != null) {
                    galleyBucket.type = "${tempName}(${++count})"
                }
                insertGalleyBucket(galleyBucket)
                callBack.invoke(getGalleyBucket(galleyBucket.type) != null, galleyBucket.type)
            }
        }

    }

    inner class MusicWithMusicBucketDAOBridge : BaseMusicWithMusicBucketDAO() {

        override fun startEvent() {
            this@DatabaseHelper.execute(Dispatchers.Default) {
                observeAllMusicBucketWithMusic().buffer().collect {
                    this@DatabaseHelper.pollEvent(getDeque())
                }
            }
        }

        fun deleteMusicWithMusicBucketAsync(musicID: Long,musicBucketId: Long) {
            execute {
                deleteMusicWithMusicBucket(musicID,musicBucketId)
            }
        }

        fun insertMusicMultiAsyncWithBucket(musicBucket: String, music: List<Music>) {
            execute {
                val bucket = musicBucketDAOBridge.getMusicBucketByName(musicBucket)
                bucket?.let { b ->
                    music.forEach {
                        insertMusicWithMusicBucket(
                            MusicWithMusicBucket(
                                b.musicBucketId,
                                it.musicBaseId
                            )
                        )
                    }
                }
            }
        }

        suspend fun insertMusicWithMusicBucket(musicID: Long, bucket: String) =
            onResult { co ->
                val musicBucket = musicBucketDAOBridge.getMusicBucketByName(bucket)
                if (musicBucket == null) {
                    co.resumeWith(Result.success(-1L))
                    return@onResult
                }
                val entity = MusicWithMusicBucket(
                    musicBucket.musicBucketId,
                    musicID
                )
                val success = Result.success(insertMusicWithMusicBucket(entity) ?: -1)
                co.resumeWith(success)
            }

    }

    inner class GalleriesWithNotesDAOBridge : BaseGalleriesWithNotesDAO() {
        override fun startEvent() = Unit
    }

    inner class NoteDirWithNoteDAOBridge : BaseNoteDirWithNoteDAO() {
        override fun startEvent() = Unit
    }

    sealed class BaseMusicBucketDAO : BaseDao() {

        private val musicBucketDAO = getMusicBucketDAO()

        fun observeAllMusicBucket(): Flow<List<MusicBucket>?> {
            return musicBucketDAO.observeAllMusicBucket()
        }

        fun getAllMusicBucket(): List<MusicBucket>? {
            return musicBucketDAO.getAllMusicBucket()
        }

        fun getMusicBucketByName(name: String): MusicBucket? {
            return musicBucketDAO.getMusicBucketByName(name)
        }

        fun addMusicBucket(musicBucket: MusicBucket) {
            offerEvent(MediaAction.OnNewMusicBucket(musicBucket))
            musicBucketDAO.addMusicBucket(musicBucket)
        }

        fun updateMusicBucket(musicBucket: MusicBucket): Int {
            offerEvent(MediaAction.OnMusicBucketUpdated(musicBucket))
            return musicBucketDAO.updateMusicBucket(musicBucket)
        }

        fun deleteMusicBucket(musicBucket: MusicBucket) {
            offerEvent(MediaAction.OnMusicBucketDeleted(musicBucket))
            musicBucketDAO.deleteMusicBucket(musicBucket)
        }

    }

    sealed class BaseMusicDAO : BaseDao() {

        private val musicDAO = getMusicDAO()

        fun getAllMusic(): List<Music>? = musicDAO.getAllMusic()

        fun getMusicByUri(uri: Uri): Music? = musicDAO.getMusicByUri(uri)

        fun insertMusic(music: Music) {
            offerEvent(MediaAction.OnMusicInserted(music))
            musicDAO.insertMusic(music)
        }

        fun deleteMusic(music: Music) {
            offerEvent(MediaAction.OnMusicDeleted(music))
            musicDAO.deleteMusic(music)
        }

        fun updateMusic(music: Music): Int {
            offerEvent(MediaAction.OnMusicUpdate(music))
            return musicDAO.updateMusic(music)
        }

    }

    sealed class BaseGalleyDAO : BaseDao() {

        private val signedGalleyDAO = getGalleyDAO()

        fun getAllSignedMedia(): List<GalleyMedia>? =
            signedGalleyDAO.getAllSignedMedia()

        fun observeAllSignedMedia(): Flow<List<GalleyMedia>?> =
            signedGalleyDAO.observeAllSignedMedia()

        fun getAllMediaByType(isVideo: Boolean): List<GalleyMedia>? =
            signedGalleyDAO.getAllMediaByType(isVideo)

        fun getAllGalley(isVideo: Boolean): List<String>? {
            return signedGalleyDAO.getAllGalley(isVideo)
        }

        fun getAllGalley(): List<String>? {
            return signedGalleyDAO.getAllGalley()
        }

        fun getAllMediaByGalley(name: String, isVideo: Boolean): List<GalleyMedia>? {
            return signedGalleyDAO.getAllMediaByGalley(name, isVideo)
        }

        fun getAllMediaByGalley(name: String): List<GalleyMedia>? {
            return signedGalleyDAO.getAllMediaByGalley(name)
        }

        fun getSignedMedia(uri: Uri): GalleyMedia? =
            signedGalleyDAO.getSignedMedia(uri)

        fun getSignedMedia(path: String): GalleyMedia? =
            signedGalleyDAO.getSignedMedia(path)

        fun deleteSignedMediaByUri(uri: Uri) {
            getSignedMedia(uri)?.let { offerEvent(MediaAction.OnMediaByUriDeleted(it)) }
            signedGalleyDAO.deleteSignedMediaByUri(uri)
        }

        fun deleteSignedMedia(media: GalleyMedia) {
            offerEvent(MediaAction.OnMediaDeleted(media))
            signedGalleyDAO.deleteSignedMedia(media)
        }

        fun insertSignedMedia(media: GalleyMedia): Long {
            offerEvent(MediaAction.OnMediaInserted(media))
            return signedGalleyDAO.insertSignedMedia(media)
        }

        fun updateSignedMedia(media: GalleyMedia) {
            offerEvent(MediaAction.OnMediaUpdated(media))
            signedGalleyDAO.updateSignedMedia(media)
        }
    }

    sealed class BaseNoteDAO : BaseDao() {

        private val noteDAO = getNoteDAO()

        fun getAllNote(): List<Note>? {
            return noteDAO.getAllNote()
        }

        fun observeAllNote(): Flow<List<Note>?> {
            return noteDAO.observeAllNote()
        }

        fun getNoteByName(name: String): Note? {
            return noteDAO.getNoteByName(name)
        }

        fun updateNote(note: Note): Int? {
            offerEvent(MediaAction.OnNoteUpdated(note))
            return noteDAO.updateNote(note)
        }

        fun deleteNote(note: Note) {
            offerEvent(MediaAction.OnNoteDeleted(note))
            noteDAO.deleteNote(note)
        }

        fun insertNote(note: Note): Long {
            offerEvent(MediaAction.OnNoteInserted(note))
            return noteDAO.insertNote(note)
        }

    }

    sealed class BaseNoteTypeDAO : BaseDao() {

        private val noteTypeDAO = getNoteTypeDAO()

        fun getNoteDir(name: String): NoteDir? = noteTypeDAO.getNoteDir(name)

        fun getALLNoteDir(): List<NoteDir>? = noteTypeDAO.getALLNoteDir()

        fun insertNoteDir(noteDir: NoteDir) {
            offerEvent(MediaAction.OnNoteDirInserted(noteDir))
            noteTypeDAO.insertNoteDir(noteDir)
        }

        fun deleteNoteDir(noteDir: NoteDir) {
            offerEvent(MediaAction.OnNoteDirDeleted(noteDir))
            noteTypeDAO.deleteNoteDir(noteDir)
        }
    }

    sealed class BaseGalleyBucketDAO : BaseDao() {

        private val galleyBucketDAO = getGalleyBucketDAO()

        fun getGalleyBucket(name: String): GalleyBucket? =
            galleyBucketDAO.getGalleyBucket(name)

        fun getALLGalleyBucket(isVideo: Boolean): List<GalleyBucket>? =
            galleyBucketDAO.getALLGalleyBucket(isVideo)

        fun insertGalleyBucket(galleyBucket: GalleyBucket) {
            offerEvent(MediaAction.OnGalleyBucketInserted(galleyBucket))
            galleyBucketDAO.insertGalleyBucket(galleyBucket)
        }

        fun deleteGalleyBucket(galleyBucket: GalleyBucket) {
            offerEvent(MediaAction.OnGalleyBucketDeleted(galleyBucket))
            galleyBucketDAO.deleteGalleyBucket(galleyBucket)
        }
    }

    sealed class BaseGalleriesWithNotesDAO : BaseDao() {

        private val galleriesWithNotesDAO = getGalleriesWithNotesDAO()

        fun insertGalleriesWithNotes(galleriesWithNotes: GalleriesWithNotes) {
            offerEvent(MediaAction.OnGalleriesWithNotesInserted(galleriesWithNotes))
            galleriesWithNotesDAO.insertGalleriesWithNotes(galleriesWithNotes)
        }

        fun getNotesWithGalley(uri: Uri): List<Note> {
            return galleriesWithNotesDAO.getNotesWithGalley(uri) ?: mutableListOf()
        }

        fun getGalleriesWithNote(noteId: Long): List<GalleyMedia> {
            return galleriesWithNotesDAO.getGalleriesWithNote(noteId) ?: mutableListOf()
        }
    }

    sealed class BaseNoteDirWithNoteDAO : BaseDao() {

        private val noteDirWithNoteDAO = getNoteDirWithNoteDAO()

        fun insertNoteDirWithNote(noteDirWithNotes: NoteDirWithNotes) {
            offerEvent(MediaAction.OnNoteDirWithNoteInserted(noteDirWithNotes))
            noteDirWithNoteDAO.insertNoteDirWithNote(noteDirWithNotes)
        }

        fun getNotesWithNoteDir(noteDirId: Long): List<Note> {
            return noteDirWithNoteDAO.getNotesWithNoteDir(noteDirId) ?: mutableListOf()
        }

        fun getNoteDirWithNote(noteId: Long): List<NoteDir> {
            return noteDirWithNoteDAO.getNoteDirWithNote(noteId) ?: mutableListOf()
        }
    }

    sealed class BaseMusicWithMusicBucketDAO : BaseDao() {

        private val musicWithMusicBucketDAO = getMusicWithMusicBucketDAO()

        fun insertMusicWithMusicBucket(musicWithMusicBucket: MusicWithMusicBucket): Long? {
            offerEvent(MediaAction.OnMusicWithMusicBucketInserted(musicWithMusicBucket))
            return musicWithMusicBucketDAO.insertMusicWithMusicBucket(musicWithMusicBucket)
        }

        fun deleteMusicWithMusicBucket(musicID: Long,musicBucketId: Long) {
            offerEvent(MediaAction.OnMusicWithMusicBucketDeleted(musicID))
            musicWithMusicBucketDAO.deleteMusicWithMusicBucket(musicID,musicBucketId)
        }

        fun observeAllMusicBucketWithMusic(): Flow<List<MusicWithMusicBucket>?> {
            return musicWithMusicBucketDAO.observeAllMusicBucketWithMusic()
        }

        fun getMusicWithMusicBucket(musicBucketId: Long): List<Music> {
            return musicWithMusicBucketDAO.getMusicWithMusicBucket(musicBucketId) ?: mutableListOf()
        }

        fun getMusicBucketWithMusic(musicID: Long): List<MusicBucket> {
            return musicWithMusicBucketDAO.getMusicBucketWithMusic(musicID) ?: mutableListOf()
        }
    }

    abstract class BaseDao {

        private val linkedBlockingDeque by lazy { LinkedBlockingDeque<MediaAction>() }
        protected var dataEvent: Job? = null

        abstract fun startEvent()

        fun stopEvent() {
            dataEvent?.cancel()
            dataEvent = null
        }

        protected fun offerEvent(mediaAction: MediaAction) {
            linkedBlockingDeque.offer(mediaAction)
        }

        protected fun getDeque() = linkedBlockingDeque
    }

}