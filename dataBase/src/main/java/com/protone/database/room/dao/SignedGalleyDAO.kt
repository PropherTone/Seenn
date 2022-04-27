package com.protone.database.room.dao

import android.net.Uri
import androidx.room.*
import com.protone.database.room.converters.UriTypeConverter
import com.protone.database.room.entity.GalleyMedia

@Dao
@TypeConverters(UriTypeConverter::class)
interface SignedGalleyDAO {

    @Query("SELECT * FROM GalleyMedia")
    fun getAllSignedMedia() : List<GalleyMedia>?

    @Delete
    fun deleteSignedMedia(media: GalleyMedia)

    @Insert
    fun insertSignedMedia(media: GalleyMedia)

    @Query("SELECT * FROM GalleyMedia WHERE uri LIKE :uri")
    fun getSignedMedia(uri: Uri) : GalleyMedia?

    @Query("SELECT * FROM GalleyMedia WHERE uri LIKE :path")
    fun getSignedMedia(path: String) : GalleyMedia?

    @Update
    fun updateSignedMedia(galleyMedia: GalleyMedia)

}