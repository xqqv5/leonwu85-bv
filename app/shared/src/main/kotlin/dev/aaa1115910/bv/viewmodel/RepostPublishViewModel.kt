package dev.aaa1115910.bv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.repositories.CommentRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.repository.RepostDraft
import dev.aaa1115910.bv.repository.RepostPublication
import dev.aaa1115910.bv.repository.RepostPublicationCoordinator
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.KoinViewModel
import kotlin.random.Random

@KoinViewModel
class RepostPublishViewModel(
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val restored = runCatching {
        Json.decodeFromString<RepostPublication>(savedState.get<String>("publication") ?: "{}")
    }.getOrDefault(RepostPublication())
    private val coordinator = RepostPublicationCoordinator(restored) { savedState["publication"] = Json.encodeToString(it) }
    val state = coordinator.state
    var text by mutableStateOf(restored.draft?.text ?: savedState.get<String>("text").orEmpty())
        private set
    var alsoComment by mutableStateOf(restored.draft?.alsoComment ?: savedState.get<Boolean>("alsoComment") ?: false)
        private set
    private var uploadId = restored.draft?.uploadId ?: "${Prefs.uid}_${System.currentTimeMillis() / 1000}_${Random.nextInt(1000, 10000)}"

    var validationError by mutableStateOf<String?>(null)
        private set

    fun prepare(allowComment: Boolean) {
        if (!savedState.contains("alsoComment") && state.value.draft == null) updateAlsoComment(allowComment)
    }

    fun updateText(value: String) {
        if (state.value.locksDraft) return
        text = value
        savedState["text"] = value
    }

    fun updateAlsoComment(value: Boolean) {
        if (state.value.locksDraft) return
        alsoComment = value
        savedState["alsoComment"] = value
    }

    fun acknowledgeSuccess() {
        coordinator.clearCompleted()
        text = ""
        alsoComment = false
        savedState["text"] = ""
        savedState["alsoComment"] = false
        uploadId = "${Prefs.uid}_${System.currentTimeMillis() / 1000}_${Random.nextInt(1000, 10000)}"
    }

    fun publish(dynamicId: String, commentOid: Long?, commentType: Long?) {
        if (state.value.publishing) return
        val draft = state.value.draft?.takeIf { state.value.locksDraft }
            ?: RepostDraft(dynamicId, text.trim(), commentOid, commentType, alsoComment, uploadId, Prefs.uid)
        if (!Prefs.isLogin || draft.ownerUid != Prefs.uid) {
            validationError = "请使用原账号登录后重试"
            return
        }
        validationError = null
        viewModelScope.launch {
            coordinator.publish(draft,
                repost = { request -> withContext(Dispatchers.IO) {
                    userRepository.repostDynamic(request.dynamicId, request.text, request.uploadId)
                } },
                comment = { request -> withContext(Dispatchers.IO) {
                    commentRepository.addComment(checkNotNull(request.commentType), checkNotNull(request.commentOid), request.text)
                } }
            )
        }
    }
}
