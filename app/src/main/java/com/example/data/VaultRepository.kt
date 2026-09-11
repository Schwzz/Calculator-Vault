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
    val pendingDeleteUri: Uri? = null,
    val stagedFile: File? = null,
    val errorMessage: String? = null
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
            val vaultDir = File(context.filesDir, "vault_files").apply { if (!exists()) mkdirs() }
            var tempFile: File? = null
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

                val ext = if (fileName.contains(".")) fileName.substring(fileName.lastIndexOf(".")) else ""
                
                // Stage 1: Copy source into temporary Vault file
                val staged = File(vaultDir, "${UUID.randomUUID()}.tmp")
                tempFile = staged
                val inStream = contentResolver.openInputStream(uri) ?: run {
                    staged.delete()
                    return@withContext ImportResult(item = null, errorMessage = "Could not open source")
                }
                val bytesCopied = inStream.use { input ->
                    FileOutputStream(staged).use { output ->
                        input.copyTo(output)
                    }
                }

                // Stage 2: Transactional verification of stored file integrity
                if (!staged.exists() || staged.length() <= 0L || (fileSize > 0L && bytesCopied != fileSize && bytesCopied <= 0L)) {
                    staged.delete()
                    return@withContext ImportResult(item = null, errorMessage = "Verification failed")
                }

                val actualSize = staged.length()
                val finalFile = File(vaultDir, "${UUID.randomUUID()}$ext")

                // Stage 3: Attempt immediate removal of original public source
                var deletedImmediately = false
                try {
                    if (android.provider.DocumentsContract.isDocumentUri(context, uri)) {
                        deletedImmediately = android.provider.DocumentsContract.deleteDocument(contentResolver, uri)
                    }
                } catch (_: Exception) {}

                if (!deletedImmediately) {
                    try {
                        val deletedRows = contentResolver.delete(uri, null, null)
                        if (deletedRows > 0) {
                            deletedImmediately = true
                        }
                    } catch (_: Exception) {}
                }

                if (deletedImmediately) {
                    // Source removed immediately! Finalize import: move file & insert into DB
                    val renamed = staged.renameTo(finalFile)
                    val storedFile = if (renamed) finalFile else staged
                    val item = VaultItem(
                        name = fileName,
                        originalPath = uri.toString(),
                        storedPath = storedFile.absolutePath,
                        fileType = detectedType,
                        sizeBytes = actualSize,
                        mimeType = mimeType,
                        createdAt = System.currentTimeMillis()
                    )
                    val id = dao.insertItem(item)
                    ImportResult(item = item.copy(id = id), pendingDeleteUri = null)
                } else {
                    // Public source requires user permission to delete (Android MediaStore delete request)
                    // DO NOT insert into DB yet to avoid duplicates. Keep staged file pending confirmation.
                    val pendingItem = VaultItem(
                        name = fileName,
                        originalPath = uri.toString(),
                        storedPath = finalFile.absolutePath,
                        fileType = detectedType,
                        sizeBytes = actualSize,
                        mimeType = mimeType,
                        createdAt = System.currentTimeMillis()
                    )
                    ImportResult(
                        item = pendingItem,
                        pendingDeleteUri = uri,
                        stagedFile = staged
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tempFile?.delete()
                ImportResult(item = null, errorMessage = e.message)
            }
        }
    }

    suspend fun finalizePendingImport(pendingItem: VaultItem, stagedFile: File): VaultItem? {
        return withContext(Dispatchers.IO) {
            try {
                val finalFile = File(pendingItem.storedPath)
                val moved = stagedFile.renameTo(finalFile)
                val storedFile = if (moved) finalFile else stagedFile
                val itemToInsert = pendingItem.copy(storedPath = storedFile.absolutePath)
                val id = dao.insertItem(itemToInsert)
                itemToInsert.copy(id = id)
            } catch (e: Exception) {
                e.printStackTrace()
                stagedFile.delete()
                null
            }
        }
    }

    suspend fun cancelPendingImport(stagedFile: File?) {
        withContext(Dispatchers.IO) {
            try {
                stagedFile?.delete()
            } catch (_: Exception) {}
        }
    }

    suspend fun unhideItem(context: Context, item: VaultItem): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(item.storedPath)
                if (!sourceFile.exists() || sourceFile.length() <= 0L) {
                    return@withContext false
                }

                val resolver = context.contentResolver
                val collectionUri = when (item.fileType) {
                    VaultFileType.PHOTO -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    VaultFileType.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    VaultFileType.AUDIO -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    VaultFileType.FILE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else null
                }

                var destinationName = item.name
                val dotIndex = item.name.lastIndexOf('.')
                val baseName = if (dotIndex != -1) item.name.substring(0, dotIndex) else item.name
                val ext = if (dotIndex != -1) item.name.substring(dotIndex) else ""

                // Check if file with same name already exists in public storage
                if (collectionUri != null) {
                    try {
                        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE)
                        resolver.query(
                            collectionUri,
                            projection,
                            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                            arrayOf(destinationName),
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                                val existingSize = if (sizeCol != -1) cursor.getLong(sizeCol) else -1L
                                if (existingSize == sourceFile.length()) {
                                    // An identical public copy already exists! Safely remove vault duplicate
                                    sourceFile.delete()
                                    dao.deleteItemsPermanently(listOf(item.id))
                                    return@withContext true
                                } else {
                                    // A different file has the same name -> disambiguate deterministically
                                    destinationName = "${baseName}_restored_${System.currentTimeMillis()}$ext"
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, destinationName)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (item.mimeType.isNotEmpty()) item.mimeType else "application/octet-stream")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val targetUri = if (collectionUri != null) resolver.insert(collectionUri, contentValues) else null
                var bytesWritten = 0L
                var writeSuccess = false

                if (targetUri != null) {
                    try {
                        resolver.openOutputStream(targetUri)?.use { output ->
                            sourceFile.inputStream().use { input ->
                                bytesWritten = input.copyTo(output)
                            }
                            output.flush()
                        }
                        // Verify restored file exists and has valid content
                        if (bytesWritten == sourceFile.length() || (sourceFile.length() == 0L && bytesWritten == 0L)) {
                            writeSuccess = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                contentValues.clear()
                                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                resolver.update(targetUri, contentValues, null, null)
                            }
                        } else {
                            resolver.delete(targetUri, null, null)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try { resolver.delete(targetUri, null, null) } catch (_: Exception) {}
                        writeSuccess = false
                    }
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && item.fileType == VaultFileType.FILE) {
                    val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!publicDir.exists()) publicDir.mkdirs()
                    var targetFile = File(publicDir, destinationName)
                    if (targetFile.exists() && targetFile.length() == sourceFile.length()) {
                        sourceFile.delete()
                        dao.deleteItemsPermanently(listOf(item.id))
                        return@withContext true
                    } else if (targetFile.exists()) {
                        targetFile = File(publicDir, "${baseName}_restored_${System.currentTimeMillis()}$ext")
                    }
                    try {
                        sourceFile.copyTo(targetFile, overwrite = false)
                        if (targetFile.length() == sourceFile.length()) {
                            writeSuccess = true
                        }
                    } catch (_: Exception) {
                        writeSuccess = false
                    }
                }

                if (writeSuccess) {
                    // Only after successful verified write do we delete the Vault file and DB entry!
                    sourceFile.delete()
                    dao.deleteItemsPermanently(listOf(item.id))
                    true
                } else {
                    // Keep vault item and file intact!
                    false
                }
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
            // 1. Fetch all items in trash and physically delete their files from disk
            try {
                val trashItems = dao.getTrashItemsList()
                trashItems.forEach { item ->
                    try {
                        val file = File(item.storedPath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Clear trash records in database
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
