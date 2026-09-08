package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VaultFileType {
    PHOTO,
    VIDEO,
    AUDIO,
    FILE
}

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val originalPath: String = "",
    val storedPath: String,
    val fileType: VaultFileType,
    val sizeBytes: Long,
    val mimeType: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isTrash: Boolean = false,
    val trashedAt: Long? = null,
    val durationMs: Long = 0
)

@Entity(tableName = "vault_notes")
data class VaultNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isTrash: Boolean = false,
    val trashedAt: Long? = null
)

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

@Entity(tableName = "vault_downloads")
data class VaultDownload(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val localPath: String,
    val progress: Float = 0f,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val speedText: String = "1.8 MB/s",
    val resolution: String = "1080p Full HD",
    val createdAt: Long = System.currentTimeMillis()
)
