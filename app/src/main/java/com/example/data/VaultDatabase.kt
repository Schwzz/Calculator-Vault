package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.model.DownloadStatus
import com.example.model.VaultDownload
import com.example.model.VaultFileType
import com.example.model.VaultItem
import com.example.model.VaultNote

class Converters {
    @TypeConverter
    fun fromFileType(value: VaultFileType): String = value.name

    @TypeConverter
    fun toFileType(value: String): VaultFileType = try {
        VaultFileType.valueOf(value)
    } catch (e: Exception) {
        VaultFileType.FILE
    }

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = try {
        DownloadStatus.valueOf(value)
    } catch (e: Exception) {
        DownloadStatus.DOWNLOADING
    }
}

@Database(
    entities = [
        VaultItem::class,
        VaultNote::class,
        VaultDownload::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "calculator_vault.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
