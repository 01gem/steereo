package com.gem.steereo.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.gem.steereo.data.db.TrackEntity
import java.util.Locale

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: TrackEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isCurrent: Boolean = false,
    onMoreClick: (() -> Unit)? = null
) {
    ListItem(
        leadingContent = if (isCurrent) {
            {
                Icon(
                    Icons.Filled.GraphicEq,
                    contentDescription = "Now playing",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        headlineContent = {
            Text(
                text = track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified,
                fontWeight = if (isCurrent) FontWeight.SemiBold else null
            )
        },
        supportingContent = {
            Text(
                text = track.artist ?: "Unknown artist",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = formatDuration(track.durationMs))
                if (onMoreClick != null) {
                    IconButton(onClick = onMoreClick) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                }
            }
        },
        modifier = if (onLongClick != null) {
            modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        } else {
            modifier.clickable(onClick = onClick)
        }
    )
}
