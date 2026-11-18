package com.gem.steereo.data

import android.net.Uri
import com.gem.steereo.data.db.AppDatabase
import com.gem.steereo.data.db.PlaylistEntity
import com.gem.steereo.data.db.PlaylistTrackCrossRef
import com.gem.steereo.data.db.TrackEntity
import com.gem.steereo.data.scan.MusicScanner
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    private val db: AppDatabase,
    private val scanner: MusicScanner
) {
    val tracks: Flow<List<TrackEntity>> = db.trackDao().observeAll()
    val folders: Flow<List<String>> = db.trackDao().observeFolders()
    val trackCount: Flow<Int> = db.trackDao().observeCount()

    fun observeByFolder(dir: String): Flow<List<TrackEntity>> = db.trackDao().observeByFolder(dir)

    suspend fun rescan(treeUri: Uri, onProgress: (Int) -> Unit) {
        val knownMap = db.trackDao().getLastModifiedMap().associate { it.documentUri to it.lastModified }
        val result = scanner.scan(treeUri, knownMap, onProgress)
        if (result.changed.isNotEmpty()) {
            db.trackDao().upsertAll(result.changed)
        }
        val allUris = db.trackDao().getAllUris()
        val deletedUris = allUris.filter { it !in result.seenUris }
        if (deletedUris.isNotEmpty()) {
            deletedUris.chunked(500).forEach { chunk ->
                db.trackDao().deleteByUris(chunk)
            }
        }
    }

    val playlists: Flow<List<PlaylistEntity>> = db.playlistDao().observePlaylists()

    suspend fun createPlaylist(name: String): Long {
        return db.playlistDao().insert(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))
    }

    suspend fun renamePlaylist(id: Long, name: String) {
        db.playlistDao().rename(id, name)
    }

    suspend fun deletePlaylist(id: Long) {
        db.playlistDao().delete(id)
    }

    fun observeTracksOfPlaylist(id: Long): Flow<List<TrackEntity>> {
        return db.playlistDao().observeTracksOfPlaylist(id)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, documentUri: String) {
        val maxPos = db.playlistDao().maxPosition(playlistId) ?: -1
        db.playlistDao().addTrack(PlaylistTrackCrossRef(playlistId, documentUri, maxPos + 1))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, documentUri: String) {
        db.playlistDao().removeTrack(playlistId, documentUri)
    }
}
