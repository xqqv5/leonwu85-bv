package dev.aaa1115910.bv.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable

@Serializable
data class RepostDraft(
    val dynamicId: String,
    val text: String,
    val commentOid: Long? = null,
    val commentType: Long? = null,
    val alsoComment: Boolean = false,
    val uploadId: String,
    val ownerUid: Long = 0L
)

enum class RepostPublicationStatus { Ready, Publishing, Failed, PartialSuccess, Success }

@Serializable
data class RepostPublication(
    val draft: RepostDraft? = null,
    val repostId: String? = null,
    val commentSucceeded: Boolean = false,
    val error: String? = null,
    val publishing: Boolean = false
) {
    val status: RepostPublicationStatus get() = when {
        publishing -> RepostPublicationStatus.Publishing
        repostId != null && (draft?.alsoComment != true || commentSucceeded) -> RepostPublicationStatus.Success
        repostId != null -> RepostPublicationStatus.PartialSuccess
        error != null -> RepostPublicationStatus.Failed
        else -> RepostPublicationStatus.Ready
    }
    val locksDraft: Boolean get() = publishing || repostId != null || commentSucceeded
}

/** 先转发再评论；每个已确认成功的步骤立即保存，重试时只执行未成功的步骤。 */
class RepostPublicationCoordinator(
    restored: RepostPublication = RepostPublication(),
    private val save: (RepostPublication) -> Unit = {}
) {
    private val mutableState = MutableStateFlow(restored.copy(publishing = false))
    val state = mutableState.asStateFlow()
    private val mutex = Mutex()

    suspend fun publish(
        draft: RepostDraft,
        repost: suspend (RepostDraft) -> String,
        comment: suspend (RepostDraft) -> Unit
    ) {
        if (!mutex.tryLock()) return
        try {
            val previous = state.value
            if (previous.status == RepostPublicationStatus.Success) return
            require(!previous.locksDraft || previous.draft == draft) { "部分步骤已成功，请重试原请求" }
            require(draft.dynamicId.isNotBlank()) { "动态 ID 无效" }
            require(!draft.alsoComment || (draft.text.isNotBlank() && (draft.commentOid ?: 0) > 0 && (draft.commentType ?: 0) > 0)) {
                "评论内容或目标无效"
            }
            set(if (previous.draft == draft) previous.copy(publishing = true, error = null)
                else RepostPublication(draft = draft, publishing = true))
            if (state.value.repostId == null) {
                try {
                    val id = repost(draft)
                    require(id.isNotBlank()) { "转发结果未返回动态 ID" }
                    set(state.value.copy(repostId = id))
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    set(state.value.copy(error = "转发失败：${error.localizedMessage ?: "请重试"}"))
                    return
                }
            }
            if (draft.alsoComment && !state.value.commentSucceeded) {
                try {
                    comment(draft)
                    set(state.value.copy(commentSucceeded = true))
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    set(state.value.copy(error = "转发已成功，评论失败：${error.localizedMessage ?: "请重试评论"}"))
                }
            }
        } finally {
            set(state.value.copy(publishing = false))
            mutex.unlock()
        }
    }

    fun clearCompleted() {
        if (state.value.status == RepostPublicationStatus.Success) set(RepostPublication())
    }

    private fun set(value: RepostPublication) {
        // Persist confirmed progress before issuing the next network request.
        save(value.copy(publishing = false))
        mutableState.value = value
    }
}
