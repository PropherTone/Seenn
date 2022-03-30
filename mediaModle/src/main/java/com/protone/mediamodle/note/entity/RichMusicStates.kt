package com.protone.mediamodle.note.entity

import android.net.Uri

data class RichMusicStates(override val uri: Uri, override val link: String?) : BaseRichStates()