package com.protone.mediamodle.note.entity

import android.net.Uri

data class RichPhotoStates(
    val uri: Uri,
    val link: String?,
    val name: String,
    val date: String?
) : RichStates()
