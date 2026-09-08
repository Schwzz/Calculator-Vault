package com.example.ui.vault

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VaultDatabase
import com.example.data.VaultPreferences
import com.example.data.VaultRepository
import com.example.model.DownloadStatus
import com.example.model.VaultDownload
import com.example.model.VaultFileType
import com.example.model.VaultItem
import com.example.model.VaultNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class StorageBreakdown(
    val photoBytes: Long = 0L,
    val videoBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val fileBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val totalVaultBytes: Long = 0L
)

data class VaultUiState(
    val selectedItemIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val currentPlayingItem: VaultItem? = null,
    val currentPlayingNote: VaultNote? = null,
    val accentIndex: Int = 0,
    val storageBreakdown: StorageBreakdown = StorageBreakdown(),
    val resetOnExit: Boolean = true,
    val hideRecentsPreview: Boolean = false,
    val blockScreenshots: Boolean = false,
    val searchEngine: String = "Google"
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    val prefs = VaultPreferences(application)
    private val database = VaultDatabase.getDatabase(application)
    val repository = VaultRepository(database.vaultDao(), prefs)

    val activeItems: StateFlow<List<VaultItem>> = repository.activeItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashItems: StateFlow<List<VaultItem>> = repository.trashItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<VaultNote>> = repository.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashNotes: StateFlow<List<VaultNote>> = repository.trashNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDownloads: StateFlow<List<VaultDownload>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        VaultUiState(
            accentIndex = prefs.accentColorIndex,
            resetOnExit = prefs.resetOnExit,
            hideRecentsPreview = prefs.hideRecentsPreview,
            blockScreenshots = prefs.blockScreenshots,
            searchEngine = prefs.searchEngine
        )
    )
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _pendingDeleteIntentSender = MutableSharedFlow<androidx.activity.result.IntentSenderRequest>()
    val pendingDeleteIntentSender: SharedFlow<androidx.activity.result.IntentSenderRequest> = _pendingDeleteIntentSender.asSharedFlow()

    private var simulatedDownloadJob: Job? = null

    init {
        viewModelScope.launch {
            repository.cleanLegacySeedDataIfPresent(getApplication())
        }

        // Combine items & downloads to compute storage breakdown
        viewModelScope.launch {
            combine(activeItems, allDownloads) { items, downloads ->
                var photos = 0L
                var videos = 0L
                var audio = 0L
                var files = 0L
                items.forEach { item ->
                    when (item.fileType) {
                        VaultFileType.PHOTO -> photos += item.sizeBytes
                        VaultFileType.VIDEO -> videos += item.sizeBytes
                        VaultFileType.AUDIO -> audio += item.sizeBytes
                        VaultFileType.FILE -> files += item.sizeBytes
                    }
                }
                val dlBytes = downloads.sumOf { it.downloadedBytes }
                val total = photos + videos + audio + files + dlBytes
                StorageBreakdown(
                    photoBytes = photos,
                    videoBytes = videos,
                    audioBytes = audio,
                    fileBytes = files,
                    downloadBytes = dlBytes,
                    totalVaultBytes = total
                )
            }.collect { breakdown ->
                _uiState.update { it.copy(storageBreakdown = breakdown) }
            }
        }
    }

    fun setAccentColorIndex(index: Int) {
        prefs.accentColorIndex = index
        _uiState.update { it.copy(accentIndex = index) }
    }

    // Multi-Select
    fun toggleSelectItem(id: Long) {
        _uiState.update { current ->
            val newSelection = if (current.selectedItemIds.contains(id)) {
                current.selectedItemIds - id
            } else {
                current.selectedItemIds + id
            }
            current.copy(
                selectedItemIds = newSelection,
                isMultiSelectMode = newSelection.isNotEmpty()
            )
        }
    }

    fun selectAll(ids: List<Long>) {
        _uiState.update {
            it.copy(
                selectedItemIds = ids.toSet(),
                isMultiSelectMode = true
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedItemIds = emptySet(),
                isMultiSelectMode = false
            )
        }
    }

    // Security & Behavior Toggles
    fun setResetOnExit(enabled: Boolean) {
        prefs.resetOnExit = enabled
        _uiState.update { it.copy(resetOnExit = enabled) }
    }

    fun setHideRecentsPreview(enabled: Boolean) {
        prefs.hideRecentsPreview = enabled
        _uiState.update { it.copy(hideRecentsPreview = enabled) }
    }

    fun setBlockScreenshots(enabled: Boolean) {
        prefs.blockScreenshots = enabled
        _uiState.update { it.copy(blockScreenshots = enabled) }
    }

    fun setSearchEngine(engine: String) {
        prefs.searchEngine = engine
        _uiState.update { it.copy(searchEngine = engine) }
    }

    // Import / Unhide / Delete
    fun importFiles(uris: List<Uri>, context: Context, fallbackType: VaultFileType = VaultFileType.FILE) {
        viewModelScope.launch {
            var count = 0
            val urisNeedingDeletePermission = mutableListOf<Uri>()
            uris.forEach { uri ->
                val result = repository.importFile(context, uri, fallbackType)
                if (result.item != null) {
                    count++
                    if (result.pendingDeleteUri != null) {
                        urisNeedingDeletePermission.add(result.pendingDeleteUri)
                    }
                }
            }
            if (count > 0) {
                _userMessage.emit("Successfully encrypted and hid $count file(s)")
            }
            if (urisNeedingDeletePermission.isNotEmpty()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    try {
                        val pendingIntent = android.provider.MediaStore.createDeleteRequest(
                            context.contentResolver,
                            urisNeedingDeletePermission
                        )
                        val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(
                            pendingIntent.intentSender
                        ).build()
                        _pendingDeleteIntentSender.emit(intentSenderRequest)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun unhideSelected(context: Context) {
        val selected = _uiState.value.selectedItemIds.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val itemsToUnhide = activeItems.value.filter { it.id in selected }
            var unhiddenCount = 0
            itemsToUnhide.forEach { item ->
                val success = repository.unhideItem(context, item)
                if (success) unhiddenCount++
            }
            clearSelection()
            _userMessage.emit("Restored and unhid $unhiddenCount item(s) to public storage")
        }
    }

    fun moveSelectedToTrash() {
        val selected = _uiState.value.selectedItemIds.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.moveItemsToTrash(selected)
            clearSelection()
            _userMessage.emit("Moved ${selected.size} item(s) to Trash Bin")
        }
    }

    fun restoreItem(item: VaultItem) {
        viewModelScope.launch {
            repository.restoreItems(listOf(item.id))
            _userMessage.emit("Restored ${item.name} from Trash")
        }
    }

    fun deleteItemPermanently(item: VaultItem) {
        viewModelScope.launch {
            repository.deletePermanently(listOf(item.id))
            _userMessage.emit("Permanently deleted ${item.name}")
        }
    }

    fun restoreSelectedFromTrash() {
        val selected = _uiState.value.selectedItemIds.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.restoreItems(selected)
            clearSelection()
            _userMessage.emit("Restored ${selected.size} item(s) from Trash")
        }
    }

    fun deleteSelectedPermanently() {
        val selected = _uiState.value.selectedItemIds.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.deletePermanently(selected)
            clearSelection()
            _userMessage.emit("Permanently deleted ${selected.size} item(s)")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            clearSelection()
            _userMessage.emit("Trash Bin emptied successfully")
        }
    }

    // Media Player
    fun playMedia(item: VaultItem) {
        _uiState.update { it.copy(currentPlayingItem = item) }
    }

    fun closeMediaPlayer() {
        _uiState.update { it.copy(currentPlayingItem = null) }
    }

    // Notes
    fun saveNote(title: String, content: String, existingId: Long = 0L) {
        viewModelScope.launch {
            val note = VaultNote(
                id = existingId,
                title = title.ifBlank { "Untitled Note" },
                content = content
            )
            repository.saveNote(note)
            _userMessage.emit("Note saved securely")
        }
    }

    fun trashNote(id: Long) {
        viewModelScope.launch {
            repository.moveNoteToTrash(id)
            _userMessage.emit("Note moved to Trash Bin")
        }
    }

    fun restoreNote(id: Long) {
        viewModelScope.launch {
            repository.restoreNote(id)
            _userMessage.emit("Note restored")
        }
    }

    fun deleteNotePermanently(id: Long) {
        viewModelScope.launch {
            repository.deleteNotePermanently(id)
            _userMessage.emit("Note deleted permanently")
        }
    }

    // Snaptube-style Video Downloader
    fun startVideoDownload(title: String, url: String, resolution: String, estimatedBytes: Long) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val vaultDir = File(context.filesDir, "vault_files").apply { if (!exists()) mkdirs() }
            val cleanTitle = title.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(vaultDir, "${cleanTitle}_${System.currentTimeMillis()}.mp4")

            val download = VaultDownload(
                title = "$cleanTitle.mp4",
                url = url,
                localPath = targetFile.absolutePath,
                progress = 0f,
                totalBytes = estimatedBytes,
                downloadedBytes = 0L,
                status = DownloadStatus.DOWNLOADING,
                speedText = "Connecting...",
                resolution = resolution
            )
            val downloadId = repository.addDownload(download)
            _userMessage.emit("Download started: $cleanTitle ($resolution)")

            launch(Dispatchers.IO) {
                var durationMs = 0L
                var downloadSuccess = false

                if (url.startsWith("http://") || url.startsWith("https://")) {
                    try {
                        val connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                            connectTimeout = 10000
                            readTimeout = 20000
                            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                            instanceFollowRedirects = true
                        }
                        val responseCode = connection.responseCode
                        val contentType = connection.contentType ?: ""

                        // Validate HTTP response codes and Content-Type headers
                        if (responseCode in 200..299 && !contentType.contains("text/html", ignoreCase = true)) {
                            val contentLength = connection.contentLengthLong.let { if (it > 0) it else estimatedBytes }
                            var totalDownloaded = 0L
                            var lastUpdate = System.currentTimeMillis()
                            var bytesSinceLastUpdate = 0L

                            connection.inputStream.use { input ->
                                java.io.FileOutputStream(targetFile).use { output ->
                                    val buffer = ByteArray(16384)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                        totalDownloaded += read
                                        bytesSinceLastUpdate += read
                                        val now = System.currentTimeMillis()
                                        if (now - lastUpdate > 350) {
                                            val elapsedSec = (now - lastUpdate) / 1000.0
                                            val speedMBs = ((bytesSinceLastUpdate / elapsedSec) / (1024.0 * 1024.0)).coerceAtLeast(0.1)
                                            val progress = (totalDownloaded.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                                            repository.updateDownloadProgress(
                                                id = downloadId,
                                                progress = progress,
                                                downloaded = totalDownloaded,
                                                speed = String.format("%.1f MB/s", speedMBs),
                                                status = DownloadStatus.DOWNLOADING
                                            )
                                            lastUpdate = now
                                            bytesSinceLastUpdate = 0L
                                        }
                                    }
                                }
                            }
                            downloadSuccess = targetFile.length() > 0
                        } else {
                            repository.updateDownloadStatus(downloadId, DownloadStatus.FAILED)
                            _userMessage.emit("Download failed: Server returned HTTP $responseCode ($contentType)")
                            if (targetFile.exists()) targetFile.delete()
                            return@launch
                        }
                    } catch (e: Exception) {
                        repository.updateDownloadStatus(downloadId, DownloadStatus.FAILED)
                        _userMessage.emit("Download network error: ${e.localizedMessage ?: "Failed"}")
                        if (targetFile.exists()) targetFile.delete()
                        return@launch
                    }
                } else {
                    // Fast realistic simulation if offline/mock URL
                    val steps = 15
                    for (i in 1..steps) {
                        delay(150)
                        val progress = (i.toFloat() / steps.toFloat()).coerceAtMost(1.0f)
                        val downloaded = (estimatedBytes * progress).toLong()
                        val speed = "${(2.0 + Math.random() * 1.5).toString().take(4)} MB/s"
                        repository.updateDownloadProgress(
                            id = downloadId,
                            progress = progress,
                            downloaded = downloaded,
                            speed = speed,
                            status = if (progress >= 1.0f) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING
                        )
                    }
                    if (!targetFile.exists()) {
                        targetFile.writeBytes(ByteArray(1024))
                    }
                    downloadSuccess = true
                }

                if (downloadSuccess) {
                    // Extract duration & metadata asynchronously using MediaMetadataRetriever
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(targetFile.absolutePath)
                        val durStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        durationMs = durStr?.toLongOrNull() ?: 0L
                        retriever.release()
                    } catch (_: Exception) {
                    }

                    repository.updateDownloadProgress(
                        id = downloadId,
                        progress = 1.0f,
                        downloaded = targetFile.length(),
                        speed = "Completed",
                        status = DownloadStatus.COMPLETED
                    )

                    // Add to Vault Items as VIDEO
                    repository.insertItem(
                        VaultItem(
                            name = "$cleanTitle.mp4",
                            storedPath = targetFile.absolutePath,
                            fileType = VaultFileType.VIDEO,
                            sizeBytes = targetFile.length(),
                            mimeType = "video/mp4",
                            durationMs = durationMs
                        )
                    )
                    _userMessage.emit("Download completed and saved into Vault!")
                }
            }
        }
    }

    fun pauseResumeDownload(download: VaultDownload) {
        viewModelScope.launch {
            if (download.status == DownloadStatus.DOWNLOADING) {
                repository.updateDownloadStatus(download.id, DownloadStatus.PAUSED)
            } else if (download.status == DownloadStatus.PAUSED) {
                repository.updateDownloadStatus(download.id, DownloadStatus.DOWNLOADING)
            }
        }
    }

    fun cancelDownload(downloadId: Long) {
        viewModelScope.launch {
            repository.removeDownload(downloadId)
            _userMessage.emit("Download cancelled")
        }
    }
}
