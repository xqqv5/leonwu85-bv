package dev.aaa1115910.bv.player

import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListItemData

/** 标题行不可播放，互动分支也不能通过系统上一集/下一集擅自选择。 */
fun List<VideoListItem>.mediaPlaylistNeighbor(aid: Long, cid: Long, offset: Int): VideoListItemData? {
    if (offset != -1 && offset != 1 || any { it is VideoListInteractiveNode }) return null
    val playable = filterIsInstance<VideoListItemData>().filter { it.aid > 0 && (it.cid ?: 0) > 0 }
    val current = playable.indexOfFirst { it.aid == aid && it.cid == cid }
    return if (current < 0) null else playable.getOrNull(current + offset)
}

fun boundedMediaSeek(positionMs: Long, durationMs: Long): Long =
    positionMs.coerceAtLeast(0L).let { if (durationMs > 0L) it.coerceAtMost(durationMs) else it }
