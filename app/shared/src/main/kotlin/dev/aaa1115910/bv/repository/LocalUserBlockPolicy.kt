package dev.aaa1115910.bv.repository

/** 全量校验后再写入，避免无效导入覆盖已有屏蔽名单。 */
fun parseBlockedUserIds(text: String): Set<Long> {
    val content = text.trim().removeSurrounding("[", "]")
    require(content.isNotBlank()) { "请输入 UID，多个 UID 用空格、逗号或换行分隔" }
    val tokens = content.split(Regex("[\\s,;，；]+"))
    require(tokens.size <= 10_000) { "单次最多导入 10000 个 UID" }
    return tokens.map { token ->
        token.trim('"').toLongOrNull()?.takeIf { it > 0L }
            ?: error("无效 UID：$token")
    }.toSet()
}
