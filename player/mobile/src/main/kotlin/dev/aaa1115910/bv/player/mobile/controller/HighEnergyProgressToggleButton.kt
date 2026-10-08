package dev.aaa1115910.bv.player.mobile.controller

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.aaa1115910.bv.player.mobile.LocalVideoHighEnergyState

@Composable
internal fun HighEnergyProgressToggleButton(modifier: Modifier = Modifier) {
    val state = LocalVideoHighEnergyState.current
    if (state.data == null) return

    IconToggleButton(
        modifier = modifier,
        checked = state.visible,
        onCheckedChange = state.onVisibilityChange,
        colors = IconButtonDefaults.iconToggleButtonColors(
            contentColor = Color.White.copy(alpha = 0.45f),
            checkedContentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ShowChart,
            contentDescription = if (state.visible) "隐藏高能进度条" else "显示高能进度条",
        )
    }
}
