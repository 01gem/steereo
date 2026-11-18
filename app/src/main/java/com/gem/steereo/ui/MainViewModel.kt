package com.gem.steereo.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gem.steereo.SteereoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
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

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val trackCount: StateFlow<Int> = repository.trackCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private var currentUriString: String? = null
    private var hasAutoScanned = false

    init {
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
        _uiState.value = checkUriPermission(uriStr)
    }

    fun rescan() {
        val readyState = _uiState.value as? UiState.Ready ?: return
        if (_scanState.value is ScanState.Scanning) return
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning(0)
            try {
                repository.rescan(readyState.rootUri) { count ->
                    _scanState.value = ScanState.Scanning(count)
                }
                _scanState.value = ScanState.Done
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
}
