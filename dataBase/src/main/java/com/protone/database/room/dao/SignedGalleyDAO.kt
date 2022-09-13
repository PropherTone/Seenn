package com.protone.database.room.dao

import android.net.Uri
import androidx.room.*
import com.protone.api.converters.UriTypeConverter
import com.protone.api.entity.GalleyMedia

@Dao
@TypeConverters(UriTypeConverter::class)
interface SignedGalleyDAO {

    @Query("SELECT * FROM GalleyMedia ORDER BY dateModified DESC")
    fun getAllSignedMedia(): List<GalleyMedia>?

    @Query("SELECT * FROM GalleyMedia WHERE isVideo LIKE :isVideo ORDER BY dateModified DESC")
    fun getAllMediaByType(isVideo: Boolean): List<GalleyMedia>?

    @Query("SELECT DISTINCT bucket FROM GalleyMedia WHERE isVideo LIKE :isVideo ORDER BY dateModified DESC")
    fun getAllGalley(isVideo: Boolean): List<String>?

    @Query("SELECT DISTINCT bucket FROM GalleyMedia ORDER BY dateModified DESC")
    fun getAllGalley(): List<String>?

    @Query("SELECT * FROM GalleyMedia WHERE bucket LIKE :name AND isVideo LIKE :isVideo ORDER BY dateModified DESC")
    fun getAllMediaByGalley(name: String, isVideo: Boolean): List<GalleyMedia>?

    @Query("SELECT * FROM GalleyMedia WHERE bucket LIKE :name ORDER BY dateModified DESC")
    fun getAllMediaByGalley(name: String): List<GalleyMedia>?

    @Query("DELETE FROM GalleyMedia WHERE media_uri LIKE :uri")
    fun deleteSignedMediaByUri(uri: Uri)

    @Delete
    fun deleteSignedMedia(media: GalleyMedia)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSignedMedia(media: GalleyMedia) : Long

    @Query("SELECT * FROM GalleyMedia WHERE media_uri LIKE :uri")
    fun getSignedMedia(uri: Uri): GalleyMedia?

    @Query("SELECT * FROM GalleyMedia WHERE path LIKE :path")
    fun getSignedMedia(path: String): GalleyMedia?

    @Update
    fun updateSignedMedia(galleyMedia: GalleyMedia)

}