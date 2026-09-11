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
                
                // Stage 1: Copy to temporary file
                val tempFile = File(vaultDir, "${UUID.randomUUID()}.tmp")
                val inStream = contentResolver.openInputStream(uri) ?: return@withContext ImportResult(item = null)
                val bytesCopied = inStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Stage 2: Transactional verification of stored file integrity
                if (!tempFile.exists() || tempFile.length() <= 0L || (fileSize > 0L && bytesCopied != fileSize && bytesCopied <= 0L)) {
                    tempFile.delete()
                    return@withContext ImportResult(item = null)
                }

                val actualSize = tempFile.length()
                val finalFile = File(vaultDir, "${UUID.randomUUID()}$ext")
                val renamed = tempFile.renameTo(finalFile)
                val storedFile = if (renamed) finalFile else tempFile

                // Stage 3: Record item in database
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

                // Stage 4: Attempt removal of original public file
                var pendingDeleteUri: Uri? = null
                var deletedImmediately = false

                try {
                    if (android.provider.DocumentsContract.isDocumentUri(context, uri)) {
                        deletedImmediately = android.provider.DocumentsContract.deleteDocument(contentResolver, uri)
                    }
                } catch (_: Exception) {
                }

                if (!deletedImmediately) {
                    try {
                        val deletedRows = contentResolver.delete(uri, null, null)
                        if (deletedRows > 0) {
                            deletedImmediately = true
                        } else {
                            pendingDeleteUri = uri
                        }
                    } catch (_: SecurityException) {
                        pendingDeleteUri = uri
                    } catch (_: Exception) {
                        pendingDeleteUri = uri
                    }
                }

                ImportResult(item = item.copy(id = id), pendingDeleteUri = if (deletedImmediately) null else pendingDeleteUri)
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
                val collectionUri = when (item.fileType) {
                    VaultFileType.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    VaultFileType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    VaultFileType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    VaultFileType.FILE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Downloads.EXTERNAL_CONTENT_URI else null
                }

                // Duplicate check: Check if identical file already exists in public storage
                var alreadyInPublicStorage = false
                if (collectionUri != null) {
                    try {
                        val projection = arrayOf(
                            MediaStore.MediaColumns._ID,
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            MediaStore.MediaColumns.SIZE
                        )
                        resolver.query(
                            collectionUri,
                            projection,
                            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                            arrayOf(item.name),
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                                val existingSize = if (sizeCol != -1) cursor.getLong(sizeCol) else -1L
                                if (existingSize == sourceFile.length()) {
                                    alreadyInPublicStorage = true
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                } else {
                    val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val existingFile = File(publicDir, item.name)
                    if (existingFile.exists() && existingFile.length() == sourceFile.length()) {
                        alreadyInPublicStorage = true
                    }
                }

                // If identical file already exists in public storage, safely remove vault copy without creating duplicate
                if (alreadyInPublicStorage) {
                    sourceFile.delete()
                    dao.deleteItemsPermanently(listOf(item.id))
                    return@withContext true
                }

                // Disambiguate filename if name exists but size is different
                var restoreDisplayName = item.name
                if (collectionUri != null) {
                    var nameConflict = false
                    try {
                        resolver.query(
                            collectionUri,
                            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                            arrayOf(restoreDisplayName),
                            null
                        )?.use { cursor ->
                            if (cursor.count > 0) nameConflict = true
                        }
                    } catch (_: Exception) {
                    }
                    if (nameConflict) {
                        val dot = item.name.lastIndexOf('.')
                        val base = if (dot != -1) item.name.substring(0, dot) else item.name
                        val ext = if (dot != -1) item.name.substring(dot) else ""
                        restoreDisplayName = "${base}_restored$ext"
                    }
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, restoreDisplayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (item.mimeType.isNotEmpty()) item.mimeType else "application/octet-stream")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val targetUri = if (collectionUri != null) resolver.insert(collectionUri, contentValues) else null
                var bytesCopied = 0L
                var copySuccessful = false

                if (targetUri != null) {
                    try {
                        val outStream = resolver.openOutputStream(targetUri)
                        if (outStream != null) {
                            outStream.use { output ->
                                sourceFile.inputStream().use { inStream ->
                                    bytesCopied = inStream.copyTo(output)
                                }
                                output.flush()
                            }
                            if (bytesCopied == sourceFile.length() || (sourceFile.length() == 0L && bytesCopied == 0L)) {
                                copySuccessful = true
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    contentValues.clear()
                                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                    resolver.update(targetUri, contentValues, null, null)
                                }
                            } else {
                                resolver.delete(targetUri, null, null)
                            }
                        }
                    } catch (e: Exception) {
                        try { resolver.delete(targetUri, null, null) } catch (_: Exception) {}
                        copySuccessful = false
                    }
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && item.fileType == VaultFileType.FILE) {
                    val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!publicDir.exists()) publicDir.mkdirs()
                    val targetFile = File(publicDir, restoreDisplayName)
                    try {
                        sourceFile.copyTo(targetFile, overwrite = false)
                        if (targetFile.length() == sourceFile.length()) {
                            copySuccessful = true
                        }
                    } catch (_: Exception) {
                        copySuccessful = false
                    }
                }

                if (copySuccessful) {
                    // Only delete private copy and database item after successful verified write
                    sourceFile.delete()
                    dao.deleteItemsPermanently(listOf(item.id))
                    true
                } else {
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
