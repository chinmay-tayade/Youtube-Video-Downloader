package com.chinmay.tayade.mp3downloader.data

import android.content.Context
import com.chinmay.tayade.mp3downloader.data.local.AppDatabase
import com.chinmay.tayade.mp3downloader.data.local.DownloadDao
import com.chinmay.tayade.mp3downloader.data.local.DownloadEntity
import com.chinmay.tayade.mp3downloader.data.local.toRecord
import com.chinmay.tayade.mp3downloader.data.model.DownloadProgress
import com.chinmay.tayade.mp3downloader.data.model.DownloadRecord
import com.chinmay.tayade.mp3downloader.data.model.DownloadRequest
import com.chinmay.tayade.mp3downloader.data.model.DownloadStatus
import com.chinmay.tayade.mp3downloader.data.service.DownloadService
import com.chinmay.tayade.mp3downloader.util.toFormatSelector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Single source of truth for downloads. Room owns the durable record; this
 * class owns the transient bits (live byte progress, cancellation flags) and is
 * the only thing that talks to [DownloadService].
 */
class DownloadRepository private constructor(
    context: Context,
    private val dao: DownloadDao,
) {
    private val appContext = context.applicationContext

    private val liveProgress = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    private val cancelledIds = ConcurrentHashMap.newKeySet<Long>()

    /** Library rows, each merged with its live progress if it is running. */
    val records: Flow<List<DownloadRecord>> =
        combine(dao.observeAll(), liveProgress) { entities, progress ->
            entities.map { entity ->
                entity.toRecord().copy(progress = progress[entity.id])
            }
        }

    // --- enqueue / cancel (called from ViewModels) -------------------------- //

    suspend fun enqueue(request: DownloadRequest): Long {
        val id = dao.insert(
            DownloadEntity(
                url = request.url,
                title = request.title,
                thumbnail = request.thumbnail,
                formatLabel = request.selection.label,
                destinationDir = request.destinationDir,
                formatSelector = request.selection.toFormatSelector(),
            ),
        )
        DownloadService.start(appContext)
        return id
    }

    suspend fun retry(id: Long) {
        val row = dao.findById(id) ?: return
        cancelledIds.remove(id)
        dao.finalize(
            id = id,
            status = DownloadStatus.QUEUED,
            filePath = null,
            sizeBytes = 0L,
            percent = 0,
            error = null,
            completedAt = null,
        )
        DownloadService.start(appContext)
    }

    fun cancel(id: Long) {
        cancelledIds.add(id)
        DownloadService.cancel(appContext, id)
    }

    suspend fun delete(id: Long, alsoRemoveFile: Boolean) {
        val row = dao.findById(id)
        cancelledIds.add(id)
        if (alsoRemoveFile) row?.filePath?.let { runCatching { File(it).delete() } }
        dao.delete(id)
        liveProgress.update { it - id }
    }

    suspend fun clearFinished() {
        dao.deleteByStatus(
            listOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED),
        )
    }

    // --- called by DownloadService --------------------------------------- //

    suspend fun nextQueued(): DownloadEntity? =
        dao.findByStatus(listOf(DownloadStatus.QUEUED)).firstOrNull { it.id !in cancelledIds }

    suspend fun recoverInterrupted() {
        dao.markInterrupted(
            running = listOf(DownloadStatus.RUNNING),
            failed = DownloadStatus.FAILED,
            reason = "Interrupted",
        )
    }

    suspend fun markRunning(id: Long) {
        dao.updateStatus(id, DownloadStatus.RUNNING)
    }

    fun isCancelled(id: Long): Boolean = id in cancelledIds

    fun publishProgress(id: Long, progress: DownloadProgress) {
        liveProgress.update { it + (id to progress) }
    }

    suspend fun updatePersistedPercent(id: Long, percent: Int) {
        dao.updateProgress(id, percent)
    }

    suspend fun finish(id: Long, result: DownloadResult) {
        val status = when {
            result.ok -> DownloadStatus.COMPLETED
            result.cancelled || id in cancelledIds -> DownloadStatus.CANCELLED
            else -> DownloadStatus.FAILED
        }
        if (status != DownloadStatus.COMPLETED) {
            result.filePath.takeIf { it.isNotBlank() }?.let { runCatching { File(it).delete() } }
        }
        dao.finalize(
            id = id,
            status = status,
            filePath = result.filePath.takeIf { it.isNotBlank() && result.ok },
            sizeBytes = result.sizeBytes,
            percent = if (result.ok) 100 else dao.findById(id)?.progressPercent ?: 0,
            error = if (status == DownloadStatus.FAILED) result.message.ifBlank { "Download failed" } else null,
            completedAt = System.currentTimeMillis(),
        )
        liveProgress.update { it - id }
        cancelledIds.remove(id)
    }

    companion object {
        @Volatile
        private var instance: DownloadRepository? = null

        fun get(context: Context): DownloadRepository = instance ?: synchronized(this) {
            instance ?: DownloadRepository(
                context,
                AppDatabase.get(context).downloadDao(),
            ).also { instance = it }
        }
    }
}
