package com.chinmay.tayade.mp3downloader.util

import com.chinmay.tayade.mp3downloader.data.model.MediaSelection

/**
 * Turns a [MediaSelection] into a yt-dlp `format` selector string.
 *
 * Every selector resolves to a single stream that is already muxed (progressive
 * video) or audio-only, so the download never needs an ffmpeg merge step.
 * A progressive/`ext`-specific preference is always followed by a plain
 * fallback so a download never fails just because the ideal container is absent.
 */
fun MediaSelection.toFormatSelector(): String = when (this) {
    MediaSelection.Auto ->
        "best[vcodec!=none][acodec!=none]/best/worst"

    is MediaSelection.Video -> {
        val h = height.coerceAtLeast(1)
        "best[height<=$h][ext=mp4][vcodec!=none][acodec!=none]/" +
            "best[height<=$h][vcodec!=none][acodec!=none]/" +
            "best[height<=$h]/best/worst"
    }

    is MediaSelection.Audio -> {
        val preferred = when (ext.lowercase()) {
            "m4a" -> "bestaudio[ext=m4a]/bestaudio[acodec^=mp4a]/bestaudio"
            "opus" -> "bestaudio[ext=opus]/bestaudio[acodec=opus]/bestaudio"
            "webm" -> "bestaudio[ext=webm]/bestaudio"
            "mp3" -> "bestaudio[ext=mp3]/bestaudio"
            else -> "bestaudio[ext=$ext]/bestaudio"
        }
        // If the site exposes no standalone audio stream to this client (some do
        // not without an attestation token), fall back to the muxed progressive
        // stream so the download still succeeds with audio, just as an mp4.
        "$preferred/best[vcodec!=none][acodec!=none]/best/worst"
    }
}
