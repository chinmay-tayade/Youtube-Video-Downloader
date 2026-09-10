package com.chinmay.tayade.mp3downloader.util

import android.util.Patterns
import kotlin.math.abs
import kotlin.math.ln

private val UNITS = arrayOf("K", "M", "B", "T")

/** Offline check that a string looks like a web URL. Never opens a connection. */
fun isWebUrl(input: String?): Boolean {
    if (input.isNullOrBlank()) return false
    return Patterns.WEB_URL.matcher(input.trim()).matches()
}

/** 1234 -> "1.23K". Null / small / negative values are handled gracefully. */
fun formatCountAbbreviated(number: Long?): String {
    val value = number ?: 0L
    if (abs(value) < 1000) return value.toString()

    val magnitude = (ln(abs(value).toDouble()) / ln(1000.0)).toInt().coerceIn(1, UNITS.size)
    val scaled = value / Math.pow(1000.0, magnitude.toDouble())
    return "%.2f%s".format(scaled, UNITS[magnitude - 1])
}
