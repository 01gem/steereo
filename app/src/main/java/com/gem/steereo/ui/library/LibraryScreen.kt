package com.gem.steereo.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gem.steereo.data.db.TrackEntity
import com.gem.steereo.data.prefs.ThemeMode
import com.gem.steereo.ui.MainViewModel
import com.gem.steereo.ui.player.MiniPlayer
import com.gem.steereo.ui.player.NowPlayingSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onChangeFolder: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val filteredTracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentUri by viewModel.currentUri.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val rootLabel by viewModel.rootLabel.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val hasPrevious by viewModel.hasPrevious.collectAsStateWithLifecycle()
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle()
    val appVolume by viewModel.appVolume.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Tracks", "Playlists", "Folders")
    val snackbarHostState = remember { SnackbarHostState() }

    var sheetTrackUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showNewFromSheet by rememberSaveable { mutableStateOf(false) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(nowPlaying) {
        if (nowPlaying == null) showNowPlaying = false
    }

    val context = LocalContext.current
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !askedNotificationPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotificationPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(scanState) {
        if (scanState is MainViewModel.ScanState.Error) {
            snackbarHostState.showSnackbar((scanState as MainViewModel.ScanState.Error).message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Steereo", style = MaterialTheme.typography.titleLarge)
                        if (rootLabel.isNotEmpty()) {
                            Text(
                                text = rootLabel,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = "Theme")
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            listOf(
                                ThemeMode.SYSTEM to "System default",
                                ThemeMode.LIGHT to "Light",
                                ThemeMode.DARK to "Dark"
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    leadingIcon = {
                                        RadioButton(selected = themeMode == mode, onClick = null)
                                    },
                                    onClick = {
                                        showThemeMenu = false
                                        onThemeModeChange(mode)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rescan") },
                            onClick = {
                                showMenu = false
                                viewModel.rescan()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change folder") },
                            onClick = {
                                showMenu = false
                                onChangeFolder()
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            MiniPlayer(
                nowPlaying = nowPlaying,
                isPlaying = isPlaying,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                positionMs = viewModel.positionMs,
                onPrevious = { viewModel.previous() },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.next() },
                onExpand = { showNowPlaying = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val s = scanState
            if (s is MainViewModel.ScanState.Scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Scanning… ${s.count} files",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            val isEmpty = tracks.isEmpty() && scanState !is MainViewModel.ScanState.Scanning
            if (isEmpty) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No music found in this folder")
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { viewModel.rescan() }) {
                                Text("Rescan")
                            }
                            Button(onClick = onChangeFolder) {
                                Text("Change folder")
                            }
                        }
                    }
                }
            } else {
                val onTrackClick: (List<TrackEntity>, Int) -> Unit = { list, index ->
                    requestNotificationPermissionOnce()
                    viewModel.play(list, index)
                }
                val onTrackLongClick: (TrackEntity) -> Unit = { track ->
                    sheetTrackUri = track.documentUri
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTabIndex) {
                        0 -> TracksTab(
                            tracks = filteredTracks,
                            onTrackClick = onTrackClick,
                            onTrackLongClick = onTrackLongClick,
                            searchQuery = searchQuery,
                            onSearchChange = viewModel::onSearchChange,
                            currentUri = currentUri,
                            onTrackMore = onTrackLongClick
                        )
                        1 -> PlaylistsTab(
                            playlists = playlists,
                            tracksOfPlaylist = { viewModel.tracksOfPlaylist(it) },
                            onPlay = onTrackClick,
                            onCreate = { viewModel.createPlaylist(it) },
                            onRename = { id, name -> viewModel.renamePlaylist(id, name) },
                            onDelete = { viewModel.deletePlaylist(it) },
                            onRemoveTrack = { id, uri -> viewModel.removeFromPlaylist(id, uri) },
                            currentUri = currentUri
                        )
                        2 -> FoldersTab(
                            folders = folders,
                            folderLabel = { viewModel.folderLabel(it) },
                            tracksInFolder = { viewModel.tracksInFolder(it) },
                            onTrackClick = onTrackClick,
                            onTrackLongClick = onTrackLongClick,
                            currentUri = currentUri,
                            onTrackMore = onTrackLongClick
                        )
                    }
                }

                val trackUri = sheetTrackUri
                if (trackUri != null && !showNewFromSheet) {
                    AddToPlaylistSheet(
                        playlists = playlists,
                        onPick = { playlist ->
                            viewModel.addToPlaylist(playlist, trackUri)
                            sheetTrackUri = null
                        },
                        onNewPlaylist = {
                            showNewFromSheet = true
                        },
                        onDismiss = {
                            sheetTrackUri = null
                        }
                    )
                }
                if (trackUri != null && showNewFromSheet) {
                    NameDialog(
                        title = "New playlist",
                        initial = "",
                        confirmLabel = "Create",
                        onConfirm = { name ->
                            viewModel.createPlaylist(name, addUri = trackUri)
                            showNewFromSheet = false
                            sheetTrackUri = null
                        },
                        onDismiss = {
                            showNewFromSheet = false
                            sheetTrackUri = null
                        }
                    )
                }
            }
        }
    }

    nowPlaying?.takeIf { showNowPlaying }?.let { activeTrack ->
        NowPlayingSheet(
            nowPlaying = activeTrack,
            isPlaying = isPlaying,
            hasPrevious = hasPrevious,
            hasNext = hasNext,
            positionMs = viewModel.positionMs,
            appVolume = appVolume,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            onSeek = viewModel::seekTo,
            onPrevious = viewModel::previous,
            onPlayPause = viewModel::togglePlayPause,
            onNext = viewModel::next,
            onVolumeChange = viewModel::onVolumeChange,
            onToggleMute = viewModel::toggleMute,
            onCycleRepeat = viewModel::cycleRepeat,
            onToggleShuffle = viewModel::toggleShuffle,
            onDismiss = { showNowPlaying = false }
        )
    }
}
