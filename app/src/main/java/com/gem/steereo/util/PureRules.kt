package com.gem.steereo.util

import com.gem.steereo.data.db.TrackEntity
import com.gem.steereo.ui.MainViewModel.FolderItem

/**
 * Android-free rules pulled out of scanner / view model / repository so they can be
 * unit-tested on the JVM. Logic is identical to the original inline code.
 */
object PureRules {

    val AUDIO_EXTENSIONS: Set<String> = setOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav")

    /** SQLite bind-variable batch size for `IN (:list)` queries. */
    const val SQL_IN_CHUNK_SIZE = 500

    /** Non-directory entry is audio if MIME starts with `audio/` OR extension (case-insensitive) is known. */
    fun isAudioFile(name: String, mime: String?): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return (mime != null && mime.startsWith("audio/")) || ext in AUDIO_EXTENSIONS
    }

    /** Relative dir of a child folder; root is "". */
    fun childRelativeDir(parent: String, name: String): String =
        if (parent.isEmpty()) name else "$parent/$name"

    /** Display label for a relativeDir in the Folders tab. */
    fun folderLabel(dir: String): String =
        if (dir.isEmpty()) "/ (root)" else dir.replace("/", " › ")

    /** Folders-tab grouping: one item per distinct relativeDir, sorted case-insensitively. */
    fun groupIntoFolders(tracks: List<TrackEntity>): List<FolderItem> =
        tracks.groupBy { it.relativeDir }
            .map { (dir, items) -> FolderItem(dir, items.size) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.dir })

    fun <T> chunkForInClause(items: List<T>): List<List<T>> = items.chunked(SQL_IN_CHUNK_SIZE)
}
