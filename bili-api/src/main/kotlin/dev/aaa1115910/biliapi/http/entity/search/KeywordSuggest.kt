package dev.aaa1115910.biliapi.http.entity.search

import dev.aaa1115910.biliapi.http.entity.BiliResponseWithoutData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/** Web 搜索联想的 WBI 响应；无联想时 result 可以是 null 或空数组。 */
@Serializable
data class KeywordSuggest(
    val code: Int,
    val message: String = "",
    val data: Data? = null
) {
    val suggests: List<Result.Tag>
        get() {
            BiliResponseWithoutData(code, message).requireSuccess()
            val tags = (data?.result as? JsonObject)?.get("tag") as? JsonArray
                ?: return emptyList()
            return tags.map { parser.decodeFromJsonElement<Result.Tag>(it) }
        }

    @Serializable
    data class Data(val result: JsonElement? = null)

    @Serializable
    data class Result(val tag: List<Tag> = emptyList()) {
        @Serializable
        data class Tag(
            val value: String,
            val term: String = "",
            val ref: Int = 0,
            val name: String = "",
            val spid: Int = 0
        )
    }

    companion object {
        private val parser = Json { ignoreUnknownKeys = true }
    }
}
