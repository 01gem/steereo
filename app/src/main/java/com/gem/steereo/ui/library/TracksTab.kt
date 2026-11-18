package com.gem.steereo.ui.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gem.steereo.data.db.TrackEntity

@Composable
fun TracksTab(
    tracks: List<TrackEntity>,
    onTrackClick: (List<TrackEntity>, Int) -> Unit,
    onTrackLongClick: (TrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(
            items = tracks,
            key = { _, track -> track.documentUri }
        ) { index, track ->
            TrackRow(
                track = track,
                onClick = { onTrackClick(tracks, index) },
                onLongClick = { onTrackLongClick(track) }
            )
            if (index < tracks.size - 1) {
                HorizontalDivider()
            }
        }
    }
}
