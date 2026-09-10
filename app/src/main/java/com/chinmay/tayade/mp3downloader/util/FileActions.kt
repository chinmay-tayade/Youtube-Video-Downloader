package com.chinmay.tayade.mp3downloader.util

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

private fun authority(context: Context) = "${context.packageName}.fileprovider"

private fun mimeTypeOf(file: File): String {
    val ext = file.extension.lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        ?: when (ext) {
            "m4a", "aac" -> "audio/mp4"
            "opus", "ogg" -> "audio/ogg"
            "mkv" -> "video/x-matroska"
            else -> if (ext in setOf("mp3", "wav", "flac")) "audio/*" else "video/*"
        }
}

/** Opens the finished file in whatever app the user has for that media type. */
fun openMediaFile(context: Context, path: String): Boolean {
    val file = File(path)
    if (!file.exists()) return false
    val uri = runCatching {
        FileProvider.getUriForFile(context, authority(context), file)
    }.getOrNull() ?: return false
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeTypeOf(file))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching { context.startActivity(intent); true }.getOrDefault(false)
}

/** Fires the system share sheet for the finished file. */
fun shareMediaFile(context: Context, path: String): Boolean {
    val file = File(path)
    if (!file.exists()) return false
    val uri = runCatching {
        FileProvider.getUriForFile(context, authority(context), file)
    }.getOrNull() ?: return false
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeTypeOf(file)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching {
        context.startActivity(
            Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    }.getOrDefault(false)
}
