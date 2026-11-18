package com.gem.steereo.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.gem.steereo.data.db.TrackEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun volumeCurve(slider: Float): Float = (slider.coerceIn(0f, 1f)).let { it * it }

data class NowPlaying(
    val title: String,
    val artist: String?,
    val durationMs: Long,
    val mediaId: String,
    val album: String?
)

data class PlaybackIssue(
    val title: String?,
    val errorCode: Int
)

class PlayerConnection(context: Context) {
    private val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    private val controllerFuture = MediaController.Builder(context, token).buildAsync()
    private var controller: MediaController? = null

    private var pendingPlay: Pair<List<MediaItem>, Int>? = null
    private var pendingVolume: Float? = null

    private var consecutiveErrors = 0

    private val _issues = MutableSharedFlow<PlaybackIssue>(extraBufferCapacity = 4)
    val issues: SharedFlow<PlaybackIssue> = _issues.asSharedFlow()

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                consecutiveErrors = 0
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            consecutiveErrors++
            val title = controller?.currentMediaItem?.mediaMetadata?.title?.toString()
            _issues.tryEmit(PlaybackIssue(title, error.errorCode))
            val ctrl = controller ?: return
            if (ctrl.hasNextMediaItem() && consecutiveErrors < ctrl.mediaItemCount) {
                ctrl.seekToNextMediaItem()
                ctrl.prepare()
                ctrl.play()
            }
        }
    }

    init {
        controllerFuture.addListener(
            {
                try {
                    val ctrl = controllerFuture.get()
                    controller = ctrl
                    ctrl.addListener(playerListener)
                    updateState()
                    pendingVolume?.let {
                        ctrl.volume = volumeCurve(it)
                        pendingVolume = null
                    }
                    pendingPlay?.let { (items, startIndex) ->
                        ctrl.setMediaItems(items, startIndex, 0L)
                        ctrl.prepare()
                        ctrl.play()
                        pendingPlay = null
                    }
                } catch (_: Exception) {}
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun updateState() {
        val ctrl = controller ?: return
        val item = ctrl.currentMediaItem
        val metadata = item?.mediaMetadata
        if (metadata?.title != null) {
            val dur = maxOf(0L, ctrl.duration)
            _nowPlaying.value = NowPlaying(
                title = metadata.title.toString(),
                artist = metadata.artist?.toString(),
                durationMs = dur,
                mediaId = item.mediaId,
                album = metadata.albumTitle?.toString()
            )
        } else {
            _nowPlaying.value = null
        }
        _isPlaying.value = ctrl.isPlaying
        _hasPrevious.value = ctrl.hasPreviousMediaItem()
        _hasNext.value = ctrl.hasNextMediaItem()
        _positionMs.value = maxOf(0L, ctrl.currentPosition)
        _repeatMode.value = ctrl.repeatMode
        _shuffleEnabled.value = ctrl.shuffleModeEnabled
    }

    fun playList(tracks: List<TrackEntity>, startIndex: Int) {
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.documentUri)
                .setUri(track.documentUri)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(track.documentUri.toUri())
                        .build()
                )
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
        }
        val ctrl = controller
        if (ctrl != null) {
            ctrl.setMediaItems(items, startIndex, 0L)
            ctrl.prepare()
            ctrl.play()
        } else {
            pendingPlay = Pair(items, startIndex)
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
        } else {
            ctrl.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun refreshPosition() {
        _positionMs.value = controller?.currentPosition ?: 0L
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms.coerceAtLeast(0L))
        refreshPosition()
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun setVolume(slider: Float) {
        val ctrl = controller
        if (ctrl != null) {
            ctrl.volume = volumeCurve(slider)
        } else {
            pendingVolume = slider
        }
    }

    fun release() {
        controller?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
    }

    fun stopAndClear() {
        controller?.stop()
        controller?.clearMediaItems()
    }
}
