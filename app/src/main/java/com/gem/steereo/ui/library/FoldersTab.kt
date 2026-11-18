package com.gem.steereo.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gem.steereo.data.db.TrackEntity
import com.gem.steereo.ui.MainViewModel.FolderItem
import kotlinx.coroutines.flow.Flow

@Composable
fun FoldersTab(
    folders: List<FolderItem>,
    folderLabel: (String) -> String,
    tracksInFolder: (String) -> Flow<List<TrackEntity>>,
    onTrackClick: (List<TrackEntity>, Int) -> Unit,
    onTrackLongClick: (TrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDir by rememberSaveable { mutableStateOf<String?>(null) }

    val currentDir = selectedDir
    if (currentDir == null) {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(folders) { folder ->
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(text = folderLabel(folder.dir))
                    },
                    supportingContent = {
                        Text(text = "${folder.trackCount} tracks")
                    },
                    modifier = Modifier.clickable { selectedDir = folder.dir }
                )
            }
        }
    } else {
        BackHandler(enabled = true) {
            selectedDir = null
        }
        val folderFlow = remember(currentDir) { tracksInFolder(currentDir) }
        val folderTracks by folderFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                IconButton(onClick = { selectedDir = null }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = folderLabel(currentDir),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(
                    items = folderTracks,
                    key = { _, track -> track.documentUri }
                ) { index, track ->
                    TrackRow(
                        track = track,
                        onClick = { onTrackClick(folderTracks, index) },
                        onLongClick = { onTrackLongClick(track) }
                    )
                    if (index < folderTracks.size - 1) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
