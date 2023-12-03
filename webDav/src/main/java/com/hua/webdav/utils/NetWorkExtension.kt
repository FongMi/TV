package com.hua.webdav.utils

import android.net.Uri

val String?.baseUrl: String?
    get() {
        if (this == null) return null
        val uri = Uri.parse(this)
        return if (uri.scheme?.startsWith("http") == true) {
            uri.host
        } else null
    }