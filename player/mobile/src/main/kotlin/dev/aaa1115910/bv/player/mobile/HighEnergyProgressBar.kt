package dev.aaa1115910.bv.player.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.video.VideoHighEnergy

data class VideoHighEnergyState(
    val data: VideoHighEnergy? = null,
    val visible: Boolean = true,
    val onVisibilityChange: (Boolean) -> Unit = {},
) {
    val visibleData: VideoHighEnergy? get() = data.takeIf { visible }
}

val LocalVideoHighEnergyState = compositionLocalOf { VideoHighEnergyState() }

internal data class HighEnergyPoint(val progress: Float, val intensity: Float)

internal fun highEnergyPoints(data: VideoHighEnergy, durationMs: Long): List<HighEnergyPoint> {
    if (durationMs <= 0L || data.stepSeconds <= 0 || data.values.size < 2) return emptyList()
    val values = data.values.map { if (it.isFinite()) it.coerceAtLeast(0f) else 0f }
    val peak = values.maxOrNull()?.takeIf { it > 0f } ?: return emptyList()
    val stepMs = data.stepSeconds * 1000L
    val points = ArrayList<HighEnergyPoint>()
    values.forEachIndexed { index, value ->
        val timeMs = index * stepMs
        if (timeMs > durationMs) {
            val previous = values[index - 1]
            val fraction = (durationMs - (index - 1) * stepMs).toFloat() / stepMs
            points.add(HighEnergyPoint(1f, (previous + (value - previous) * fraction) / peak))
            return points
        }
        points.add(HighEnergyPoint(timeMs.toFloat() / durationMs, value / peak))
        if (timeMs == durationMs) return points
    }
    return points
}

@Composable
fun HighEnergyProgressBar(
    modifier: Modifier = Modifier,
    data: VideoHighEnergy,
    durationMs: Long,
    color: Color,
) {
    val points = remember(data, durationMs) { highEnergyPoints(data, durationMs) }
    if (points.size < 2) return
    val curveModifier = remember(points, color) {
        Modifier.drawWithCache {
            val strokeWidth = 1.dp.toPx()
            val top = strokeWidth / 2f
            val baseline = size.height - top
            fun x(point: HighEnergyPoint) = point.progress * size.width
            fun y(point: HighEnergyPoint) = baseline - point.intensity * (baseline - top)
            val curve = Path().apply {
                moveTo(x(points.first()), y(points.first()))
                // Horizontal control points smooth the curve without overshooting its peaks.
                points.zipWithNext().forEach { (previous, next) ->
                    val middleX = (x(previous) + x(next)) / 2f
                    cubicTo(middleX, y(previous), middleX, y(next), x(next), y(next))
                }
            }
            val area = Path().apply {
                addPath(curve)
                lineTo(x(points.last()), baseline)
                lineTo(x(points.first()), baseline)
                close()
            }
            onDrawBehind {
                drawPath(area, color.copy(alpha = 0.4f))
                drawPath(curve, color, style = Stroke(strokeWidth))
            }
        }
    }
    Box(modifier = modifier.fillMaxWidth().height(12.dp).then(curveModifier))
}
