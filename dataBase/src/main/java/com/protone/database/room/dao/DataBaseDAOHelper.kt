package com.protone.database.room.dao

import android.net.Uri
import com.protone.database.room.*
import com.protone.database.room.entity.*

object DataBaseDAOHelper : BaseDAOHelper(), MusicBucketDAO, MusicDAO, SignedGalleyDAO, NoteDAO,
    NoteTypeDAO, GalleyBucketDAO, GalleriesWithNotesDAO, NoteDirWithNoteDAO,
    MusicWithMusicBucketDAO {

    //MusicBucket
    private val musicBucketDAO by lazy { getMusicBucketDAO() }

    suspend fun getAllMusicBucketRs() = onResult<List<MusicBucket>?> {
        execute {
            it.resumeWith(Result.success(getAllMusicBucket()))
        }
    }

    override fun getAllMusicBucket(): List<MusicBucket>? {
        return musicBucketDAO.getAllMusicBucket()
    }

    override fun getMusicBucketByName(name: String): MusicBucket? {
        return musicBucketDAO.getMusicBucketByName(name)
    }

    override fun addMusicBucket(musicBucket: MusicBucket) {
        musicBucketDAO.addMusicBucket(musicBucket)
    }

    fun addMusicBucketThread(musicBucket: MusicBucket) {
        execute {
            musicBucketDAO.addMusicBucket(musicBucket)
        }
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

    override fun updateMusicBucket(bucket: MusicBucket): Int {
        return musicBucketDAO.updateMusicBucket(bucket)
    }

    fun updateMusicBucketBack(bucket: MusicBucket) {
        execute {
            updateMusicBucket(bucket)
        }
    }

    suspend fun updateMusicBucketRs(bucket: MusicBucket) = onResult<Int> {
        execute {
            it.resumeWith(Result.success(updateMusicBucket(bucket)))
        }
    }

    suspend fun deleteMusicBucketRs(bucket: MusicBucket) = onResult<Boolean> {
        execute {
            musicBucketDAO.deleteMusicBucket(bucket)
            it.resumeWith(Result.success(musicBucketDAO.getMusicBucketByName(bucket.name) == null))
        }
    }

    override fun deleteMusicBucket(bucket: MusicBucket) {
        execute {
            musicBucketDAO.deleteMusicBucket(bucket)
        }
    }

    //Music
    private val musicDAO by lazy { getMusicDAO() }

    fun insertMusicMulti(music: List<Music>) {
        execute {
            music.forEach {
                musicDAO.insertMusic(it)
            }
        }
    }

    fun deleteMusicMulti(music: List<Music>) {
        execute {
            music.forEach {
                musicDAO.deleteMusic(it)
            }
        }
    }


    suspend fun getAllMusicRs() = onResult<List<Music>?> {
        execute {
            it.resumeWith(Result.success(getAllMusic()))
        }
    }

    fun insertMusicCheck(music: Music) {
        musicDAO.getMusicByUri(music.uri).let {
            if (it == null) insertMusic(music)
        }
    }

    override fun insertMusic(music: Music) {
        musicDAO.insertMusic(music)
    }

    override fun getAllMusic(): List<Music>? = musicDAO.getAllMusic()

    override fun deleteMusic(music: Music) {
        execute {
            musicDAO.deleteMusic(music)
        }
    }

    override fun updateMusic(music: Music): Int = musicDAO.updateMusic(music)

    override fun updateMusicMyBucket(name: String, bucket: List<String>): Int =
        musicDAO.updateMusicMyBucket(name, bucket)

    override fun getMusicByUri(uri: Uri): Music? = musicDAO.getMusicByUri(uri)

    suspend fun updateMusicRs(music: Music) = onResult<Int> {
        execute {
            it.resumeWith(Result.success(updateMusic(music)))
        }
    }

    suspend fun updateMusicMyBucketRs(name: String, bucket: List<String>) = onResult<Int> {
        execute {
            it.resumeWith(Result.success(updateMusicMyBucket(name, bucket)))
        }
    }

    //Galley
    private val signedGalleyDAO by lazy { getGalleyDAO() }

    suspend fun getAllSignedMediaRs() = onResult<List<GalleyMedia>?> {
        execute {
            it.resumeWith(Result.success(getAllSignedMedia()))
        }
    }

    override fun getAllSignedMedia(): List<GalleyMedia>? =
        signedGalleyDAO.getAllSignedMedia()

    override fun getAllMediaByType(isVideo: Boolean): List<GalleyMedia>? =
        signedGalleyDAO.getAllMediaByType(isVideo)

    override fun getAllGalley(isVideo: Boolean): List<String>? {
        return signedGalleyDAO.getAllGalley(isVideo)
    }

    override fun getAllMediaByGalley(name: String, isVideo: Boolean): List<GalleyMedia>? {
        return signedGalleyDAO.getAllMediaByGalley(name, isVideo)
    }

    fun deleteSignedMedias(list: MutableList<GalleyMedia>) {
        execute {
            list.forEach {
                deleteSignedMediaByUri(it.uri)
            }
        }
    }

    override fun deleteSignedMediaByUri(uri: Uri) {
        signedGalleyDAO.deleteSignedMediaByUri(uri)
    }

    override fun deleteSignedMedia(media: GalleyMedia) {
        execute {
            signedGalleyDAO.deleteSignedMedia(media)
        }
    }

    fun updateMediaMulti(list: MutableList<GalleyMedia>) {
        execute { list.forEach { updateSignedMedia(it) } }
    }

    override fun insertSignedMedia(media: GalleyMedia) {
        execute { sortSignedMedia(media) }
    }

    fun sortSignedMedia(media: GalleyMedia) {
        getSignedMedia(media.uri).let {
            if (it != null) {
                it.name = media.name
                it.path = media.path
                it.bucket = media.bucket
                it.type = media.type
                it.date = media.date
                updateSignedMedia(it)
            } else signedGalleyDAO.insertSignedMedia(media)
        }
    }

    override fun getSignedMedia(uri: Uri): GalleyMedia? =
        signedGalleyDAO.getSignedMedia(uri)

    override fun getSignedMedia(path: String): GalleyMedia? =
        signedGalleyDAO.getSignedMedia(path)

    suspend fun getSignedMediaRs(uri: Uri) = onResult<GalleyMedia?> {
        execute {
            it.resumeWith(Result.success(getSignedMedia(uri)))
        }
    }

    override fun updateSignedMedia(galleyMedia: GalleyMedia) {
        signedGalleyDAO.updateSignedMedia(galleyMedia)
    }

    //Note
    private val noteDAO by lazy { getNoteDAO() }

    override fun getAllNote(): List<Note>? {
        return noteDAO.getAllNote()
    }

    override fun getNoteByName(name: String): Note? {
        return noteDAO.getNoteByName(name)
    }

    override fun updateNote(note: Note): Int? {
        return noteDAO.updateNote(note)
    }

    override fun deleteNote(note: Note) {
        execute {
            noteDAO.deleteNote(note)
        }
    }

    override fun insertNote(note: Note): Long {
        return noteDAO.insertNote(note)
    }

    suspend fun insertNoteRs(note: Note) =
        onResult<Pair<Boolean, Long>> {
            execute {
                var count = 0
                val tempName = note.title
                val names = mutableMapOf<String, Int>()
                getAllNote()?.forEach {
                    names[it.title] = 1
                    if (it.title == note.title) {
                        note.title = "${tempName}(${++count})"
                    }
                }
                while (names[note.title] != null) {
                    note.title = "${tempName}(${++count})"
                }
                val id = insertNote(note)
                it.resumeWith(Result.success(Pair(getNoteByName(note.title) != null, id)))
            }
        }

    //NoteType
    private val noteTypeDAO by lazy { getNoteTypeDAO() }

    suspend fun insertNoteDirRs(
        noteDir: NoteDir,
    ) = onResult<Pair<Boolean, String>> {
        execute {
            var count = 0
            val tempName = noteDir.name
            val names = mutableMapOf<String, Int>()
            getALLNoteDir()?.forEach {
                names[it.name] = 1
                if (it.name == noteDir.name) {
                    noteDir.name = "${tempName}(${++count})"
                }
            }
            while (names[noteDir.name] != null) {
                noteDir.name = "${tempName}(${++count})"
            }
            insertNoteDir(noteDir)
            it.resumeWith(Result.success(Pair(getNoteDir(noteDir.name) != null, noteDir.name)))
        }
    }

    override fun insertNoteDir(noteDir: NoteDir) {
        noteTypeDAO.insertNoteDir(noteDir)
    }

    override fun getNoteDir(name: String): NoteDir? = noteTypeDAO.getNoteDir(name)


    override fun deleteNoteDir(noteDir: NoteDir) {
        noteTypeDAO.deleteNoteDir(noteDir)
    }

    suspend fun doDeleteNoteDirRs(noteDir: NoteDir) = onResult<Boolean> {
        execute {
            deleteNoteDir(noteDir)
            it.resumeWith(Result.success(getNoteDir(noteDir.name) != null))
        }
    }

    override fun getALLNoteDir(): List<NoteDir>? = noteTypeDAO.getALLNoteDir()

    //GalleyBucket
    private val galleyBucketDAO by lazy { getGalleyBucketDAO() }

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

    override fun insertGalleyBucket(galleyBucket: GalleyBucket) {
        galleyBucketDAO.insertGalleyBucket(galleyBucket)
    }

    override fun getGalleyBucket(name: String): GalleyBucket? =
        galleyBucketDAO.getGalleyBucket(name)

    override fun deleteGalleyBucket(galleyBucket: GalleyBucket) {
        execute {
            galleyBucketDAO.deleteGalleyBucket(galleyBucket)
        }
    }

    suspend fun getGalleyBucketRs(name: String) = onResult<GalleyBucket?> {
        execute {
            it.resumeWith(Result.success(getGalleyBucket(name)))
        }
    }

    override fun getALLGalleyBucket(isVideo: Boolean): List<GalleyBucket>? =
        galleyBucketDAO.getALLGalleyBucket(isVideo)

    private val galleriesWithNotesDAO by lazy { getGalleriesWithNotesDAO() }

    override fun insertGalleriesWithNotes(galleriesWithNotes: GalleriesWithNotes) {
        galleriesWithNotesDAO.insertGalleriesWithNotes(galleriesWithNotes)
    }

    override fun getNotesWithGalley(mediaId: Long): List<Note> {
        return galleriesWithNotesDAO.getNotesWithGalley(mediaId) ?: mutableListOf()
    }

    override fun getGalleriesWithNote(noteId: Long): List<GalleyMedia> {
        return galleriesWithNotesDAO.getGalleriesWithNote(noteId) ?: mutableListOf()
    }

    private val noteDirWithNoteDAO by lazy { getNoteDirWithNoteDAO() }

    override fun insertNoteDirWithNote(noteDirWithNotes: NoteDirWithNotes) {
        noteDirWithNoteDAO.insertNoteDirWithNote(noteDirWithNotes)
    }

    override fun getNotesWithNoteDir(noteDirId: Long): List<Note> {
        return noteDirWithNoteDAO.getNotesWithNoteDir(noteDirId) ?: mutableListOf()
    }

    override fun getNoteDirWithNote(noteId: Long): List<NoteDir> {
        return noteDirWithNoteDAO.getNoteDirWithNote(noteId) ?: mutableListOf()
    }

    private val musicWithMusicBucketDAO by lazy { getMusicWithMusicBucketDAO() }

    suspend fun insertMusicWithMusicBucket(musicID: Long, bucket: String) = onResult<Long> { co ->
        val musicBucket = getMusicBucketByName(bucket)
        if (musicBucket == null) {
            co.resumeWith(Result.success(-1L))
            return@onResult
        }
        val entity = MusicWithMusicBucket(
            null,
            musicBucket.musicBucketId,
            musicID
        )
        val success = Result.success(insertMusicWithMusicBucket(entity) ?: -1)
        co.resumeWith(success)
    }

    override fun insertMusicWithMusicBucket(musicWithMusicBucket: MusicWithMusicBucket): Long? {
        return musicWithMusicBucketDAO.insertMusicWithMusicBucket(musicWithMusicBucket)
    }

    override fun deleteMusicWithMusicBucket(musicID: Long) {
        musicWithMusicBucketDAO.deleteMusicWithMusicBucket(musicID)
    }

    override fun getMusicWithMusicBucket(musicBucketId: Long): List<Music> {
        return musicWithMusicBucketDAO.getMusicWithMusicBucket(musicBucketId) ?: mutableListOf()
    }

    override fun getMusicBucketWithMusic(musicID: Long): List<MusicBucket> {
        return musicWithMusicBucketDAO.getMusicBucketWithMusic(musicID) ?: mutableListOf()
    }

}

