package dev.aaa1115910.bv.viewmodel

import dev.aaa1115910.biliapi.entity.DashAudio
import dev.aaa1115910.biliapi.entity.DashVideo
import dev.aaa1115910.bv.player.entity.PlaybackMediaMode

internal data class VodPlaybackSourceSelection(
    val mediaMode: PlaybackMediaMode,
    val videoUrls: List<String>,
    val audioUrls: List<String>,
)

/** Select a usable mode before sending the optional video/audio inputs to the player. */
internal fun selectVodPlaybackSource(
    video: DashVideo?,
    audio: DashAudio?,
    requestedMode: PlaybackMediaMode,
): VodPlaybackSourceSelection? {
    val videoUrl = video?.baseUrl?.takeIf { it.isNotBlank() }
    val audioUrl = audio?.baseUrl?.takeIf { it.isNotBlank() }
    val hasEmbeddedAudio = videoUrl != null && video.isMuxed
    if (videoUrl == null && audioUrl == null) return null

    val mode = when {
        videoUrl == null -> PlaybackMediaMode.AudioOnly
        audioUrl == null && !hasEmbeddedAudio -> PlaybackMediaMode.Normal
        else -> requestedMode
    }
    val videoUrls = if (mode == PlaybackMediaMode.Normal && videoUrl != null) {
        listOf(videoUrl) + video.backUrl
    } else {
        emptyList()
    }
    val audioUrls = when {
        audioUrl != null -> listOf(audioUrl) + audio.backUrl
        mode == PlaybackMediaMode.AudioOnly && hasEmbeddedAudio -> listOf(videoUrl) + video.backUrl
        else -> emptyList()
    }
    return VodPlaybackSourceSelection(mode, videoUrls, audioUrls)
}
