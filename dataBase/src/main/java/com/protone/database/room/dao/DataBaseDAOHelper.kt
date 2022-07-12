package com.protone.database.room.dao

import android.net.Uri
import com.protone.database.room.*
import com.protone.database.room.entity.*

object DataBaseDAOHelper : BaseDAOHelper(), MusicBucketDAO, MusicDAO, SignedGalleyDAO, NoteDAO,
    NoteTypeDAO, GalleyBucketDAO {

    //MusicBucket
    private var musicBucketDAO: MusicBucketDAO? = null

    init {
        musicBucketDAO()
    }

    private fun musicBucketDAO() {
        if (musicBucketDAO == null) {
            musicBucketDAO = getMusicBucketDAO()
        }
    }

    suspend fun getAllMusicBucketRs() = onResult<List<MusicBucket>?> {
        execute {
            it.resumeWith(Result.success(getAllMusicBucket()))
        }
    }

    override fun getAllMusicBucket(): List<MusicBucket>? {
        return musicBucketDAO?.getAllMusicBucket()
    }

    override fun getMusicBucketByName(name: String): MusicBucket? {
        return musicBucketDAO?.getMusicBucketByName(name)
    }

    override fun addMusicBucket(musicBucket: MusicBucket) {
        musicBucketDAO?.addMusicBucket(musicBucket)
    }

    fun addMusicBucketThread(musicBucket: MusicBucket) {
        execute {
            musicBucketDAO?.addMusicBucket(musicBucket)
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
        return musicBucketDAO?.updateMusicBucket(bucket) ?: -1
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
            musicBucketDAO?.deleteMusicBucket(bucket)
            it.resumeWith(Result.success(musicBucketDAO?.getMusicBucketByName(bucket.name) == null))
        }
    }

    override fun deleteMusicBucket(bucket: MusicBucket) {
        execute {
            musicBucketDAO?.deleteMusicBucket(bucket)
        }
    }


    //Music
    private var musicDAO: MusicDAO? = null

    init {
        if (musicDAO == null) {
            musicDAO = getMusicDAO()
        }
    }

    fun insertMusicMulti(music: List<Music>) {
        execute {
            music.forEach {
                musicDAO?.insertMusic(it)
            }
        }
    }

    fun deleteMusicMulti(music: List<Music>) {
        execute {
            music.forEach {
                musicDAO?.deleteMusic(it)
            }
        }
    }


    suspend fun getAllMusicRs() = onResult<List<Music>?> {
        execute {
            it.resumeWith(Result.success(getAllMusic()))
        }
    }

    fun insertMusicCheck(music: Music) {
        musicDAO?.getMusicByUri(music.uri).let {
            if (it == null) insertMusic(music)
        }
    }

    override fun insertMusic(music: Music) {
        musicDAO?.insertMusic(music)
    }

    override fun getAllMusic(): List<Music>? = musicDAO?.getAllMusic()

    override fun deleteMusic(music: Music) {
        execute {
            musicDAO?.deleteMusic(music)
        }
    }

    override fun updateMusic(music: Music): Int = musicDAO?.updateMusic(music) ?: -1

    override fun updateMusicMyBucket(name: String, bucket: List<String>): Int =
        musicDAO?.updateMusicMyBucket(name, bucket) ?: -1

    override fun getMusicByUri(uri: Uri): Music? = musicDAO?.getMusicByUri(uri)

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
    private var signedGalleyDAO: SignedGalleyDAO? = null

    init {
        if (signedGalleyDAO == null) {
            signedGalleyDAO = getGalleyDAO()
        }
    }

    suspend fun getAllSignedMediaRs() = onResult<List<GalleyMedia>?> {
        execute {
            it.resumeWith(Result.success(getAllSignedMedia()))
        }
    }

    override fun getAllSignedMedia(): List<GalleyMedia>? =
        signedGalleyDAO?.getAllSignedMedia()

    override fun getAllMediaByType(isVideo: Boolean): List<GalleyMedia>? =
        signedGalleyDAO?.getAllMediaByType(isVideo)

    override fun getAllGalley(isVideo: Boolean): List<String>? {
        return signedGalleyDAO?.getAllGalley(isVideo)
    }

    override fun getAllMediaByGalley(name: String, isVideo: Boolean): List<GalleyMedia>? {
        return signedGalleyDAO?.getAllMediaByGalley(name, isVideo)
    }

    fun deleteSignedMedias(list: MutableList<GalleyMedia>) {
        execute {
            list.forEach {
                deleteSignedMediaByUri(it.uri)
            }
        }
    }

    override fun deleteSignedMediaByUri(uri: Uri) {
        signedGalleyDAO?.deleteSignedMediaByUri(uri)
    }

    override fun deleteSignedMedia(media: GalleyMedia) {
        execute {
            signedGalleyDAO?.deleteSignedMedia(media)
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
            } else signedGalleyDAO?.insertSignedMedia(media)
        }
    }

    override fun getSignedMedia(uri: Uri): GalleyMedia? =
        signedGalleyDAO?.getSignedMedia(uri)

    override fun getSignedMedia(path: String): GalleyMedia? =
        signedGalleyDAO?.getSignedMedia(path)

    suspend fun getSignedMediaRs(uri: Uri) = onResult<GalleyMedia?> {
        execute {
            it.resumeWith(Result.success(getSignedMedia(uri)))
        }
    }

    override fun updateSignedMedia(galleyMedia: GalleyMedia) {
        signedGalleyDAO?.updateSignedMedia(galleyMedia)
    }

    //Note
    private var noteDAO: NoteDAO? = null

    init {
        if (noteDAO == null) {
            noteDAO = getNoteDAO()
        }
    }

    override fun getAllNote(): List<Note>? {
        return noteDAO?.getAllNote()
    }

    override fun getNoteByName(name: String): Note? {
        return noteDAO?.getNoteByName(name)
    }

    override fun updateNote(note: Note): Int? {
        return noteDAO?.updateNote(note)
    }

    override fun deleteNote(note: Note) {
        execute {
            noteDAO?.deleteNote(note)
        }
    }

    override fun insertNote(note: Note) {
        noteDAO?.insertNote(note)
    }

    suspend fun insertNoteRs(note: Note) =
        onResult<Pair<Boolean, String>> {
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
                insertNote(note)
                it.resumeWith(Result.success(Pair(getNoteByName(note.title) != null, note.title)))
            }
        }

    //NoteType
    private var noteTypeDAO: NoteTypeDAO? = null

    init {
        if (noteTypeDAO == null) {
            noteTypeDAO = getNoteTypeDAO()
        }
    }

    suspend fun insertNoteTypeRs(
        noteType: NoteType,
    ) = onResult<Pair<Boolean, String>> {
        execute {
            var count = 0
            val tempName = noteType.type
            val names = mutableMapOf<String, Int>()
            getALLNoteType()?.forEach {
                names[it.type] = 1
                if (it.type == noteType.type) {
                    noteType.type = "${tempName}(${++count})"
                }
            }
            while (names[noteType.type] != null) {
                noteType.type = "${tempName}(${++count})"
            }
            insertNoteType(noteType)
            it.resumeWith(Result.success(Pair(getNoteType(noteType.type) != null, noteType.type)))
        }
    }

    override fun insertNoteType(noteType: NoteType) {
        noteTypeDAO?.insertNoteType(noteType)
    }

    override fun getNoteType(name: String): NoteType? = noteTypeDAO?.getNoteType(name)


    override fun deleteNoteType(noteType: NoteType) {
        noteTypeDAO?.deleteNoteType(noteType)
    }

    suspend fun doDeleteNoteTypeRs(noteType: NoteType) = onResult<Boolean> {
        execute {
            deleteNoteType(noteType)
            it.resumeWith(Result.success(getNoteType(noteType.type) != null))
        }
    }

    override fun getALLNoteType(): List<NoteType>? = noteTypeDAO?.getALLNoteType()

    //GalleyBucket
    private var galleyBucketDAO: GalleyBucketDAO? = null

    init {
        if (galleyBucketDAO == null) {
            galleyBucketDAO = getGalleyBucketDAO()
        }
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

    override fun insertGalleyBucket(galleyBucket: GalleyBucket) {
        galleyBucketDAO?.insertGalleyBucket(galleyBucket)
    }

    override fun getGalleyBucket(name: String): GalleyBucket? =
        galleyBucketDAO?.getGalleyBucket(name)

    override fun deleteGalleyBucket(galleyBucket: GalleyBucket) {
        execute {
            galleyBucketDAO?.deleteGalleyBucket(galleyBucket)
        }
    }

    suspend fun getGalleyBucketRs(name: String) = onResult<GalleyBucket?> {
        execute {
            it.resumeWith(Result.success(getGalleyBucket(name)))
        }
    }

    override fun getALLGalleyBucket(isVideo: Boolean): List<GalleyBucket>? =
        galleyBucketDAO?.getALLGalleyBucket(isVideo)

}

