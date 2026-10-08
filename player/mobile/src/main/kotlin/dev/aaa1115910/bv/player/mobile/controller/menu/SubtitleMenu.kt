package dev.aaa1115910.bv.player.mobile.controller.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleMenu(
    onChangeSubtitle: (Long) -> Unit,
    onChangeSecondarySubtitle: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = LocalVideoPlayerConfigData.current
    var secondarySlot by rememberSaveable { mutableStateOf(false) }
    val subtitles = config.availableSubtitleTracks.filter { it.id != -1L && it.url.isNotBlank() }
    val selectedId = if (secondarySlot) config.currentSecondarySubtitleId else config.currentSubtitleId
    val enabled = !secondarySlot || config.currentSubtitleId != -1L
    val onSelect = if (secondarySlot) onChangeSecondarySubtitle else onChangeSubtitle

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("字幕") },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭字幕设置")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !secondarySlot, onClick = { secondarySlot = false }, label = { Text("主字幕") })
                    FilterChip(selected = secondarySlot, onClick = { secondarySlot = true }, label = { Text("副字幕") })
                }
                val primaryName = subtitles.find { it.id == config.currentSubtitleId }?.langDoc ?: "关闭"
                val secondaryName = subtitles.find { it.id == config.currentSecondarySubtitleId }?.langDoc ?: "关闭"
                Text("主字幕：$primaryName\n副字幕：$secondaryName", modifier = Modifier.padding(vertical = 8.dp))
            }
            if (subtitles.isEmpty()) {
                item { Text("当前视频暂无可用字幕", modifier = Modifier.padding(vertical = 12.dp)) }
            } else {
                if (!enabled) {
                    item { Text("请先开启主字幕，再选择副字幕", modifier = Modifier.padding(vertical = 8.dp)) }
                }
                item {
                    SubtitleOption("关闭", selectedId == -1L, enabled) { onSelect(-1L) }
                }
                items(
                    items = subtitles.filter { !secondarySlot || it.id != config.currentSubtitleId },
                    key = { it.id },
                ) { subtitle ->
                    SubtitleOption(subtitle.langDoc, selectedId == subtitle.id, enabled) { onSelect(subtitle.id) }
                }
            }
        }
    }
}

@Composable
private fun SubtitleOption(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, enabled = enabled, onClick = null)
        Text(text = text, modifier = Modifier.padding(start = 8.dp))
    }
}
