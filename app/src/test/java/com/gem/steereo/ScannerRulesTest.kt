package com.gem.steereo

import com.gem.steereo.data.db.TrackEntity
import com.gem.steereo.ui.MainViewModel.FolderItem
import com.gem.steereo.util.PureRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerRulesTest {

    // ---- Audio file filter -------------------------------------------------

    @Test
    fun knownExtensions_accepted_withoutMime() {
        for (ext in listOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav")) {
            assertTrue("song.$ext", PureRules.isAudioFile("song.$ext", null))
        }
    }

    @Test
    fun uppercaseExtension_accepted() {
        assertTrue(PureRules.isAudioFile("SONG.MP3", null))
        assertTrue(PureRules.isAudioFile("Track.FlAc", null))
    }

    @Test
    fun noExtension_rejected_withoutAudioMime() {
        assertFalse(PureRules.isAudioFile("song", null))
        assertFalse(PureRules.isAudioFile("song", "application/octet-stream"))
    }

    @Test
    fun audioMime_withOddExtension_accepted() {
        assertTrue(PureRules.isAudioFile("track.bin", "audio/mpeg"))
        assertTrue(PureRules.isAudioFile("track", "audio/mpeg"))
    }

    @Test
    fun nonAudio_rejected() {
        assertFalse(PureRules.isAudioFile("cover.jpg", "image/jpeg"))
        assertFalse(PureRules.isAudioFile("notes.txt", "text/plain"))
        assertFalse(PureRules.isAudioFile("song.mp3.txt", "text/plain"))
    }

    @Test
    fun knownExtension_acceptedEvenWithWrongMime() {
        assertTrue(PureRules.isAudioFile("song.mp3", "application/octet-stream"))
    }

    // ---- relativeDir building ---------------------------------------------

    @Test
    fun childOfRoot_isJustName() {
        assertEquals("Music", PureRules.childRelativeDir("", "Music"))
    }

    @Test
    fun nestedChild_joinedWithSlash() {
        assertEquals("Music/Rock", PureRules.childRelativeDir("Music", "Rock"))
        assertEquals("Music/Rock/Live", PureRules.childRelativeDir("Music/Rock", "Live"))
    }

    // ---- Folder label -------------------------------------------------------

    @Test
    fun rootLabel() {
        assertEquals("/ (root)", PureRules.folderLabel(""))
    }

    @Test
    fun singleLevelLabel_unchanged() {
        assertEquals("Music", PureRules.folderLabel("Music"))
    }

    @Test
    fun nestedLabel_usesChevrons() {
        assertEquals("Music › Rock › Live", PureRules.folderLabel("Music/Rock/Live"))
    }

    // ---- Folders-tab grouping ----------------------------------------------

    private fun track(uri: String, dir: String) = TrackEntity(
        documentUri = uri,
        title = uri,
        artist = null,
        album = null,
        durationMs = 0L,
        relativeDir = dir,
        fileName = "$uri.mp3",
        lastModified = 0L
    )

    @Test
    fun grouping_emptyLibrary_noFolders() {
        assertEquals(emptyList<FolderItem>(), PureRules.groupIntoFolders(emptyList()))
    }

    @Test
    fun grouping_countsAndSortsCaseInsensitive() {
        val tracks = listOf(
            track("a", "Rock"),
            track("b", ""),
            track("c", "jazz"),
            track("d", "Rock"),
            track("e", "Rock/Live")
        )
        assertEquals(
            listOf(
                FolderItem("", 1),
                FolderItem("jazz", 1),
                FolderItem("Rock", 2),
                FolderItem("Rock/Live", 1)
            ),
            PureRules.groupIntoFolders(tracks)
        )
    }

    @Test
    fun grouping_nestedDirIsSeparateFromParent() {
        val folders = PureRules.groupIntoFolders(
            listOf(track("a", "Music"), track("b", "Music/Sub"))
        )
        assertEquals(2, folders.size)
        assertEquals(1, folders.first { it.dir == "Music" }.trackCount)
    }

    // ---- IN-list chunking (size = 500) --------------------------------------

    private fun sizesFor(n: Int) = PureRules.chunkForInClause(List(n) { it }).map { it.size }

    @Test
    fun chunkSize_underSqliteLegacyLimit() {
        assertTrue(PureRules.SQL_IN_CHUNK_SIZE in 1..999)
    }

    @Test
    fun chunk_zero() {
        assertEquals(emptyList<Int>(), sizesFor(0))
    }

    @Test
    fun chunk_900() {
        assertEquals(listOf(500, 400), sizesFor(900))
    }

    @Test
    fun chunk_901() {
        assertEquals(listOf(500, 401), sizesFor(901))
    }

    @Test
    fun chunk_2000() {
        assertEquals(listOf(500, 500, 500, 500), sizesFor(2000))
    }

    @Test
    fun chunk_preservesOrderAndItems() {
        val input = List(1234) { "uri$it" }
        assertEquals(input, PureRules.chunkForInClause(input).flatten())
    }
}
