package dev.aaa1115910.bv.mobile.component.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import dev.aaa1115910.biliapi.entity.video.VideoHighEnergy
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import org.koin.compose.koinInject

private data class LoadedHighEnergy(
    val aid: Long,
    val cid: Long,
    val data: VideoHighEnergy?,
)

@Composable
internal fun rememberHighEnergyProgressData(
    aid: Long,
    cid: Long,
    enabled: Boolean,
    onlineVideoReady: Boolean,
    repository: VideoPlayRepository = koinInject(),
): VideoHighEnergy? {
    val loaded by produceState<LoadedHighEnergy?>(
        initialValue = null,
        aid, cid, enabled, onlineVideoReady, repository,
    ) {
        value = null
        if (!enabled || !onlineVideoReady || aid <= 0L || cid <= 0L) return@produceState
        val data = try {
            repository.getVideoHighEnergy(aid, cid)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        coroutineContext.ensureActive()
        value = LoadedHighEnergy(aid, cid, data)
    }
    // Key changes cancel the producer; also hide the previous part before the new effect runs.
    return loaded?.takeIf {
        enabled && onlineVideoReady && it.aid == aid && it.cid == cid
    }?.data
}
