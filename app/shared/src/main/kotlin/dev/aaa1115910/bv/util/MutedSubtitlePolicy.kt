package dev.aaa1115910.bv.util

import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import java.util.Locale

internal class MutedSubtitlePolicy {
    private var active = false
    private var manualSelectionMade = false
    private var automaticSelectionMade = false

    /** Returns true only when subtitles opened automatically should be closed. */
    fun setActive(active: Boolean): Boolean {
        this.active = active
        return (!active && automaticSelectionMade).also { closeAutomaticSubtitle ->
            if (closeAutomaticSubtitle) automaticSelectionMade = false
        }
    }

    fun selectAutomaticSubtitle(currentSubtitleId: Long, available: List<Subtitle>): Long? {
        if (!active || manualSelectionMade || automaticSelectionMade || currentSubtitleId != -1L) {
            return null
        }
        return available.preferredSubtitleForMutedPlayback()?.id?.also {
            automaticSelectionMade = true
        }
    }

    fun onManualSelection() {
        manualSelectionMade = true
        automaticSelectionMade = false
    }

    fun resetAutomaticSelection() {
        automaticSelectionMade = false
    }

    fun resetForVideo() {
        manualSelectionMade = false
        automaticSelectionMade = false
    }
}

internal fun List<Subtitle>.preferredSubtitleForMutedPlayback(): Subtitle? {
    val subtitles = filter { it.id != -1L && it.url.isNotBlank() }
    return subtitles.firstOrNull { it.isChinese() && !it.isAi() }
        ?: subtitles.firstOrNull { it.isChinese() }
        ?: subtitles.firstOrNull { !it.isAi() }
        ?: subtitles.firstOrNull()
}

private fun Subtitle.isAi(): Boolean =
    type == SubtitleType.AI || lang.startsWith("ai-", ignoreCase = true)

private fun Subtitle.isChinese(): Boolean {
    val code = lang.lowercase(Locale.ROOT).removePrefix("ai-")
    return code == "zh" || code.startsWith("zh-") || code.startsWith("zh_") ||
        lang.contains("中文") || langDoc.contains("中文")
}
