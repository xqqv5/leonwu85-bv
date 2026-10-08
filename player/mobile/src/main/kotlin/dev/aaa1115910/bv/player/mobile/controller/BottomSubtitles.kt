package dev.aaa1115910.bv.player.mobile.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekData

@Composable
internal fun BottomSubtitles(
    isFullScreen: Boolean,
    isInPictureInPicture: Boolean,
    controlsVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val config = LocalVideoPlayerConfigData.current
    val position = LocalVideoPlayerSeekData.current.position
    if (config.isLive || config.currentSubtitleId == -1L) return

    val primary = config.currentSubtitleData.firstOrNull { it.isShowing(position) && it.content.isNotBlank() }
    val secondary = config.currentSecondarySubtitleData
        .takeIf { config.currentSecondarySubtitleId != -1L && config.currentSecondarySubtitleId != config.currentSubtitleId }
        ?.firstOrNull { it.isShowing(position) && it.content.isNotBlank() }
    val scale = when {
        isInPictureInPicture -> 0.5f
        isFullScreen -> 1f
        else -> 0.7f
    }
    val controlsPadding = when {
        !controlsVisible -> 0.dp
        isFullScreen -> 104.dp
        else -> 48.dp
    }
    val subtitlePadding = if (secondary != null) {
        config.currentSecondarySubtitleBottomPadding
    } else {
        config.currentSubtitleBottomPadding
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = subtitlePadding * scale + controlsPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp * scale),
        ) {
            primary?.let {
                SubtitleBubble(it, config.currentSubtitleFontSize * scale, config.currentSubtitleBackgroundOpacity)
            }
            secondary?.let {
                SubtitleBubble(it, config.currentSecondarySubtitleFontSize * scale, config.currentSecondarySubtitleBackgroundOpacity)
            }
        }
    }
}

@Composable
private fun SubtitleBubble(item: SubtitleItem, fontSize: TextUnit, backgroundOpacity: Float) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = backgroundOpacity.coerceIn(0f, 1f)))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            text = item.content,
            color = Color.White,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 1f), 3f)),
        )
        if (item.isAI) {
            Text(
                modifier = Modifier.padding(start = 3.dp),
                text = "AI",
                color = Color.White,
                fontSize = (fontSize.value / 2).coerceAtLeast(8f).sp,
            )
        }
    }
}
