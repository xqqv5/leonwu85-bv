package dev.aaa1115910.bv.repository

import dev.aaa1115910.biliapi.http.entity.BiliResponse
import dev.aaa1115910.biliapi.http.entity.web.NavResponseData
import dev.aaa1115910.bv.entity.AuthData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

internal fun BiliResponse<NavResponseData>.isSessionAuthenticated(uid: Long): Boolean? =
    when (code) {
        -101 -> false
        0 -> (data ?: result)?.let { profile ->
            when {
                !profile.isLogin -> false
                profile.mid == uid -> true
                else -> null
            }
        }
        else -> null
    }

internal class AuthFailureVerifier {
    private val mutex = Mutex()

    suspend fun verify(
        authData: AuthData,
        isCurrentSession: (AuthData) -> Boolean,
        validateSession: suspend (AuthData) -> Boolean?,
        onInvalidSession: suspend (AuthData) -> Unit,
        onVerificationFailure: (Exception) -> Unit
    ) {
        // The validation endpoint can itself return -101 and trigger the global callback.
        // Ignore callbacks while this check is running instead of queuing another validation.
        if (!mutex.tryLock()) return
        try {
            if (!isCurrentSession(authData)) return
            val authenticated = try {
                validateSession(authData)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onVerificationFailure(error)
                return
            }
            if (authenticated == false && isCurrentSession(authData)) {
                onInvalidSession(authData)
            }
        } finally {
            mutex.unlock()
        }
    }
}
