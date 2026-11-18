package com.gem.steereo.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [Index("relativeDir")]
)
data class TrackEntity(
    @PrimaryKey val documentUri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val relativeDir: String = "",
    val fileName: String,
    val lastModified: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "documentUri"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["documentUri"],
            childColumns = ["documentUri"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentUri")]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val documentUri: String,
    val position: Int
)
