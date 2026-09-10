package com.chinmay.tayade.mp3downloader.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chinmay.tayade.mp3downloader.MainActivity
import com.chinmay.tayade.mp3downloader.R
import com.chinmay.tayade.mp3downloader.util.formatBytes
import com.chinmay.tayade.mp3downloader.util.hasNotificationPermission

object NotificationHelper {

    const val PROGRESS_CHANNEL = "downloads_progress"
    const val DONE_CHANNEL = "downloads_done"
    const val FOREGROUND_ID = 1001
    private const val DONE_ID_BASE = 2000

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                PROGRESS_CHANNEL,
                context.getString(R.string.channel_progress),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                DONE_CHANNEL,
                context.getString(R.string.channel_done),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_DOWNLOADS, true)
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun buildProgress(
        context: Context,
        title: String,
        downloadedBytes: Long,
        totalBytes: Long,
        indeterminate: Boolean,
        cancelIntent: PendingIntent,
    ): android.app.Notification {
        val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
        val text = if (totalBytes > 0) {
            "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
        } else {
            context.getString(R.string.notif_starting)
        }
        return NotificationCompat.Builder(context, PROGRESS_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(context))
            .setProgress(100, percent, indeterminate)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.action_cancel),
                cancelIntent,
            )
            .build()
    }

    @android.annotation.SuppressLint("MissingPermission") // guarded by hasNotificationPermission()
    fun showCompleted(context: Context, id: Long, title: String, success: Boolean, detail: String) {
        if (!hasNotificationPermission(context)) return
        val notification = NotificationCompat.Builder(context, DONE_CHANNEL)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error,
            )
            .setContentTitle(title)
            .setContentText(detail)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(DONE_ID_BASE + id.toInt(), notification)
    }
}
