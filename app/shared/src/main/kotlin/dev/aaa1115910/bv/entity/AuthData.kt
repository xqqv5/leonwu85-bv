package dev.aaa1115910.bv.entity

import dev.aaa1115910.bv.util.Prefs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AuthData(
    @SerialName("DedeUserID")
    val uid: Long,
    @SerialName("DedeUserID__ckMd5")
    val uidCkMd5: String,
    val sid: String,
    @SerialName("bili_jct")
    val biliJct: String,
    @SerialName("SESSDATA")
    val sessData: String,
    @SerialName("expired_date")
    val tokenExpiredData: Long,
    @SerialName("access_token")
    val accessToken: String = "",
    @SerialName("refresh_token")
    val refreshToken: String = ""
) {
    companion object {
        fun fromJson(json: String): AuthData {
            return Json.decodeFromString(json)
        }

        fun fromPrefs(): AuthData = Prefs.readAuthData()
    }

    fun toJson(): String = Json.encodeToString(this)
    fun saveToPrefs() {
        Prefs.saveAuthData(this)
    }
}
