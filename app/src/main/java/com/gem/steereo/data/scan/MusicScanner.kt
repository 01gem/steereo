package com.gem.steereo.data.scan

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.gem.steereo.data.db.TrackEntity
import com.gem.steereo.util.PureRules
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanResult(
    val changed: List<TrackEntity>,
    val seenUris: Set<String>
)

class MusicScanner(private val context: Context) {

    private companion object {
        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
    }

    suspend fun scan(
        treeUri: Uri,
        known: Map<String, Long>,
        onProgress: (Int) -> Unit
    ): ScanResult = withContext(Dispatchers.IO) {
        val changed = mutableListOf<TrackEntity>()
        val seenUris = mutableSetOf<String>()
        var audioCount = 0

        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            ?: return@withContext ScanResult(emptyList(), emptySet())

        fun scanDir(parentDocId: String, relativeDir: String) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            val cursor = context.contentResolver.query(
                childrenUri,
                PROJECTION,
                null,
                null,
                null
            ) ?: return

            cursor.use { c ->
                val idIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modifiedIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (c.moveToNext()) {
                    val docId = c.getString(idIdx) ?: continue
                    val name = c.getString(nameIdx) ?: ""
                    val mime = c.getString(mimeIdx)
                    val lastModified = if (modifiedIdx >= 0 && !c.isNull(modifiedIdx)) c.getLong(modifiedIdx) else 0L

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (name.startsWith(".")) continue
                        val subDir = PureRules.childRelativeDir(relativeDir, name)
                        scanDir(docId, subDir)
                    } else {
                        val isAudio = PureRules.isAudioFile(name, mime)
                        if (!isAudio) continue

                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                        seenUris.add(fileUri)
                        audioCount++

                        if (audioCount % 25 == 0) {
                            onProgress(audioCount)
                        }

                        if (known[fileUri] == lastModified) {
                            continue
                        }

                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, Uri.parse(fileUri))
                            val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            val title = if (!rawTitle.isNullOrBlank()) rawTitle else name.substringBeforeLast('.')
                            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            val durationMs = durationStr?.toLongOrNull() ?: 0L

                            changed.add(
                                TrackEntity(
                                    documentUri = fileUri,
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    durationMs = durationMs,
                                    relativeDir = relativeDir,
                                    fileName = name,
                                    lastModified = lastModified
                                )
                            )
                        } catch (e: Exception) {
                            Log.w("MusicScanner", "Failed metadata for $fileUri", e)
                        } finally {
                            try { retriever.release() } catch (_: Exception) {}
                        }
                    }
                }
            }
        }

        scanDir(rootDocId, "")
        onProgress(audioCount)

        ScanResult(changed, seenUris)
    }
}
