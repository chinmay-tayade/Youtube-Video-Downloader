package com.chinmay.tayade.mp3downloader.ui.navigation

import android.util.Base64

/** Type-safe-ish route helpers for the app's four screens. */
object Destinations {
    const val HOME = "home"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"

    const val FORMATS_ARG_URL = "url"
    const val FORMATS = "formats/{$FORMATS_ARG_URL}"

    fun formats(url: String): String = "formats/${encode(url)}"

    fun encode(raw: String): String =
        Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)

    fun decode(encoded: String): String =
        runCatching {
            String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrDefault(encoded)
}
