package com.gem.steereo.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class UriLastModified(
    val documentUri: String,
    val lastModified: Long
)

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT DISTINCT relativeDir FROM tracks ORDER BY relativeDir COLLATE NOCASE")
    fun observeFolders(): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE relativeDir = :dir ORDER BY title COLLATE NOCASE")
    fun observeByFolder(dir: String): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM tracks")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("SELECT documentUri FROM tracks")
    suspend fun getAllUris(): List<String>

    @Query("SELECT documentUri, lastModified FROM tracks")
    suspend fun getLastModifiedMap(): List<UriLastModified>

    @Query("DELETE FROM tracks WHERE documentUri IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    @Query("DELETE FROM tracks")
    suspend fun clearAll()
}
