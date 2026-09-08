package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.model.DownloadStatus
import com.example.model.VaultDownload
import com.example.model.VaultFileType
import com.example.model.VaultItem
import com.example.model.VaultNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class ImportResult(
    val item: VaultItem?,
    val pendingDeleteUri: Uri? = null
)

class VaultRepository(
    private val dao: VaultDao,
    private val prefs: VaultPreferences
) {
    val activeItems: Flow<List<VaultItem>> = dao.getAllActiveItems()
    val trashItems: Flow<List<VaultItem>> = dao.getTrashItems()
    val activeNotes: Flow<List<VaultNote>> = dao.getAllActiveNotes()
    val trashNotes: Flow<List<VaultNote>> = dao.getTrashNotes()
    val allDownloads: Flow<List<VaultDownload>> = dao.getAllDownloads()

    fun getItemsByType(type: VaultFileType): Flow<List<VaultItem>> = dao.getItemsByType(type)
    fun getItemCountByType(type: VaultFileType): Flow<Int> = dao.getItemCountByType(type)
    fun getTotalSizeByType(type: VaultFileType): Flow<Long> = dao.getTotalSizeByType(type)
    fun getTotalVaultSize(): Flow<Long> = dao.getTotalActiveVaultSize()

    suspend fun importFile(context: Context, uri: Uri, fallbackType: VaultFileType = VaultFileType.FILE): ImportResult {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "hidden_file_${System.currentTimeMillis()}"
                var fileSize = 0L
                var mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                val detectedType = when {
                    mimeType.startsWith("image/") || fileName.endsWith(".jpg", true) || fileName.endsWith(".png", true) || fileName.endsWith(".jpeg", true) || fileName.endsWith(".webp", true) -> VaultFileType.PHOTO
                    mimeType.startsWith("video/") || fileName.endsWith(".mp4", true) || fileName.endsWith(".mkv", true) || fileName.endsWith(".webm", true) -> VaultFileType.VIDEO
                    mimeType.startsWith("audio/") || fileName.endsWith(".mp3", true) || fileName.endsWith(".wav", true) || fileName.endsWith(".m4a", true) -> VaultFileType.AUDIO
                    else -> fallbackType
                }

                val vaultDir = File(context.filesDir, "vault_files").apply { if (!exists()) mkdirs() }
                val ext = if (fileName.contains(".")) fileName.substring(fileName.lastIndexOf(".")) else ""
                val hiddenFile = File(vaultDir, "${UUID.randomUUID()}$ext")

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(hiddenFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (fileSize <= 0) {
                    fileSize = hiddenFile.length()
                }

                val item = VaultItem(
                    name = fileName,
                    originalPath = uri.toString(),
                    storedPath = hiddenFile.absolutePath,
                    fileType = detectedType,
                    sizeBytes = fileSize,
                    mimeType = mimeType,
                    createdAt = System.currentTimeMillis()
                )
                val id = dao.insertItem(item)

                // Attempt to delete the original source file from MediaStore
                var pendingDeleteUri: Uri? = null
                try {
                    val deletedRows = contentResolver.delete(uri, null, null)
                    if (deletedRows <= 0) {
                        pendingDeleteUri = uri
                    }
                } catch (_: SecurityException) {
                    pendingDeleteUri = uri
                } catch (_: Exception) {
                }

                ImportResult(item = item.copy(id = id), pendingDeleteUri = pendingDeleteUri)
            } catch (e: Exception) {
                e.printStackTrace()
                ImportResult(item = null)
            }
        }
    }

    suspend fun unhideItem(context: Context, item: VaultItem): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(item.storedPath)
                if (!sourceFile.exists()) {
                    dao.deleteItemsPermanently(listOf(item.id))
                    return@withContext false
                }

                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (item.mimeType.isNotEmpty()) item.mimeType else "application/octet-stream")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val targetUri = when (item.fileType) {
                    VaultFileType.PHOTO -> resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    VaultFileType.VIDEO -> resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                    VaultFileType.AUDIO -> resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                    VaultFileType.FILE -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        } else {
                            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val targetFile = File(publicDir, item.name)
                            sourceFile.copyTo(targetFile, overwrite = true)
                            null
                        }
                    }
                }

                if (targetUri != null) {
                    resolver.openOutputStream(targetUri)?.use { outStream ->
                        sourceFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(targetUri, contentValues, null, null)
                    }
                }

                // Delete private hidden copy and remove from DB
                sourceFile.delete()
                dao.deleteItemsPermanently(listOf(item.id))
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun moveItemsToTrash(ids: List<Long>) = dao.moveItemsToTrash(ids)

    suspend fun restoreItems(ids: List<Long>) = dao.restoreItemsFromTrash(ids)

    suspend fun deletePermanently(ids: List<Long>) {
        withContext(Dispatchers.IO) {
            ids.forEach { id ->
                val item = dao.getItemById(id)
                if (item != null) {
                    try {
                        val file = File(item.storedPath)
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            dao.deleteItemsPermanently(ids)
        }
    }

    suspend fun emptyTrash() {
        withContext(Dispatchers.IO) {
            // Trash items
            dao.emptyTrashItems()
            dao.emptyTrashNotes()
        }
    }

    // Notes
    suspend fun saveNote(note: VaultNote): Long {
        return if (note.id == 0L) {
            dao.insertNote(note)
        } else {
            dao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            note.id
        }
    }

    suspend fun moveNoteToTrash(id: Long) = dao.moveNoteToTrash(id)
    suspend fun restoreNote(id: Long) = dao.restoreNote(id)
    suspend fun deleteNotePermanently(id: Long) = dao.deleteNotePermanently(id)

    // Downloads
    suspend fun insertItem(item: VaultItem): Long = dao.insertItem(item)
    suspend fun addDownload(download: VaultDownload): Long = dao.insertDownload(download)
    suspend fun updateDownloadStatus(id: Long, status: DownloadStatus) = dao.updateDownloadStatus(id, status)
    suspend fun updateDownloadProgress(id: Long, progress: Float, downloaded: Long, speed: String, status: DownloadStatus) =
        dao.updateDownloadProgress(id, progress, downloaded, speed, status)
    suspend fun removeDownload(id: Long) = dao.deleteDownload(id)

    // Legacy Seed Data Cleanup (Ensures 100% clean, empty vault for real user files)
    suspend fun cleanLegacySeedDataIfPresent(context: Context) {
        withContext(Dispatchers.IO) {
            if (!prefs.hasCleanedLegacySeed) {
                try {
                    dao.deleteAllItems()
                    dao.deleteAllNotes()
                    dao.clearAllDownloads()

                    val vaultDir = File(context.filesDir, "vault_files")
                    if (vaultDir.exists()) {
                        vaultDir.listFiles()?.forEach { file ->
                            if (file.name.contains("Passport_Scan") ||
                                file.name.contains("Security_Backyard") ||
                                file.name.contains("Voice_Memo") ||
                                file.name.contains("Financial_Audit") ||
                                file.name.contains("security_cam") ||
                                file.name.contains("private_id") ||
                                file.name.contains("confidential")
                            ) {
                                file.delete()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    prefs.hasCleanedLegacySeed = true
                }
            }
        }
    }
}
