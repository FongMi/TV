package com.hua.webdav

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WebDavConfig(
    var url: String,
    var username: String,
    var password: String
) : Parcelable
