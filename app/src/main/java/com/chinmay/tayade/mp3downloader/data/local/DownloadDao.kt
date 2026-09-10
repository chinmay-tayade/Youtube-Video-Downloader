package com.chinmay.tayade.mp3downloader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chinmay.tayade.mp3downloader.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun findById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status IN (:statuses) ORDER BY createdAt ASC")
    suspend fun findByStatus(statuses: List<DownloadStatus>): List<DownloadEntity>

    @Insert
    suspend fun insert(entity: DownloadEntity): Long

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus)

    @Query("UPDATE downloads SET progressPercent = :percent WHERE id = :id")
    suspend fun updateProgress(id: Long, percent: Int)

    @Query(
        "UPDATE downloads SET status = :status, filePath = :filePath, sizeBytes = :sizeBytes, " +
            "progressPercent = :percent, errorMessage = :error, completedAt = :completedAt WHERE id = :id",
    )
    suspend fun finalize(
        id: Long,
        status: DownloadStatus,
        filePath: String?,
        sizeBytes: Long,
        percent: Int,
        error: String?,
        completedAt: Long?,
    )

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM downloads WHERE status IN (:statuses)")
    suspend fun deleteByStatus(statuses: List<DownloadStatus>)

    /** Recover rows left mid-flight by a process death. */
    @Query(
        "UPDATE downloads SET status = :failed, errorMessage = :reason " +
            "WHERE status IN (:running)",
    )
    suspend fun markInterrupted(
        running: List<DownloadStatus>,
        failed: DownloadStatus,
        reason: String,
    )
}
