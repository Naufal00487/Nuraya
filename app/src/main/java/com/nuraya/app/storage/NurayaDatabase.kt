package com.nuraya.app.storage

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_entries")
data class UserEntry(
    @PrimaryKey val id: String,
    val type: String,
    val key: String,
    val title: String,
    val subtitle: String = "",
    val extra: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val qariId: String,
    val surahId: Int,
    val title: String,
    val status: String = "queued",
    val progress: Int = 0,
    val filePath: String = "",
    val error: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM user_entries ORDER BY updatedAt DESC, id ASC")
    fun observeAll(): Flow<List<UserEntry>>

    @Query("SELECT * FROM user_entries WHERE type = :type ORDER BY updatedAt DESC, id ASC")
    fun observe(type: String): Flow<List<UserEntry>>

    @Query("SELECT * FROM user_entries WHERE id = :id LIMIT 1")
    suspend fun get(id: String): UserEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: UserEntry)

    @Query("DELETE FROM user_entries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM user_entries WHERE type = :type")
    suspend fun clear(type: String)

    @Query("DELETE FROM user_entries WHERE type = :type AND id NOT IN (SELECT id FROM user_entries WHERE type = :type ORDER BY updatedAt DESC LIMIT :limit)")
    suspend fun trimHistory(type: String, limit: Int = 300)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY updatedAt DESC, id ASC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun get(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE downloads SET status = :status, progress = :progress, filePath = :filePath, error = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateState(
        id: String,
        status: String,
        progress: Int,
        filePath: String,
        error: String,
        updatedAt: Long,
    )
}

@Database(entities = [UserEntry::class, DownloadEntity::class], version = 1, exportSchema = true)
abstract class NurayaDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile private var instance: NurayaDatabase? = null

        fun get(context: Context): NurayaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NurayaDatabase::class.java,
                "nuraya.db",
            ).build().also { instance = it }
        }
    }
}
