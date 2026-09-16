package com.example.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
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
    val originalDeleted: Boolean = false,
    val pendingDeleteUri: Uri? = null,
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

    private fun isCanonicalMediaStoreUri(uri: Uri): Boolean {
        if (uri.authority != "media") return false
        val path = uri.path ?: return false
        if (path.contains("picker")) return false
        return path.matches(Regex("^/external[^/]*/(images|video|audio|file|files)(/media)?/\\d+$"))
    }

    private fun resolveToMediaStoreUri(
        context: Context,
        uri: Uri,
        fileType: VaultFileType = VaultFileType.FILE,
        fileName: String = "",
        fileSize: Long = 0L
    ): Uri? {
        // 1. If it is already a canonical MediaStore URI, return it directly
        if (isCanonicalMediaStoreUri(uri)) {
            return uri
        }

        val contentResolver = context.contentResolver

        // 2. If it is a DocumentsContract document URI
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val authority = uri.authority

            // MediaDocumentsProvider
            if (authority == "com.android.providers.media.documents") {
                val parts = docId.split(":")
                if (parts.size >= 2) {
                    val type = parts[0]
                    val id = parts[1].toLongOrNull()
                    if (id != null) {
                        return when (type) {
                            "image" -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                            "video" -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                            "audio" -> ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            else -> ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                        }
                    }
                } else {
                    val id = docId.toLongOrNull()
                    if (id != null) {
                        return when (fileType) {
                            VaultFileType.PHOTO -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                            VaultFileType.VIDEO -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                            VaultFileType.AUDIO -> ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            VaultFileType.FILE -> ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                        }
                    }
                }
            }

            // ExternalStorageProvider (primary:DCIM/Camera/...)
            if (authority == "com.android.externalstorage.documents") {
                val subPath = docId.substringAfter(":")
                val externalPath = "/storage/emulated/0/$subPath"
                val tableUri = when (fileType) {
                    VaultFileType.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    VaultFileType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    VaultFileType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    VaultFileType.FILE -> MediaStore.Files.getContentUri("external")
                }
                try {
                    contentResolver.query(
                        tableUri,
                        arrayOf(MediaStore.MediaColumns._ID),
                        "${MediaStore.MediaColumns.DATA} = ?",
                        arrayOf(externalPath),
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(0)
                            return ContentUris.withAppendedId(tableUri, id)
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Photo Picker URI or query by candidate IDs
        val targetTableUri = when (fileType) {
            VaultFileType.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            VaultFileType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            VaultFileType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            VaultFileType.FILE -> MediaStore.Files.getContentUri("external")
        }

        val rawId = uri.lastPathSegment?.toLongOrNull()
        if (rawId != null) {
            val candidateIds = mutableListOf(rawId)
            if (rawId >= 1_000_000_000L) {
                candidateIds.add(rawId % 1_000_000_000L)
            }
            for (cId in candidateIds) {
                val testUri = ContentUris.withAppendedId(targetTableUri, cId)
                try {
                    contentResolver.query(testUri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            return testUri
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 4. Try matching in MediaStore by fileName & fileSize if known
        if (fileName.isNotBlank()) {
            try {
                val selection = if (fileSize > 0L) {
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?"
                } else {
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                }
                val args = if (fileSize > 0L) arrayOf(fileName, fileSize.toString()) else arrayOf(fileName)
                contentResolver.query(
                    targetTableUri,
                    arrayOf(MediaStore.MediaColumns._ID),
                    selection,
                    args,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        return ContentUris.withAppendedId(targetTableUri, id)
                    }
                }
            } catch (_: Exception) {}
        }

        return null
    }

    suspend fun importFile(context: Context, uri: Uri, fallbackType: VaultFileType = VaultFileType.FILE): ImportResult {
        return withContext(Dispatchers.IO) {
            val vaultDir = File(context.filesDir, "vault_files").apply { if (!exists()) mkdirs() }
            var tempFile: File? = null
            try {
                val contentResolver = context.contentResolver
                var fileName = "vault_file_${System.currentTimeMillis()}"
                var fileSize = 0L
                var mimeType = contentResolver.getType(uri) ?: ""

                // Step 1: Query content resolver for metadata
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) {
                                val name = cursor.getString(nameIndex)
                                if (!name.isNullOrBlank()) fileName = name
                            }
                            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("VaultRepository", "Could not query URI metadata: ${e.message}")
                }

                val ext = if (fileName.contains(".")) fileName.substring(fileName.lastIndexOf(".")) else ""
                if (mimeType.isBlank() && ext.isNotEmpty()) {
                    val cleanExt = ext.removePrefix(".").lowercase()
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(cleanExt) ?: ""
                }

                val detectedType = when {
                    mimeType.startsWith("image/") || ext.matches(Regex("\\.(jpg|jpeg|png|webp|gif|bmp|heic)", RegexOption.IGNORE_CASE)) -> VaultFileType.PHOTO
                    mimeType.startsWith("video/") || ext.matches(Regex("\\.(mp4|mkv|webm|mov|3gp|avi)", RegexOption.IGNORE_CASE)) -> VaultFileType.VIDEO
                    mimeType.startsWith("audio/") || ext.matches(Regex("\\.(mp3|wav|m4a|aac|flac|ogg)", RegexOption.IGNORE_CASE)) -> VaultFileType.AUDIO
                    else -> fallbackType
                }

                if (mimeType.isBlank()) {
                    mimeType = when (detectedType) {
                        VaultFileType.PHOTO -> "image/jpeg"
                        VaultFileType.VIDEO -> "video/mp4"
                        VaultFileType.AUDIO -> "audio/mpeg"
                        VaultFileType.FILE -> "application/octet-stream"
                    }
                }

                // Step 2: Stage source into temporary Vault file
                val staged = File(vaultDir, "${UUID.randomUUID()}.tmp")
                tempFile = staged
                val inStream = try {
                    contentResolver.openInputStream(uri)
                } catch (e: Exception) {
                    Log.e("VaultRepository", "Failed to open inputStream for $uri", e)
                    null
                }

                if (inStream == null) {
                    staged.delete()
                    return@withContext ImportResult(item = null, errorMessage = "Could not open source file")
                }

                val bytesCopied = inStream.use { input ->
                    FileOutputStream(staged).use { output ->
                        input.copyTo(output)
                    }
                }

                // Step 3: Transactional verification of stored file integrity
                if (!staged.exists() || staged.length() <= 0L || (fileSize > 0L && bytesCopied != fileSize && bytesCopied <= 0L)) {
                    staged.delete()
                    return@withContext ImportResult(item = null, errorMessage = "File integrity verification failed")
                }

                val actualSize = staged.length()
                val finalFile = File(vaultDir, "${UUID.randomUUID()}$ext")
                val renamed = staged.renameTo(finalFile)
                val storedFile = if (renamed) finalFile else staged
                if (!storedFile.exists() || storedFile.length() <= 0L) {
                    storedFile.delete()
                    return@withContext ImportResult(item = null, errorMessage = "Could not store file in Vault")
                }

                // Extract video duration if it's a video
                var durationMs = 0L
                if (detectedType == VaultFileType.VIDEO) {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(storedFile.absolutePath)
                        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        durationMs = time?.toLongOrNull() ?: 0L
                        retriever.release()
                    } catch (_: Exception) {}
                }

                // Step 4: Finalize database record once Vault copy is established
                val item = VaultItem(
                    name = fileName,
                    originalPath = uri.toString(),
                    storedPath = storedFile.absolutePath,
                    fileType = detectedType,
                    sizeBytes = actualSize,
                    mimeType = mimeType,
                    durationMs = durationMs,
                    createdAt = System.currentTimeMillis()
                )
                val id = dao.insertItem(item)
                val insertedItem = item.copy(id = id)

                // Step 5: Only AFTER successful verification and Vault establishment, attempt to remove original
                var originalDeleted = false
                try {
                    if (DocumentsContract.isDocumentUri(context, uri)) {
                        originalDeleted = DocumentsContract.deleteDocument(contentResolver, uri)
                    }
                } catch (_: Exception) {}

                val resolvedMediaStoreUri = resolveToMediaStoreUri(context, uri, detectedType, fileName, actualSize)
                if (!originalDeleted && resolvedMediaStoreUri != null) {
                    try {
                        val deletedRows = contentResolver.delete(resolvedMediaStoreUri, null, null)
                        if (deletedRows > 0) {
                            originalDeleted = true
                        }
                    } catch (_: Exception) {}
                }

                if (!originalDeleted && !DocumentsContract.isDocumentUri(context, uri) && !isCanonicalMediaStoreUri(uri)) {
                    try {
                        val deletedRows = contentResolver.delete(uri, null, null)
                        if (deletedRows > 0) {
                            originalDeleted = true
                        }
                    } catch (_: Exception) {}
                }

                if (originalDeleted) {
                    ImportResult(item = insertedItem, originalDeleted = true)
                } else if (resolvedMediaStoreUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ImportResult(item = insertedItem, originalDeleted = false, pendingDeleteUri = resolvedMediaStoreUri)
                } else {
                    ImportResult(item = insertedItem, originalDeleted = false, pendingDeleteUri = null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tempFile?.delete()
                ImportResult(item = null, errorMessage = e.message)
            }
        }
    }

    suspend fun renameItem(id: Long, newName: String) {
        withContext(Dispatchers.IO) {
            val item = dao.getItemById(id) ?: return@withContext
            val trimmed = newName.trim()
            if (trimmed.isNotEmpty()) {
                dao.updateItem(item.copy(name = trimmed))
            }
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

                // STEP 1: Safely check whether a public source already exists before creating any new file
                var originalPublicExists = false
                var verifiedExistingUri: Uri? = null

                if (item.originalPath.isNotBlank()) {
                    try {
                        val origUri = Uri.parse(item.originalPath)
                        resolver.openInputStream(origUri)?.use { stream ->
                            val firstByte = stream.read()
                            if (firstByte != -1) {
                                originalPublicExists = true
                                verifiedExistingUri = origUri
                            }
                        }
                    } catch (_: Exception) {}

                    if (!originalPublicExists) {
                        try {
                            val resolvedOrig = resolveToMediaStoreUri(
                                context,
                                Uri.parse(item.originalPath),
                                item.fileType,
                                item.name,
                                item.sizeBytes
                            )
                            if (resolvedOrig != null) {
                                resolver.openInputStream(resolvedOrig)?.use { stream ->
                                    val firstByte = stream.read()
                                    if (firstByte != -1) {
                                        originalPublicExists = true
                                        verifiedExistingUri = resolvedOrig
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    if (!originalPublicExists && (item.originalPath.startsWith("/") || item.originalPath.startsWith("file://"))) {
                        val path = if (item.originalPath.startsWith("file://")) {
                            Uri.parse(item.originalPath).path ?: ""
                        } else {
                            item.originalPath
                        }
                        val f = File(path)
                        if (f.exists() && f.length() > 0L) {
                            originalPublicExists = true
                        }
                    }
                }

                // Also check if an identical file with same name and size exists in public storage
                if (!originalPublicExists && collectionUri != null) {
                    try {
                        resolver.query(
                            collectionUri,
                            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE),
                            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                            arrayOf(item.name),
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                                val existingSize = if (sizeCol != -1) cursor.getLong(sizeCol) else -1L
                                val existingId = if (idCol != -1) cursor.getLong(idCol) else -1L
                                if (existingSize == sourceFile.length() || (item.sizeBytes > 0L && existingSize == item.sizeBytes)) {
                                    originalPublicExists = true
                                    if (existingId != -1L) {
                                        verifiedExistingUri = ContentUris.withAppendedId(collectionUri, existingId)
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // If public copy already exists: do NOT create a duplicate!
                if (originalPublicExists) {
                    var publicValid = true
                    if (verifiedExistingUri != null) {
                        try {
                            resolver.openInputStream(verifiedExistingUri!!)?.use { s ->
                                publicValid = (s.read() != -1)
                            }
                        } catch (_: Exception) {
                            publicValid = false
                        }
                    }
                    if (publicValid) {
                        // Public already has exactly 1 copy. Remove vault copy & record cleanly.
                        sourceFile.delete()
                        dao.deleteItemsPermanently(listOf(item.id))
                        return@withContext true
                    }
                }

                // STEP 2: Restore to public gallery/storage when no public item exists
                var destinationName = item.name
                val dotIndex = item.name.lastIndexOf('.')
                val baseName = if (dotIndex != -1) item.name.substring(0, dotIndex) else item.name
                val ext = if (dotIndex != -1) item.name.substring(dotIndex) else ""

                if (collectionUri != null) {
                    try {
                        resolver.query(
                            collectionUri,
                            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                            arrayOf(destinationName),
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                destinationName = "${baseName}_restored_${System.currentTimeMillis()}$ext"
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
                        if (bytesWritten == sourceFile.length() || (sourceFile.length() == 0L && bytesWritten == 0L)) {
                            writeSuccess = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                contentValues.clear()
                                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                resolver.update(targetUri, contentValues, null, null)
                            }
                            // Verify restored public copy can be opened and read
                            resolver.openInputStream(targetUri)?.use { s ->
                                if (s.read() == -1 && sourceFile.length() > 0L) {
                                    writeSuccess = false
                                }
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
                    if (targetFile.exists()) {
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
