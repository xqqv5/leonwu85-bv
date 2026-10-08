package dev.aaa1115910.bv.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.BiliPassportHttpApi
import dev.aaa1115910.biliapi.repositories.AuthRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.dao.AppDatabase
import dev.aaa1115910.bv.entity.AuthData
import dev.aaa1115910.bv.entity.db.UserDB
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.util.Date

data class ValidatedUserIdentity(
    val uid: Long,
    val username: String,
    val avatar: String
)

@Single
class UserRepository(
    private val authRepository: AuthRepository,
    private val db: AppDatabase = BVApp.getAppDatabase()
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val initialAuthData = AuthData.fromPrefs()
    var isLogin by mutableStateOf(Prefs.isLogin)
    var uid by mutableLongStateOf(initialAuthData.uid)
    var uidCkMd5 by mutableStateOf(initialAuthData.uidCkMd5)
    var sid by mutableStateOf(initialAuthData.sid)
    var sessData by mutableStateOf(initialAuthData.sessData)
    var biliJct by mutableStateOf(initialAuthData.biliJct)
    var expiredDate by mutableStateOf(Date(initialAuthData.tokenExpiredData))

    var accessToken by mutableStateOf(initialAuthData.accessToken)
    var refreshToken by mutableStateOf(initialAuthData.refreshToken)

    var username by mutableStateOf("")
    var avatar by mutableStateOf("")

    private val authMutationMutex = Mutex()
    private val authFailureVerifier = AuthFailureVerifier()

    private fun reloadFromPrefs() {
        logger.info { "Reload auth data from prefs" }

        val authData = AuthData.fromPrefs()
        uid = authData.uid
        uidCkMd5 = authData.uidCkMd5
        sid = authData.sid
        sessData = authData.sessData
        biliJct = authData.biliJct
        isLogin = Prefs.isLogin
        expiredDate = Date(authData.tokenExpiredData)
        accessToken = authData.accessToken
        refreshToken = authData.refreshToken
    }

    private fun saveToPrefs(authData: AuthData, isLogin: Boolean = true) {
        logger.info { "Save auth data to prefs" }

        Prefs.saveAuthData(authData, isLogin)
        reloadFromPrefs()
        updateAuthRepository()
    }

    suspend fun logout() {
        logoutCurrentUser()
    }

    private fun isCurrentSession(authData: AuthData): Boolean =
        isLogin && uid == authData.uid && sessData == authData.sessData

    private suspend fun logoutCurrentUser(expectedAuth: AuthData? = null): Boolean =
        authMutationMutex.withLock {
            if (expectedAuth != null && !isCurrentSession(expectedAuth)) return@withLock false
            val user = db.userDao().findUserByUid(uid)
            user?.let {
                db.userDao().delete(it)
                logger.info { "Delete user $uid in user db" }
            } ?: let {
                logger.info { "Not found user $uid in user db" }
            }
            withContext(Dispatchers.Main.immediate) { clearAuth() }
            true
        }

    suspend fun logoutFromServer() {
        val logoutAuth = AuthData.fromPrefs()
        BiliPassportHttpApi.logout(
            biliCSRF = logoutAuth.biliJct,
            sessData = logoutAuth.sessData,
            dedeUserID = logoutAuth.uid,
            dedeUserIDCkMd5 = logoutAuth.uidCkMd5,
            sid = logoutAuth.sid
        ).requireSuccess()
        logoutCurrentUser(logoutAuth)
    }

    suspend fun logoutOnAuthFailure(reason: String, authData: AuthData) {
        if (authData.uid <= 0L || authData.sessData.isBlank()) return
        authFailureVerifier.verify(
            authData = authData,
            isCurrentSession = ::isCurrentSession,
            validateSession = { credentials ->
                BiliHttpApi.getWebInterfaceNav(
                    buvid3 = Prefs.buvid3,
                    sessData = credentials.sessData,
                    dedeUserID = credentials.uid,
                    dedeUserIDCkMd5 = credentials.uidCkMd5,
                    biliJct = credentials.biliJct,
                    sid = credentials.sid
                ).isSessionAuthenticated(credentials.uid)
            },
            onInvalidSession = { credentials ->
                if (logoutCurrentUser(credentials)) {
                    logger.info { "Session invalidated after auth failure: $reason" }
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.exception_auth_failure)
                            .toast(BVApp.context)
                    }
                }
            },
            onVerificationFailure = { error ->
                logger.warn(error) { "Could not verify login session; keeping saved credentials" }
            }
        )
    }

    private fun clearAuth() {
        logger.info { "Clear auth data in UserRepository" }
        saveToPrefs(
            AuthData(
                uid = 0L,
                uidCkMd5 = "",
                sid = "",
                biliJct = "",
                sessData = "",
                tokenExpiredData = 0L
            ),
            isLogin = false
        )
        username = ""
        avatar = ""
    }

    private fun updateAuthRepository() {
        authRepository.sessionData = sessData
        authRepository.biliJct = biliJct
        authRepository.dedeUserIDCkMd5 = uidCkMd5
        authRepository.sid = sid
        authRepository.accessToken = accessToken
        authRepository.mid = uid
        authRepository.buvid3 = Prefs.buvid3
    }

    suspend fun setUser(user: UserDB) {
        authMutationMutex.withLock {
            withContext(Dispatchers.Main.immediate) {
                saveToPrefs(AuthData.fromJson(user.auth))
            }
        }
        BVApp.instance?.initRepository()
        BVApp.instance?.initProxy()
        updateAvatar()
    }

    /**
     * Verifies that the cookie returned by a login flow is usable before it is persisted.
     * The account id from the authenticated endpoint must match the one from the login result.
     */
    suspend fun validateAuthData(authData: AuthData): ValidatedUserIdentity {
        require(authData.uid > 0L) { "Invalid account id returned by login" }
        require(authData.sessData.isNotBlank()) { "Login cookie is empty" }

        val profile = BiliHttpApi.getWebInterfaceNav(
            buvid3 = Prefs.buvid3,
            sessData = authData.sessData,
            dedeUserID = authData.uid,
            dedeUserIDCkMd5 = authData.uidCkMd5,
            biliJct = authData.biliJct,
            sid = authData.sid
        ).getResponseData()
        check(profile.isLogin) { "Login cookie is not authenticated" }
        check(profile.mid == authData.uid) {
            "Login cookie does not match the returned account"
        }
        return ValidatedUserIdentity(
            uid = profile.mid,
            username = profile.uname,
            avatar = profile.face
        )
    }

    suspend fun addUser(
        authData: AuthData,
        identity: ValidatedUserIdentity? = null
    ) {
        require(identity == null || identity.uid == authData.uid) {
            "Validated account does not match the login result"
        }

        authMutationMutex.withLock {
            val existUser = db.userDao().findUserByUid(authData.uid)
            existUser?.let {
                it.auth = authData.toJson()
                identity?.username?.takeIf(String::isNotBlank)?.let { username ->
                    it.username = username
                }
                identity?.avatar?.takeIf(String::isNotBlank)?.let { avatar ->
                    it.avatar = avatar
                }
                db.userDao().update(it)
            } ?: let {
                val newUser = UserDB(
                    uid = authData.uid,
                    username = identity?.username?.takeIf(String::isNotBlank)
                        ?: "User ${authData.uid}",
                    avatar = identity?.avatar?.takeIf(String::isNotBlank)
                        ?: "https://i0.hdslb.com/bfs/article/b6b843d84b84a3ba5526b09ebf538cd4b4c8c3f3.jpg",
                    auth = authData.toJson()
                )
                db.userDao().insert(newUser)
            }
            withContext(Dispatchers.Main.immediate) { saveToPrefs(authData) }
        }
        BVApp.instance?.initRepository()
        BVApp.instance?.initProxy()
        if (identity == null) {
            updateAvatar()
        } else {
            reloadAvatar()
        }
    }

    suspend fun updateAvatar() {
        val user = db.userDao().findUserByUid(uid)
        user?.let {
            runCatching {
                val responseData =
                    BiliHttpApi.getWebInterfaceNav(
                        buvid3 = Prefs.buvid3,
                        sessData = sessData,
                        dedeUserID = uid,
                        dedeUserIDCkMd5 = uidCkMd5,
                        biliJct = biliJct,
                        sid = sid
                    ).getResponseData()
                check(responseData.isLogin && responseData.mid == uid) {
                    "Current account cookie is not authenticated"
                }
                logger.fInfo { "Updating user name and avatar" }
                username = responseData.uname
                avatar = responseData.face
                user.username = username
                user.avatar = avatar
                db.userDao().update(user)
            }.onFailure {
                logger.info {
                    "Update user name and avatar failed: ${it.stackTraceToString()}"
                }
            }
        }
    }

    suspend fun reloadAvatar() {
        val requestedUid = withContext(Dispatchers.Main.immediate) { uid }
        val user = db.userDao().findUserByUid(requestedUid)

        withContext(Dispatchers.Main.immediate) {
            // The active account may have changed while Room was loading the user.
            if (uid != requestedUid) return@withContext

            if (user == null || !isLogin) {
                username = ""
                avatar = ""
            } else {
                username = user.username
                avatar = user.avatar
            }
        }
    }

    suspend fun findUserByUid(uid: Long): UserDB? {
        return db.userDao().findUserByUid(uid)
    }

    suspend fun updateUser(user: UserDB){
        db.userDao().update(user)
    }
}
