package com.gem.steereo.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    val trackCount: Int
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT p.*, COUNT(pt.documentUri) AS trackCount FROM playlists p LEFT JOIN playlist_tracks pt ON pt.playlistId = p.id GROUP BY p.id ORDER BY p.name COLLATE NOCASE")
    fun observePlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT t.* FROM tracks t INNER JOIN playlist_tracks pt ON t.documentUri = pt.documentUri WHERE pt.playlistId = :id ORDER BY pt.position ASC")
    fun observeTracksOfPlaylist(id: Long): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrack(crossRef: PlaylistTrackCrossRef): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND documentUri = :documentUri")
    suspend fun removeTrack(playlistId: Long, documentUri: String)

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int?
}
