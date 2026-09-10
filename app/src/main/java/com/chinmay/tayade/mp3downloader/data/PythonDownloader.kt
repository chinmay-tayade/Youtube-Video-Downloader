package com.chinmay.tayade.mp3downloader.data

import com.chinmay.tayade.mp3downloader.data.model.FormatOptions
import com.chinmay.tayade.mp3downloader.data.model.VideoInfo
import com.chaquo.python.Python
import org.json.JSONObject

/** Outcome of a download attempt. */
data class DownloadResult(
    val ok: Boolean,
    val message: String,
    val filePath: String = "",
    val ext: String = "",
    val sizeBytes: Long = 0L,
) {
    val cancelled: Boolean get() = !ok && message.equals("Cancelled", ignoreCase = true)
}

/**
 * Called from Python during a download. Implementations must be cheap and
 * thread-safe – Python invokes them from the download thread.
 */
interface ProgressSink {
    fun onProgress(json: String)
    fun isCancelled(): Boolean
}

/**
 * Thin, exception-safe wrapper around the Chaquopy `video_downloader` module.
 * Every call returns a fully-formed result – the Python side always emits JSON,
 * and any failure here is caught and reported rather than thrown.
 */
object PythonDownloader {

    private fun module() = Python.getInstance().getModule("video_downloader")

    fun getVideoInfo(url: String): VideoInfo = try {
        parseInfo(JSONObject(module().callAttr("get_video_info", url).toString()))
    } catch (e: Exception) {
        VideoInfo(ok = false, error = e.message ?: "Unexpected error")
    }

    fun probeFormats(url: String): FormatOptions = try {
        val obj = JSONObject(module().callAttr("probe_formats", url).toString())
        val info = parseInfo(obj)
        FormatOptions(
            info = info,
            videoHeights = obj.optJSONArray("heights").toIntList(),
            audioExtensions = obj.optJSONArray("audio").toStringList(),
        )
    } catch (e: Exception) {
        FormatOptions(info = VideoInfo(ok = false, error = e.message ?: "Unexpected error"))
    }

    fun download(
        url: String,
        destinationDir: String,
        formatSelector: String,
        sink: ProgressSink,
    ): DownloadResult = try {
        val obj = JSONObject(
            module().callAttr("download", url, destinationDir, formatSelector, sink).toString(),
        )
        DownloadResult(
            ok = obj.optBoolean("ok", false),
            message = obj.optString("message", ""),
            filePath = obj.optString("file_path", ""),
            ext = obj.optString("ext", ""),
            sizeBytes = obj.optLong("filesize", 0L),
        )
    } catch (e: Exception) {
        DownloadResult(ok = false, message = e.message ?: "Unexpected error")
    }

    private fun parseInfo(obj: JSONObject) = VideoInfo(
        ok = obj.optBoolean("ok", false),
        id = obj.optString("id", ""),
        title = obj.optString("title", "Unknown title"),
        thumbnail = obj.optString("thumbnail", ""),
        uploader = obj.optString("uploader", ""),
        webpageUrl = obj.optString("webpage_url", ""),
        views = obj.optLong("views", 0L),
        likes = obj.optLong("likes", 0L),
        durationSeconds = obj.optInt("duration", 0),
        error = obj.optString("error", ""),
    )

    private fun org.json.JSONArray?.toIntList(): List<Int> {
        this ?: return emptyList()
        return (0 until length()).map { optInt(it) }.filter { it > 0 }
    }

    private fun org.json.JSONArray?.toStringList(): List<String> {
        this ?: return emptyList()
        return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() } }
    }
}
