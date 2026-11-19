package com.gem.steereo.data

import android.net.Uri
import com.gem.steereo.data.db.AppDatabase
import com.gem.steereo.data.db.PlaylistEntity
import com.gem.steereo.data.db.PlaylistWithCount
import com.gem.steereo.data.db.PlaylistTrackCrossRef
import com.gem.steereo.data.db.TrackEntity
import com.gem.steereo.data.scan.MusicScanner
import com.gem.steereo.util.PureRules
import kotlinx.coroutines.flow.Flow

data class RescanSummary(
    val found: Int,
    val added: Int,
    val removed: Int,
    val keptLibrary: Boolean
)

class MusicRepository(
    private val db: AppDatabase,
    private val scanner: MusicScanner
) {
    val tracks: Flow<List<TrackEntity>> = db.trackDao().observeAll()
    val folders: Flow<List<String>> = db.trackDao().observeFolders()
    val trackCount: Flow<Int> = db.trackDao().observeCount()

    fun observeByFolder(dir: String): Flow<List<TrackEntity>> = db.trackDao().observeByFolder(dir)

    suspend fun rescan(treeUri: Uri, onProgress: (Int) -> Unit): RescanSummary {
        val knownMap = db.trackDao().getLastModifiedMap().associate { it.documentUri to it.lastModified }
        val result = scanner.scan(treeUri, knownMap, onProgress)
        val added = result.changed.count { it.documentUri !in knownMap }
        if (result.changed.isNotEmpty()) {
            db.trackDao().upsertAll(result.changed)
        }
        val allUris = db.trackDao().getAllUris()
        if (result.seenUris.isEmpty() && allUris.isNotEmpty()) {
            return RescanSummary(
                found = 0,
                added = 0,
                removed = 0,
                keptLibrary = true
            )
        }
        val deletedUris = allUris.filter { it !in result.seenUris }
        if (deletedUris.isNotEmpty()) {
            PureRules.chunkForInClause(deletedUris).forEach { chunk ->
                db.trackDao().deleteByUris(chunk)
            }
        }
        return RescanSummary(
            found = result.seenUris.size,
            added = added,
            removed = deletedUris.size,
            keptLibrary = false
        )
    }

    val playlists: Flow<List<PlaylistEntity>> = db.playlistDao().observePlaylists()
    val playlistsWithCount: Flow<List<PlaylistWithCount>> = db.playlistDao().observePlaylistsWithCount()

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

    suspend fun addTrackToPlaylist(playlistId: Long, documentUri: String): Boolean {
        val maxPos = db.playlistDao().maxPosition(playlistId) ?: -1
        val rowId = db.playlistDao().addTrack(PlaylistTrackCrossRef(playlistId, documentUri, maxPos + 1))
        return rowId != -1L
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, documentUri: String) {
        db.playlistDao().removeTrack(playlistId, documentUri)
    }
}
