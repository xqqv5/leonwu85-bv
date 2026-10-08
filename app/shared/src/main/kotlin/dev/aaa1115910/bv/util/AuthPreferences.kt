package dev.aaa1115910.bv.util

import androidx.datastore.preferences.core.edit
import de.schnettler.datastore.manager.DataStoreManager
import dev.aaa1115910.bv.entity.AuthData
import kotlinx.coroutines.flow.first

internal suspend fun DataStoreManager.readAuthData(): AuthData {
    val preferences = preferenceFlow.first()
    return AuthData(
        uid = preferences[PrefKeys.prefUidKey] ?: 0L,
        uidCkMd5 = preferences[PrefKeys.prefUidCkMd5Key].orEmpty(),
        sid = preferences[PrefKeys.prefSidKey].orEmpty(),
        biliJct = preferences[PrefKeys.prefBiliJctKey].orEmpty(),
        sessData = preferences[PrefKeys.prefSessDataKey].orEmpty(),
        tokenExpiredData = preferences[PrefKeys.prefTokenExpiredDateKey] ?: 0L,
        accessToken = preferences[PrefKeys.prefAccessTokenKey].orEmpty(),
        refreshToken = preferences[PrefKeys.prefRefreshTokenKey].orEmpty()
    )
}

internal suspend fun DataStoreManager.saveAuthData(authData: AuthData, isLogin: Boolean) {
    // Keep the existing keys, but commit the login flag and all credentials together.
    dataStore.edit { preferences ->
        preferences[PrefKeys.prefIsLoginKey] = isLogin
        preferences[PrefKeys.prefUidKey] = authData.uid
        preferences[PrefKeys.prefUidCkMd5Key] = authData.uidCkMd5
        preferences[PrefKeys.prefSidKey] = authData.sid
        preferences[PrefKeys.prefSessDataKey] = authData.sessData
        preferences[PrefKeys.prefBiliJctKey] = authData.biliJct
        preferences[PrefKeys.prefTokenExpiredDateKey] = authData.tokenExpiredData
        preferences[PrefKeys.prefAccessTokenKey] = authData.accessToken
        preferences[PrefKeys.prefRefreshTokenKey] = authData.refreshToken
    }
}
