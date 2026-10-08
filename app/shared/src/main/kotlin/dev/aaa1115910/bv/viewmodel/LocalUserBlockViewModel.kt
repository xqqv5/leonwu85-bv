package dev.aaa1115910.bv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.bv.repository.LocalUserBlockRepository
import dev.aaa1115910.bv.repository.parseBlockedUserIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LocalUserBlockViewModel(private val repository: LocalUserBlockRepository) : ViewModel() {
    val blocked = repository.blocked
    var input by mutableStateOf("")
    var message by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    fun add() = update {
        val ids = parseBlockedUserIds(input)
        require(ids.size == 1) { "添加时请输入一个 UID，多个 UID 请使用合并导入" }
        repository.add(ids.single()).getOrThrow()
        "已本地屏蔽"
    }

    fun import() = update { "已导入 ${repository.import(input).getOrThrow()} 个新 UID" }
    fun remove(mid: Long) = update(clearInput = false) {
        repository.remove(mid).getOrThrow()
        "已解除屏蔽，刷新内容后生效"
    }

    fun export(): String = repository.export()

    private fun update(clearInput: Boolean = true, action: () -> String) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            try {
                message = withContext(Dispatchers.IO) { action() }
                if (clearInput) input = ""
            } catch (error: Exception) {
                message = error.localizedMessage ?: "保存失败，请重试"
            } finally {
                saving = false
            }
        }
    }
}
