package com.protone.mediamodle.note.entity

import android.net.Uri

data class RichPhotoStates(
    override val uri: Uri,
    override val link: String?,
    val name: String,
    val date: String?
) : BaseRichStates()