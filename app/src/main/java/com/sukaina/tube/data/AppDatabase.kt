package com.sukaina.tube.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val videoId: Long,
    val watchedAt: Long,
    val positionMs: Long,
    val durationMs: Long
)

@Entity(tableName = "reactions")
data class ReactionEntity(
    @PrimaryKey val videoId: Long,
    /** 1 = إعجاب، -1 = عدم إعجاب */
    val value: Int,
    val updatedAt: Long
)

@Entity(tableName = "watch_later")
data class WatchLaterEntity(
    @PrimaryKey val videoId: Long,
    val addedAt: Long
)

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(item: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT :limit")
    suspend fun history(limit: Int = 200): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE videoId = :videoId LIMIT 1")
    suspend fun historyFor(videoId: Long): HistoryEntity?

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("DELETE FROM history WHERE videoId = :videoId")
    suspend fun deleteHistory(videoId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReaction(item: ReactionEntity)

    @Query("DELETE FROM reactions WHERE videoId = :videoId")
    suspend fun deleteReaction(videoId: Long)

    @Query("SELECT * FROM reactions WHERE videoId = :videoId LIMIT 1")
    suspend fun reactionFor(videoId: Long): ReactionEntity?

    @Query("SELECT * FROM reactions WHERE value = 1 ORDER BY updatedAt DESC")
    suspend fun liked(): List<ReactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchLater(item: WatchLaterEntity)

    @Query("DELETE FROM watch_later WHERE videoId = :videoId")
    suspend fun deleteWatchLater(videoId: Long)

    @Query("SELECT * FROM watch_later ORDER BY addedAt DESC")
    suspend fun watchLater(): List<WatchLaterEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE videoId = :videoId)")
    suspend fun inWatchLater(videoId: Long): Boolean
}

@Database(
    entities = [HistoryEntity::class, ReactionEntity::class, WatchLaterEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): VideoDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sukaina-tube.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
