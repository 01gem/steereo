package com.gem.steereo.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import com.gem.steereo.SteereoApp
import com.gem.steereo.data.db.PlaylistEntity
import com.gem.steereo.data.db.PlaylistWithCount
import com.gem.steereo.data.db.TrackEntity
import com.gem.steereo.playback.NowPlaying
import com.gem.steereo.playback.PlayerConnection
import com.gem.steereo.playback.PlaybackIssue
import com.gem.steereo.util.PureRules
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    data class FolderItem(val dir: String, val trackCount: Int)

    sealed interface UiState {
        data object Loading : UiState
        data class NeedsSetup(val message: String?) : UiState
        data class Ready(val rootUri: Uri) : UiState
    }

    sealed interface ScanState {
        data object Idle : ScanState
        data class Scanning(val count: Int) : ScanState
        data object Done : ScanState
        data class Error(val message: String) : ScanState
    }

    private val settings = (app as SteereoApp).settings
    private val repository = (app as SteereoApp).repository

    private val player = PlayerConnection(app)

    val nowPlaying: StateFlow<NowPlaying?> = player.nowPlaying
    val currentUri: StateFlow<String?> = nowPlaying
        .map { it?.mediaId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val isPlaying: StateFlow<Boolean> = player.isPlaying
    val hasPrevious: StateFlow<Boolean> = player.hasPrevious
    val hasNext: StateFlow<Boolean> = player.hasNext
    val positionMs: StateFlow<Long> = player.positionMs
    val repeatMode: StateFlow<Int> = player.repeatMode
    val shuffleEnabled: StateFlow<Boolean> = player.shuffleEnabled

    val appVolume = MutableStateFlow(1f)
    private var volumeBeforeMute = 1f
    private val volumeSaveFlow = MutableSharedFlow<Float>(extraBufferCapacity = 1)

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val playlists: StateFlow<List<PlaylistWithCount>> = repository.playlistsWithCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun tracksOfPlaylist(id: Long): Flow<List<TrackEntity>> = repository.observeTracksOfPlaylist(id)

    fun createPlaylist(name: String, addUri: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val finalName = if (trimmed.length > 50) trimmed.take(50) else trimmed
        viewModelScope.launch {
            val id = repository.createPlaylist(finalName)
            if (addUri != null) {
                repository.addTrackToPlaylist(id, addUri)
                _messages.emit("Added to “$finalName”")
            } else {
                _messages.emit("Created “$finalName”")
            }
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val finalName = if (trimmed.length > 50) trimmed.take(50) else trimmed
        viewModelScope.launch {
            repository.renamePlaylist(id, finalName)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addToPlaylist(playlist: PlaylistEntity, uri: String) {
        viewModelScope.launch {
            val added = repository.addTrackToPlaylist(playlist.id, uri)
            if (added) {
                _messages.emit("Added to “${playlist.name}”")
            } else {
                _messages.emit("Already in “${playlist.name}”")
            }
        }
    }

    fun removeFromPlaylist(playlistId: Long, uri: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, uri)
        }
    }

    val trackCount: StateFlow<Int> = repository.trackCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val tracks: StateFlow<List<TrackEntity>> = repository.tracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val searchQuery = MutableStateFlow("")

    fun onSearchChange(q: String) {
        searchQuery.value = q
    }

    val filteredTracks: StateFlow<List<TrackEntity>> =
        combine(tracks, searchQuery.debounce(150)) { list, q ->
            val query = q.trim()
            if (query.isBlank()) list else list.filter { track ->
                track.title.contains(query, ignoreCase = true) ||
                    track.artist?.contains(query, ignoreCase = true) == true ||
                    track.album?.contains(query, ignoreCase = true) == true
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val folders: StateFlow<List<FolderItem>> = repository.tracks.map { list ->
        PureRules.groupIntoFolders(list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun tracksInFolder(dir: String): Flow<List<TrackEntity>> = repository.observeByFolder(dir)

    val rootLabel: StateFlow<String> = uiState.map { state ->
        if (state is UiState.Ready) {
            val uri = state.rootUri
            try {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                val parts = docId.split(":", limit = 2)
                val volume = parts.getOrNull(0)
                val path = parts.getOrNull(1) ?: ""
                val volLabel = if (volume == "primary") "Internal storage" else "SD card"
                val pathFormatted = if (path.isNotEmpty()) " › " + path.replace("/", " › ") else ""
                volLabel + pathFormatted
            } catch (_: Exception) {
                uri.lastPathSegment ?: "Music folder"
            }
        } else ""
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    fun folderLabel(dir: String): String = PureRules.folderLabel(dir)

    private var currentUriString: String? = null
    private var hasAutoScanned = false

    init {
        viewModelScope.launch {
            appVolume.value = settings.appVolume.first()
        }
        viewModelScope.launch {
            volumeSaveFlow.debounce(300).collect { v ->
                settings.setAppVolume(v)
            }
        }
        viewModelScope.launch {
            settings.rootTreeUri.collect { uriString ->
                currentUriString = uriString
                val newState = checkUriPermission(uriString)
                _uiState.value = newState
                if (newState is UiState.Ready && !hasAutoScanned) {
                    hasAutoScanned = true
                    rescan()
                }
            }
        }
        viewModelScope.launch {
            player.isPlaying.collectLatest { playing ->
                while (playing) {
                    player.refreshPosition()
                    delay(500)
                }
            }
        }
        viewModelScope.launch {
            player.issues.collect { issue ->
                val msg = if (issue.title != null) {
                    "Can't play “${issue.title}” — skipped"
                } else {
                    "Can't play this file"
                }
                _messages.emit(msg)
                if (issue.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
                    issue.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION
                ) {
                    rescan()
                }
            }
        }
    }

    fun play(tracks: List<TrackEntity>, index: Int) = player.playList(tracks, index)
    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(ms: Long) = player.seekTo(ms)

    fun onVolumeChange(v: Float) {
        val clamped = v.coerceIn(0f, 1f)
        appVolume.value = clamped
        player.setVolume(clamped)
        volumeSaveFlow.tryEmit(clamped)
    }

    fun toggleMute() {
        if (appVolume.value > 0f) {
            volumeBeforeMute = appVolume.value
            onVolumeChange(0f)
        } else {
            onVolumeChange(volumeBeforeMute.takeIf { it > 0f } ?: 1f)
        }
    }

    fun cycleRepeat() {
        val next = when (player.repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.setRepeatMode(next)
        viewModelScope.launch { settings.setRepeatMode(next) }
    }

    fun toggleShuffle() {
        val next = !player.shuffleEnabled.value
        player.setShuffle(next)
        viewModelScope.launch { settings.setShuffleEnabled(next) }
    }

    fun onFolderChosen(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            resolver.persistedUriPermissions
                .filter { it.uri != uri }
                .forEach { perm ->
                    try {
                        resolver.releasePersistableUriPermission(
                            perm.uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) { }
                }
            if (uri.toString() != currentUriString) {
                player.stopAndClear()
            }
            currentUriString = uri.toString()
            _uiState.value = UiState.Ready(uri)
            viewModelScope.launch {
                settings.setRootTreeUri(uri.toString())
            }
            rescan()
        } catch (e: SecurityException) {
            _uiState.value = UiState.NeedsSetup(e.localizedMessage ?: "Permission error")
        }
    }

    fun recheckPermission() {
        val uriStr = currentUriString ?: return
        val newState = checkUriPermission(uriStr)
        if (newState is UiState.NeedsSetup) {
            player.stopAndClear()
        }
        _uiState.value = newState
    }

    fun rescan() {
        val readyState = _uiState.value as? UiState.Ready ?: return
        if (_scanState.value is ScanState.Scanning) return
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning(0)
            try {
                val summary = repository.rescan(readyState.rootUri) { count ->
                    _scanState.value = ScanState.Scanning(count)
                }
                if (summary.keptLibrary) {
                    _scanState.value = ScanState.Error("Music folder is empty or unavailable. Library kept — check the folder or SD card, then Rescan.")
                } else {
                    _scanState.value = ScanState.Done
                    if (summary.added > 0 || summary.removed > 0) {
                        _messages.emit("Library updated: +${summary.added}, −${summary.removed}")
                    }
                }
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.localizedMessage ?: "Scan failed")
            }
        }
    }

    private fun checkUriPermission(uriString: String?): UiState {
        if (uriString.isNullOrEmpty()) {
            return UiState.NeedsSetup(null)
        }
        val uri = Uri.parse(uriString)
        val hasPermission = getApplication<Application>().contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == uri && perm.isReadPermission
        }
        return if (hasPermission) {
            UiState.Ready(uri)
        } else {
            UiState.NeedsSetup("Music folder no longer accessible. Pick it again.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
