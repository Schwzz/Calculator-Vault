package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.DownloadStatus
import com.example.model.VaultDownload
import com.example.model.VaultFileType
import com.example.model.VaultItem
import com.example.model.VaultNote
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    // Vault Items
    @Query("SELECT * FROM vault_items WHERE isTrash = 0 ORDER BY createdAt DESC")
    fun getAllActiveItems(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isTrash = 0 AND fileType = :fileType ORDER BY createdAt DESC")
    fun getItemsByType(fileType: VaultFileType): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isTrash = 1 ORDER BY trashedAt DESC")
    fun getTrashItems(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isTrash = 1")
    suspend fun getTrashItemsList(): List<VaultItem>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): VaultItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<VaultItem>)

    @Update
    suspend fun updateItem(item: VaultItem)

    @Query("UPDATE vault_items SET isTrash = 1, trashedAt = :timestamp WHERE id IN (:ids)")
    suspend fun moveItemsToTrash(ids: List<Long>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vault_items SET isTrash = 0, trashedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreItemsFromTrash(ids: List<Long>)

    @Query("DELETE FROM vault_items WHERE id IN (:ids)")
    suspend fun deleteItemsPermanently(ids: List<Long>)

    @Query("DELETE FROM vault_items WHERE isTrash = 1")
    suspend fun emptyTrashItems()

    @Query("SELECT COUNT(*) FROM vault_items WHERE isTrash = 0 AND fileType = :fileType")
    fun getItemCountByType(fileType: VaultFileType): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM vault_items WHERE isTrash = 0 AND fileType = :fileType")
    fun getTotalSizeByType(fileType: VaultFileType): Flow<Long>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM vault_items WHERE isTrash = 0")
    fun getTotalActiveVaultSize(): Flow<Long>

    // Vault Notes
    @Query("SELECT * FROM vault_notes WHERE isTrash = 0 ORDER BY updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<VaultNote>>

    @Query("SELECT * FROM vault_notes WHERE isTrash = 1 ORDER BY trashedAt DESC")
    fun getTrashNotes(): Flow<List<VaultNote>>

    @Query("SELECT * FROM vault_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): VaultNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VaultNote): Long

    @Update
    suspend fun updateNote(note: VaultNote)

    @Query("UPDATE vault_notes SET isTrash = 1, trashedAt = :timestamp WHERE id = :id")
    suspend fun moveNoteToTrash(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vault_notes SET isTrash = 0, trashedAt = NULL WHERE id = :id")
    suspend fun restoreNote(id: Long)

    @Query("DELETE FROM vault_notes WHERE id = :id")
    suspend fun deleteNotePermanently(id: Long)

    @Query("DELETE FROM vault_notes WHERE isTrash = 1")
    suspend fun emptyTrashNotes()

    // Vault Downloads
    @Query("SELECT * FROM vault_downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<VaultDownload>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: VaultDownload): Long

    @Update
    suspend fun updateDownload(download: VaultDownload)

    @Query("UPDATE vault_downloads SET status = :status WHERE id = :id")
    suspend fun updateDownloadStatus(id: Long, status: DownloadStatus)

    @Query("UPDATE vault_downloads SET progress = :progress, downloadedBytes = :downloadedBytes, speedText = :speedText, status = :status WHERE id = :id")
    suspend fun updateDownloadProgress(id: Long, progress: Float, downloadedBytes: Long, speedText: String, status: DownloadStatus)

    @Query("DELETE FROM vault_downloads WHERE id = :id")
    suspend fun deleteDownload(id: Long)

    @Query("DELETE FROM vault_downloads")
    suspend fun clearAllDownloads()

    @Query("DELETE FROM vault_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM vault_notes")
    suspend fun deleteAllNotes()
}
