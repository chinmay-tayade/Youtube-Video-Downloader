package com.chinmay.tayade.mp3downloader.data

import com.chaquo.python.Python
import org.json.JSONObject

/** Metadata for a single video. [ok] is false when the lookup failed. */
data class VideoInfo(
    val ok: Boolean,
    val title: String,
    val thumbnail: String,
    val views: Long,
    val likes: Long,
    val error: String,
)

/** Outcome of a download attempt. */
data class DownloadResult(val ok: Boolean, val message: String)

/**
 * Thin wrapper around the Chaquopy `video_downloader` module. Every call returns
 * a fully-formed result object – the Python side always emits JSON, and any
 * failure here is caught and reported rather than thrown.
 */
object PythonDownloader {

    private fun module() = Python.getInstance().getModule("video_downloader")

    fun getVideoInfo(url: String): VideoInfo = try {
        val obj = JSONObject(module().callAttr("get_video_info", url).toString())
        VideoInfo(
            ok = obj.optBoolean("ok", false),
            title = obj.optString("title", "Unknown title"),
            thumbnail = obj.optString("thumbnail", ""),
            views = obj.optLong("views", 0L),
            likes = obj.optLong("likes", 0L),
            error = obj.optString("error", ""),
        )
    } catch (e: Exception) {
        VideoInfo(false, "", "", 0L, 0L, e.message ?: "Unexpected error")
    }

    fun downloadVideo(url: String, destination: String): DownloadResult = try {
        val obj = JSONObject(module().callAttr("download_video", url, destination).toString())
        DownloadResult(obj.optBoolean("ok", false), obj.optString("message", ""))
    } catch (e: Exception) {
        DownloadResult(false, e.message ?: "Unexpected error")
    }
}
