package com.protone.mediamodle.media

import com.protone.database.room.entity.GalleyMedia

interface IGalleyFragment {
    fun select(galleyMedia: MutableList<GalleyMedia>)
    fun openView(galleyMedia: GalleyMedia, galley: String)
}

interface FragMailer {
    fun deleteMedia(galleyMedia: GalleyMedia)
    fun addBucket(name: String, list: MutableList<GalleyMedia>)
    fun selectAll()
    fun onActionBtn()
    fun getChooseGalley(): MutableList<GalleyMedia>?
    fun onGalleyUpdate(updateList: ArrayList<GalleyMedia>)
}