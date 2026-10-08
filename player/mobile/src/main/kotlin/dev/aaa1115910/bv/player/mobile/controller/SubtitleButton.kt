package dev.aaa1115910.bv.player.mobile.controller

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData

@Composable
internal fun SubtitleButton(onClick: () -> Unit) {
    val config = LocalVideoPlayerConfigData.current
    if (config.isLive) return

    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.ClosedCaption,
            contentDescription = "字幕设置",
            tint = if (config.currentSubtitleId != -1L) MaterialTheme.colorScheme.primary else Color.White
        )
    }
}
