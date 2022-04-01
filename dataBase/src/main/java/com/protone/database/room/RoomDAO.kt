package com.protone.database.room

import com.protone.database.room.dao.*

fun getGalleyDAO(): SignedGalleyDAO {
    return SeennDataBase.database.getGalleyDAO()
}

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