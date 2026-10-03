package app.pixo

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OAuth 登录（App API Token）。
 * 用户提供 Pixiv OAuth Refresh Token，应用在启动/过期时自动换取 Access Token，
 * 之后的请求以 `Authorization: Bearer` 走 app-api.pixiv.net，无需 Cookie 与 CSRF。
 */
object PixivAuth {
    const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    const val HASH_SECRET = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"
    private const val TOKEN_URL = "https://oauth.secure.pixiv.net/auth/token"
    private const val REDIRECT_URI = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback"
    private const val FORM = "application/x-www-form-urlencoded"

    var refreshToken: String
        get() = AppPrefs.refreshToken
        set(value) {
            AppPrefs.refreshToken = value.trim()
        }

    var accessToken: String
        get() = AppPrefs.accessToken
        set(value) {
            AppPrefs.accessToken = value
        }

    var lastLoginUser: String
        get() = AppPrefs.lastLoginUser
        set(value) {
            AppPrefs.lastLoginUser = value
        }

    val isLoggedIn: Boolean get() = refreshToken.isNotBlank() || accessToken.isNotBlank()

    /** 是否启用 Token（App API）通道；未配置 Token 时回退到 Cookie 网页通道。 */
    val isTokenMode: Boolean get() = isLoggedIn

    /**
     * 用 Refresh Token 换取 Access Token。
     * 返回 null 表示成功；返回错误描述表示失败。
     */
    suspend fun login(rt: String): String? = withContext(Dispatchers.Default) {
        if (rt.isBlank()) return@withContext "Refresh Token 不能为空"
        try {
            val result = requestToken("refresh_token=${
                URLEncoder.encode(rt.trim(), "UTF-8")
            }", "refresh_token")
            val access = result.str("access_token")
            if (access.isBlank()) return@withContext "登录失败：响应缺少 access_token"
            refreshToken = rt
            accessToken = access
            val user = result.obj("user")
            val name = user?.str("name")?.ifBlank { user?.str("account") } ?: ""
            val account = user?.str("account").orEmpty()
            lastLoginUser = if (name.isNotBlank() && account.isNotBlank()) "$name (@$account)" else name
            AppPrefs.selfUserId = user?.lng("id") ?: 0L
            log("PixivAuth", "OAuth 登录成功 user=$lastLoginUser id=${AppPrefs.selfUserId}")
            PixivHttp.refreshClient()
            null
        } catch (e: Exception) {
            log("PixivAuth", "OAuth 登录失败: ${e.message}")
            e.message ?: "登录失败"
        }
    }

    /** 刷新 Access Token；失败时返回错误描述。 */
    suspend fun refreshAccessToken(): String? = withContext(Dispatchers.Default) {
        if (refreshToken.isBlank()) return@withContext "未配置 Refresh Token"
        try {
            val result = requestToken("refresh_token=${
                URLEncoder.encode(refreshToken, "UTF-8")
            }", "refresh_token")
            val access = result.str("access_token")
            if (access.isBlank()) return@withContext "刷新失败：响应缺少 access_token"
            accessToken = access
            result.str("refresh_token").takeIf { it.isNotBlank() }?.let { refreshToken = it }
            log("PixivAuth", "Access Token 已刷新")
            null
        } catch (e: Exception) {
            log("PixivAuth", "刷新失败: ${e.message}")
            e.message ?: "刷新失败"
        }
    }

    fun logout() {
        refreshToken = ""
        accessToken = ""
        lastLoginUser = ""
        AppPrefs.selfUserId = 0L
        PixivHttp.refreshClient()
    }

    /**
     * PKCE 登录（Authorization Code + PKCE，2022 年后 Pixiv 唯一登录方式）。
     * 返回 (code_verifier, code_challenge)，浏览器打开授权页登录后跳转 pixiv://account/login?code=XXXX，
     * 用 code + verifier 调 [exchangeAuthCode] 换 refresh_token。
     */
    fun pkceChallenge(): Pair<String, String> {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        val verifier = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        return verifier to challenge
    }

    fun pkceLoginUrl(challenge: String): String =
        "https://app-api.pixiv.net/web/v1/login?code_challenge=$challenge&code_challenge_method=S256&client=pixiv-android"

    /** 用授权码 + verifier 换 token；成功返回 null，失败返回错误描述。 */
    suspend fun exchangeAuthCode(code: String, verifier: String): String? = withContext(ioDispatcher) {
        try {
            val ts = PixivHttp.appApiTimestamp()
            val form = "client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&code=${URLEncoder.encode(code, "UTF-8")}" +
                "&code_verifier=$verifier&grant_type=authorization_code&include_policy=true" +
                "&redirect_uri=${URLEncoder.encode(REDIRECT_URI, "UTF-8")}"
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(form.toRequestBody(FORM.toMediaType()))
                .header("User-Agent", PixivHttp.APP_USER_AGENT)
                .header("App-OS", "android")
                .header("App-OS-Version", "11")
                .header("App-Version", "5.0.234")
                .header("Content-Type", FORM)
                .header("X-Client-Time", ts)
                .header("X-Client-Hash", PixivHttp.appApiHash(ts))
                .build()
            val text = PixivHttp.client.newCall(request).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw java.io.IOException("HTTP ${r.code} $body")
                body
            }
            val result = kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject
            val access = result.str("access_token")
            if (access.isBlank()) return@withContext "登录失败：响应缺少 access_token"
            accessToken = access
            result.str("refresh_token").takeIf { it.isNotBlank() }?.let { refreshToken = it }
            val user = result.obj("user")
            val name = user?.str("name").orEmpty()
            val account = user?.str("account").orEmpty()
            lastLoginUser = if (name.isNotBlank() && account.isNotBlank()) "$name (@$account)" else name
            AppPrefs.selfUserId = user?.lng("id") ?: 0L
            PixivHttp.refreshClient()
            log("PixivAuth", "PKCE 登录成功 user=$lastLoginUser id=${AppPrefs.selfUserId}")
            null
        } catch (e: Exception) {
            log("PixivAuth", "PKCE 登录失败: ${e.message}")
            e.message ?: "登录失败"
        }
    }

    private suspend fun requestToken(formData: String, grant: String): JsonObject =
        withContext(ioDispatcher) {
            val ts = PixivHttp.appApiTimestamp()
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(("client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&grant_type=$grant&$formData" +
                    "&get_secure_url=1&include_policy=true").toRequestBody(FORM.toMediaType()))
                .header("User-Agent", PixivHttp.APP_USER_AGENT)
                .header("App-OS", "android")
                .header("App-OS-Version", "11")
                .header("App-Version", "5.0.234")
                .header("Accept-Language", "zh-CN")
                .header("Content-Type", FORM)
                .header("X-Client-Time", ts)
                .header("X-Client-Hash", PixivHttp.appApiHash(ts))
                .build()
            PixivHttp.client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                log("PixivAuth", "token HTTP ${response.code} len=${text.length}")
                if (!response.isSuccessful) {
                    throw java.io.IOException("HTTP ${response.code}")
                }
                val element = kotlinx.serialization.json.Json.parseToJsonElement(text)
                (element as? JsonObject) ?: throw java.io.IOException("响应不是 JSON 对象")
            }
        }
}
