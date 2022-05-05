package com.protone.mediamodle.note.entity

import android.net.Uri

class RichPhotoStates(
    val uri: Uri,
    name: String?,
    val date: String?
) : RichStates(name ?: "")
