package com.chinmay.tayade.mp3downloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chinmay.tayade.mp3downloader.data.model.DownloadRecord
import com.chinmay.tayade.mp3downloader.data.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val url: String,
    val title: String,
    val thumbnail: String,
    val formatLabel: String,
    val destinationDir: String,
    val formatSelector: String,
    val filePath: String? = null,
    val sizeBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progressPercent: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)

fun DownloadEntity.toRecord(): DownloadRecord = DownloadRecord(
    id = id,
    url = url,
    title = title,
    thumbnail = thumbnail,
    formatLabel = formatLabel,
    filePath = filePath,
    sizeBytes = sizeBytes,
    status = status,
    errorMessage = errorMessage,
    createdAt = createdAt,
    completedAt = completedAt,
)
