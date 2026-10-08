package dev.aaa1115910.bv.viewmodel.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.bv.repository.LocalUserBlockRepository
import dev.aaa1115910.bv.repository.observeList
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.repositories.RecommendVideoRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.fError
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.swapListWithMainContext
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class RankViewModel(
    private val recommendVideoRepository: RecommendVideoRepository,
    private val localUserBlocks: LocalUserBlockRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger {}

    val allRankVideoList = mutableStateListOf<UgcItem>()

    init { localUserBlocks.observeList(viewModelScope, allRankVideoList) { it.authorId } }
    var loading by mutableStateOf(false)
    var loaded by mutableStateOf(false)

    suspend fun loadAllRank(refresh: Boolean = false) {
        if (loading || (loaded && !refresh)) return
        loading = true
        logger.fInfo { "Load all rank videos" }
        runCatching {
            val videos = recommendVideoRepository.getRankVideos(rid = 0)
            withContext(Dispatchers.Main.immediate) {
                allRankVideoList.clear()
                allRankVideoList.addAll(videos.filterNot { localUserBlocks.isBlocked(it.authorId) })
            }
            loaded = true
        }.onFailure {
            logger.fError { "Load all rank video list failed: ${it.stackTraceToString()}" }
            withContext(Dispatchers.Main) {
                "加载全站排行榜失败: ${it.localizedMessage}".toast(BVApp.context)
            }
        }
        loading = false
    }
}
