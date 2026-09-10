package com.chinmay.tayade.mp3downloader.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

/**
 * yt-dlp writes through a plain filesystem path, so every destination the app
 * offers has to resolve to one. The app holds `MANAGE_EXTERNAL_STORAGE`, which
 * makes writing anywhere under the primary volume possible.
 */
object StoragePaths {

    private const val FOLDER_NAME = "YouTube Video Downloader"

    /** `/storage/emulated/0/Movies/YouTube Video Downloader`, created on demand. */
    fun defaultDownloadDir(context: Context): String {
        val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val target = File(movies, FOLDER_NAME)
        if (runCatching { target.mkdirs(); target.isDirectory || target.exists() }.getOrDefault(false)) {
            return target.absolutePath
        }
        return appPrivateDownloadDir(context)
    }

    /** Always-writable fallback that needs no permission. */
    fun appPrivateDownloadDir(context: Context): String {
        val external = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return external?.absolutePath
            ?: File(context.filesDir, "downloads").apply { mkdirs() }.absolutePath
    }

    /**
     * Converts a Storage-Access-Framework tree Uri
     * (`content://com.android.externalstorage.documents/tree/primary:Foo/Bar`)
     * to `/storage/emulated/0/Foo/Bar`. Returns null for non-primary volumes or
     * anything we can't map safely.
     */
    fun treeUriToPath(uri: Uri?): String? {
        uri ?: return null
        val docId = runCatching {
            android.provider.DocumentsContract.getTreeDocumentId(uri)
        }.getOrNull() ?: return null

        val parts = docId.split(':', limit = 2)
        if (parts.size != 2) return null
        val (volume, relative) = parts
        if (!volume.equals("primary", ignoreCase = true)) return null

        val root = Environment.getExternalStorageDirectory()?.absolutePath ?: return null
        val cleaned = relative.trim('/')
        return if (cleaned.isEmpty()) root else "$root/$cleaned"
    }

    /** True when [path] can be created and written to right now. */
    fun isWritable(path: String): Boolean = runCatching {
        val dir = File(path)
        (dir.exists() || dir.mkdirs()) && dir.canWrite()
    }.getOrDefault(false)
}
