package com.chinmay.tayade.mp3downloader.util

import android.util.Patterns
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

private val COUNT_UNITS = arrayOf("K", "M", "B", "T")

/** Offline check that a string looks like a web URL. Never opens a connection. */
fun isWebUrl(input: String?): Boolean {
    if (input.isNullOrBlank()) return false
    return Patterns.WEB_URL.matcher(input.trim()).matches()
}

private val YOUTUBE_HOSTS = setOf(
    "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com",
    "youtu.be", "www.youtu.be",
)

/**
 * Pure-Kotlin (no Android framework) check that a URL points at YouTube.
 * Kept framework-free so it is unit-testable without Robolectric.
 */
fun isYouTubeUrl(input: String?): Boolean {
    val host = hostOf(input) ?: return false
    return host in YOUTUBE_HOSTS
}

/** Extracts a lowercase host from a URL string, or null if it has none. */
fun hostOf(input: String?): String? {
    if (input.isNullOrBlank()) return null
    val trimmed = input.trim()
    val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
    val afterScheme = withScheme.substringAfter("://", "")
    if (afterScheme.isEmpty()) return null
    val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
    val hostPort = authority.substringAfterLast('@')
    val host = hostPort.substringBefore(':')
    return host.lowercase(Locale.US).ifBlank { null }
}

/** 1234 -> "1.23K". Null / small / negative values are handled gracefully. */
fun formatCountAbbreviated(number: Long?): String {
    val value = number ?: 0L
    if (abs(value) < 1000) return value.toString()
    val magnitude = (ln(abs(value).toDouble()) / ln(1000.0)).toInt().coerceIn(1, COUNT_UNITS.size)
    val scaled = value / 1000.0.pow(magnitude.toDouble())
    return String.format(Locale.US, "%.2f%s", scaled, COUNT_UNITS[magnitude - 1])
}

/** 15_728_640 -> "15.0 MB". */
fun formatBytes(bytes: Long?): String {
    val value = bytes ?: 0L
    if (value <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val magnitude = (ln(value.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
    val scaled = value / 1024.0.pow(magnitude.toDouble())
    return if (magnitude == 0) "$value B"
    else String.format(Locale.US, "%.1f %s", scaled, units[magnitude])
}

/** Bytes/second -> "1.2 MB/s". */
fun formatSpeed(bytesPerSecond: Long?): String {
    val value = bytesPerSecond ?: 0L
    if (value <= 0L) return "—"
    return "${formatBytes(value)}/s"
}

/** 3661 -> "1:01:01", 61 -> "1:01". */
fun formatDuration(totalSeconds: Int?): String {
    val secs = (totalSeconds ?: 0).coerceAtLeast(0)
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

/** 90 -> "about 1 min left". */
fun formatEta(seconds: Int?): String {
    val s = seconds ?: 0
    if (s <= 0) return ""
    return when {
        s < 60 -> "about ${s}s left"
        s < 3600 -> "about ${s / 60} min left"
        else -> "about ${s / 3600} hr left"
    }
}
