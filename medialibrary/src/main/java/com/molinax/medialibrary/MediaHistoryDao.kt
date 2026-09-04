package com.molinax.medialibrary

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MediaHistoryEntry)

    @Query("SELECT * FROM media_history ORDER BY lastPlayedAt DESC")
    fun observeAll(): Flow<List<MediaHistoryEntry>>

    @Query("SELECT * FROM media_history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): MediaHistoryEntry?

    @Query("DELETE FROM media_history WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}
