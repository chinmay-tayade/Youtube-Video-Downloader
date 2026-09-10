package com.chinmay.tayade.mp3downloader.data.model

/** Lightweight metadata for a single video. [ok] is false when the lookup failed. */
data class VideoInfo(
    val ok: Boolean,
    val id: String = "",
    val title: String = "",
    val thumbnail: String = "",
    val uploader: String = "",
    val webpageUrl: String = "",
    val views: Long = 0L,
    val likes: Long = 0L,
    val durationSeconds: Int = 0,
    val error: String = "",
)

/**
 * The formats we can actually save on device without an ffmpeg merge step:
 * progressive video heights (video + audio muxed) and audio-only extensions.
 */
data class FormatOptions(
    val info: VideoInfo,
    val videoHeights: List<Int> = emptyList(),
    val audioExtensions: List<String> = emptyList(),
) {
    val hasVideo: Boolean get() = videoHeights.isNotEmpty()
    val hasAudio: Boolean get() = audioExtensions.isNotEmpty()
}

/** A concrete choice the user made on the formats screen. */
sealed interface MediaSelection {
    /** yt-dlp lets us pick "best" – it will land on a progressive stream. */
    object Auto : MediaSelection

    /** Progressive video capped at [height] pixels. */
    data class Video(val height: Int) : MediaSelection

    /** Audio-only in the given container (m4a, opus, webm, mp3 …). */
    data class Audio(val ext: String) : MediaSelection

    val label: String
        get() = when (this) {
            Auto -> "Auto (best)"
            is Video -> "${height}p"
            is Audio -> "Audio · ${ext.uppercase()}"
        }

    val isAudioOnly: Boolean get() = this is Audio
}

/** Everything the download service needs to run one job. */
data class DownloadRequest(
    val url: String,
    val title: String,
    val thumbnail: String,
    val destinationDir: String,
    val selection: MediaSelection,
)

enum class DownloadStatus { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }

/** Live byte-level progress for a running job (never persisted). */
data class DownloadProgress(
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Int = 0,
) {
    /** 0f..1f, or null when the total size is still unknown (indeterminate). */
    val fraction: Float?
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else null
}

/** A row in the downloads library, merged with any live [progress]. */
data class DownloadRecord(
    val id: Long,
    val url: String,
    val title: String,
    val thumbnail: String,
    val formatLabel: String,
    val filePath: String?,
    val sizeBytes: Long,
    val status: DownloadStatus,
    val errorMessage: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val progress: DownloadProgress? = null,
) {
    val isActive: Boolean get() = status == DownloadStatus.QUEUED || status == DownloadStatus.RUNNING
}
