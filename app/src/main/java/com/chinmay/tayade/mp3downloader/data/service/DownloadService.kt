package com.chinmay.tayade.mp3downloader.data.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.chinmay.tayade.mp3downloader.data.DownloadRepository
import com.chinmay.tayade.mp3downloader.data.ProgressSink
import com.chinmay.tayade.mp3downloader.data.PythonDownloader
import com.chinmay.tayade.mp3downloader.data.local.DownloadEntity
import com.chinmay.tayade.mp3downloader.data.model.DownloadProgress
import com.chinmay.tayade.mp3downloader.util.formatBytes
import com.chinmay.tayade.mp3downloader.util.hasNotificationPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground service that drains the download queue one job at a time. Survives
 * the UI being backgrounded, the activity being recreated, and the back button.
 */
class DownloadService : LifecycleService() {

    private val repo by lazy { DownloadRepository.get(this) }
    private val draining = AtomicBoolean(false)
    @Volatile private var latestStartId = 0

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        latestStartId = startId

        if (intent?.action == ACTION_CANCEL) {
            val id = intent.getLongExtra(EXTRA_ID, -1L)
            if (id >= 0) repo.cancel(id)
        }

        androidx.core.app.ServiceCompat.startForeground(
            this,
            NotificationHelper.FOREGROUND_ID,
            NotificationHelper.buildProgress(
                context = this,
                title = getString(com.chinmay.tayade.mp3downloader.R.string.notif_preparing),
                downloadedBytes = 0,
                totalBytes = 0,
                indeterminate = true,
                cancelIntent = noopIntent(),
            ),
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )

        drainQueue()
        return START_NOT_STICKY
    }

    private fun drainQueue() {
        if (!draining.compareAndSet(false, true)) return
        lifecycleScope.launch {
            try {
                repo.recoverInterrupted()
                do {
                    while (true) {
                        val next = withContext(Dispatchers.IO) { repo.nextQueued() } ?: break
                        runJob(next)
                    }
                    // Release, then re-check so a row enqueued during the gap is not stranded.
                    draining.set(false)
                    val leftover = withContext(Dispatchers.IO) { repo.nextQueued() } != null
                } while (leftover && draining.compareAndSet(false, true))
            } finally {
                draining.set(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                // A start that arrived after this drain began keeps the service alive.
                stopSelf(latestStartId)
            }
        }
    }

    private suspend fun runJob(entity: DownloadEntity) {
        val id = entity.id
        repo.markRunning(id)

        if (repo.isCancelled(id)) {
            repo.finish(id, com.chinmay.tayade.mp3downloader.data.DownloadResult(false, "Cancelled"))
            return
        }

        var lastNotifyAt = 0L
        var lastPersistPercent = -1
        val notifier = NotificationManagerCompat.from(this)

        val sink = object : ProgressSink {
            override fun isCancelled(): Boolean = repo.isCancelled(id)

            @android.annotation.SuppressLint("MissingPermission") // guarded by hasNotificationPermission()
            override fun onProgress(json: String) {
                val progress = parseProgress(json) ?: return
                repo.publishProgress(id, progress)

                val now = System.currentTimeMillis()
                if (now - lastNotifyAt >= NOTIFY_INTERVAL_MS) {
                    lastNotifyAt = now
                    if (hasNotificationPermission(this@DownloadService)) runCatching {
                        notifier.notify(
                            NotificationHelper.FOREGROUND_ID,
                            NotificationHelper.buildProgress(
                                context = this@DownloadService,
                                title = entity.title,
                                downloadedBytes = progress.downloadedBytes,
                                totalBytes = progress.totalBytes,
                                indeterminate = progress.fraction == null,
                                cancelIntent = cancelIntent(id),
                            ),
                        )
                    }
                    val percent = ((progress.fraction ?: 0f) * 100).toInt()
                    if (percent != lastPersistPercent) {
                        lastPersistPercent = percent
                        lifecycleScope.launch { repo.updatePersistedPercent(id, percent) }
                    }
                }
            }
        }

        val result = withContext(Dispatchers.IO) {
            PythonDownloader.download(
                url = entity.url,
                destinationDir = entity.destinationDir,
                formatSelector = entity.formatSelector,
                sink = sink,
            )
        }

        repo.finish(id, result)

        if (result.ok && result.filePath.isNotBlank()) {
            scanFile(result.filePath)
            NotificationHelper.showCompleted(
                this, id, entity.title, success = true,
                detail = getString(
                    com.chinmay.tayade.mp3downloader.R.string.notif_saved,
                    formatBytes(result.sizeBytes),
                ),
            )
        } else if (!result.cancelled && !repo.isCancelled(id)) {
            NotificationHelper.showCompleted(
                this, id, entity.title, success = false,
                detail = result.message.ifBlank {
                    getString(com.chinmay.tayade.mp3downloader.R.string.notif_failed)
                },
            )
        }
    }

    private fun scanFile(path: String) {
        runCatching { MediaScannerConnection.scanFile(applicationContext, arrayOf(path), null, null) }
    }

    private fun parseProgress(json: String): DownloadProgress? = runCatching {
        val o = JSONObject(json)
        DownloadProgress(
            downloadedBytes = o.optLong("downloaded_bytes", 0L),
            totalBytes = o.optLong("total_bytes", 0L),
            speedBytesPerSec = o.optLong("speed", 0L),
            etaSeconds = o.optInt("eta", 0),
        )
    }.getOrNull()

    private fun cancelIntent(id: Long): PendingIntent {
        val intent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getService(
            this, id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun noopIntent(): PendingIntent = PendingIntent.getService(
        this, Int.MAX_VALUE,
        Intent(this, DownloadService::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val ACTION_CANCEL = "com.chinmay.tayade.mp3downloader.action.CANCEL"
        private const val EXTRA_ID = "download_id"
        private const val NOTIFY_INTERVAL_MS = 600L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, DownloadService::class.java),
            )
        }

        fun cancel(context: Context, id: Long) {
            // The service is already running (and in the foreground) whenever there
            // is something to cancel, so a plain startService is enough and avoids
            // the API 31+ background-FGS-start restriction.
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_ID, id)
            }
            runCatching { context.startService(intent) }
        }
    }
}
