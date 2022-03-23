package com.protone.database.room

import com.protone.database.room.dao.MusicBucketDAO
import com.protone.database.room.dao.MusicDAO
import com.protone.database.room.dao.NoteDAO
import com.protone.database.room.dao.NoteTypeDAO

fun getNoteDAO(): NoteDAO {
    return SeennDataBase.database.getNoteDAO()
}

fun getNoteTypeDAO(): NoteTypeDAO {
    return SeennDataBase.database.getNoteTypeDAO()
}

fun getMusicBucketDAO(): MusicBucketDAO {
    return SeennDataBase.database.getMusicBucketDAO()
}

fun getMusicDAO(): MusicDAO {
    return SeennDataBase.database.getMusicDAO()
}