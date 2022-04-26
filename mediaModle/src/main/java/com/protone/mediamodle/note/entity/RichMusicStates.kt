package com.protone.mediamodle.note.entity

import android.net.Uri

data class RichMusicStates(val uri: Uri, val link: String?, val name: String) :
    RichStates()