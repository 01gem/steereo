package com.gem.steereo.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gem.steereo.data.db.PlaylistEntity
import com.gem.steereo.data.db.PlaylistWithCount
import com.gem.steereo.data.db.TrackEntity
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsTab(
    playlists: List<PlaylistWithCount>,
    tracksOfPlaylist: (Long) -> Flow<List<TrackEntity>>,
    onPlay: (List<TrackEntity>, Int) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onRemoveTrack: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var deletingPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var menuTargetId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(playlists, selectedId) {
        if (selectedId != null && playlists.none { it.playlist.id == selectedId }) {
            selectedId = null
        }
    }

    if (selectedId == null) {
        Box(modifier = modifier.fillMaxSize()) {
            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No playlists yet")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(playlists, key = { it.playlist.id }) { item ->
                        Box {
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null
                                    )
                                },
                                headlineContent = { Text(item.playlist.name) },
                                supportingContent = { Text("${item.trackCount} tracks") },
                                modifier = Modifier.combinedClickable(
                                    onClick = { selectedId = item.playlist.id },
                                    onLongClick = { menuTargetId = item.playlist.id }
                                )
                            )
                            DropdownMenu(
                                expanded = menuTargetId == item.playlist.id,
                                onDismissRequest = { menuTargetId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        menuTargetId = null
                                        renamingPlaylist = item.playlist
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        menuTargetId = null
                                        deletingPlaylist = item.playlist
                                    }
                                )
                            }
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                text = { Text("New playlist") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    } else {
        BackHandler(enabled = true) {
            selectedId = null
        }
        val currentPlaylist = playlists.find { it.playlist.id == selectedId }?.playlist
        val trackFlow = remember(selectedId) { tracksOfPlaylist(selectedId!!) }
        val playlistTracks by trackFlow.collectAsStateWithLifecycle(initialValue = emptyList())

        Column(modifier = modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                IconButton(onClick = { selectedId = null }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = currentPlaylist?.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                FilledTonalButton(
                    onClick = { onPlay(playlistTracks, 0) },
                    enabled = playlistTracks.isNotEmpty()
                ) {
                    Text("Play")
                }
            }
            HorizontalDivider()

            if (playlistTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Long-press a song in Tracks or Folders to add it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(
                        items = playlistTracks,
                        key = { _, track -> track.documentUri }
                    ) { index, track ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TrackRow(
                                track = track,
                                onClick = { onPlay(playlistTracks, index) },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveTrack(selectedId!!, track.documentUri) }) {
                                Icon(
                                    imageVector = Icons.Outlined.RemoveCircleOutline,
                                    contentDescription = "Remove"
                                )
                            }
                        }
                        if (index < playlistTracks.size - 1) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        NameDialog(
            title = "New playlist",
            initial = "",
            confirmLabel = "Create",
            onConfirm = {
                onCreate(it)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    renamingPlaylist?.let { target ->
        NameDialog(
            title = "Rename playlist",
            initial = target.name,
            confirmLabel = "Rename",
            onConfirm = {
                onRename(target.id, it)
                renamingPlaylist = null
            },
            onDismiss = { renamingPlaylist = null }
        )
    }

    deletingPlaylist?.let { target ->
        ConfirmDeleteDialog(
            name = target.name,
            onConfirm = {
                onDelete(target.id)
                deletingPlaylist = null
            },
            onDismiss = { deletingPlaylist = null }
        )
    }
}
