package app.pixo

import androidx.compose.foundation.layout.size
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 共享 HTTP 基础设施：OkHttp 客户端、鉴权头、App API 请求签名与 JSON 辅助函数。
 * 网页 API（Cookie）与 App API（Bearer Token）两条传输通道共用此层。
 */
object PixivHttp {
    const val WEB_REFERER = "https://www.pixiv.net/"
    const val APP_REFERER = "https://app-api.pixiv.net/"

    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    const val APP_USER_AGENT = "PixivAndroidApp/5.0.234 (Android 11; Pixel 5)"

    private val json = Json { ignoreUnknownKeys = true }

    var client: OkHttpClient = buildClient()
        private set

    fun refreshClient() {
        client = buildClient()
    }

    private fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val b = req.newBuilder()
                if (req.header("Referer") == null) b.header("Referer", WEB_REFERER)
                if (req.header("User-Agent") == null) b.header("User-Agent", USER_AGENT)
                // Cookie 只在网页域下发，避免把旧会话捎带给 App API 导致 400
                if (req.header("Cookie") == null && req.url.host == "www.pixiv.net") {
                    cookieHeader()?.let { b.header("Cookie", it) }
                }
                // Bearer Token 只发给 App API，避免污染网页域请求（与 Cookie 规则对偶）。
                if (req.header("Authorization") == null && req.url.host == "app-api.pixiv.net") {
                    PixivAuth.accessToken.takeIf { it.isNotBlank() }
                        ?.let { b.header("Authorization", "Bearer $it") }
                }
                chain.proceed(b.build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 拼装 Cookie 头。
     *
     * 网页内嵌登录拿到的是多子域合并后的完整会话（含 device_token 等），按浏览器行为整体发出；
     * 只粘贴了裸 session id 时补上 PHPSESSID 名称，兼容旧的粘贴方式。
     */
    fun cookieHeader(): String? {
        val raw = AppPrefs.cookie.replace("\r", "").replace("\n", "").trim()
        if (raw.isEmpty()) return null
        if (raw.contains("=")) return raw
        return "PHPSESSID=$raw"
    }

    /** 从 PHPSESSID 前缀推导登录用户 ID。 */
    fun selfUserId(): Long {
        val raw = AppPrefs.cookie
        val m = Regex("(?i)(?:^|;)\\s*PHPSESSID=([^;]+)").find(raw)
        val session = m?.groupValues?.get(1)?.substringBefore("_")
        return session?.trim()?.toLongOrNull() ?: 0L
    }

    /** App API 请求签名（X-Client-Time / X-Client-Hash）。 */
    fun appApiTimestamp(): String =
        ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))

    fun appApiHash(timestamp: String): String =
        md5Hex(timestamp + PixivAuth.HASH_SECRET)

    fun md5Hex(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** 网页 API 常用 JSON 请求头。 */
    fun webJsonHeaders(): Request.Builder {
        return Request.Builder()
            .header("User-Agent", USER_AGENT)
            .header("Referer", WEB_REFERER)
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
    }

    /** App API 标准请求头（含签名）。 */
    fun appApiHeaders(): Request.Builder {
        val ts = appApiTimestamp()
        return Request.Builder()
            .header("User-Agent", APP_USER_AGENT)
            .header("Referer", APP_REFERER)
            .header("App-OS", "android")
            .header("App-OS-Version", "11")
            .header("App-Version", "5.0.234")
            .header("Accept-Language", "zh-CN")
            .header("X-Client-Time", ts)
            .header("X-Client-Hash", appApiHash(ts))
    }

    /** 抓取原始文本（HTML 页面等）。 */
    suspend fun getText(builder: Request.Builder): String = withContext(ioDispatcher) {
        val url = builder.build().url.toString()
        log("PixoApi", "TEXT $url")
        val request = builder.build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            log("PixoApi", "HTTP ${response.code} len=${text.length}")
            if (!response.isSuccessful) throw java.io.IOException(httpError(response.code))
            text
        }
    }

    /** 抓取原始字节（ugoira zip 等二进制资源）。 */
    suspend fun fetchBytes(builder: Request.Builder): ByteArray = withContext(ioDispatcher) {
        val url = builder.build().url.toString()
        log("PixoApi", "BIN $url")
        val request = builder.build()
        client.newCall(request).execute().use { response ->
            val body = response.body
            val bytes = if (body != null) body.bytes() else ByteArray(0)
            log("PixoApi", "HTTP ${response.code} len=${bytes.size}")
            if (!response.isSuccessful) throw java.io.IOException(httpError(response.code))
            bytes
        }
    }

    suspend fun getJson(builder: Request.Builder): JsonObject = withContext(ioDispatcher) {
        val url = builder.build().url.toString()
        log("PixoApi", "GET $url | auth=${if (PixivAuth.isTokenMode) "token" else if (AppPrefs.cookie.isBlank()) "none" else "cookie"}")
        var request = builder.build()
        var (code, text) = execute(request)
        maybeRefreshAndRetry(code, builder) { refreshedRequest ->
            request = refreshedRequest
            val retry = execute(request)
            code = retry.first
            text = retry.second
        }
        log("PixoApi", "HTTP $code len=${text.length}")
        if (code !in 200..299) throw java.io.IOException(httpError(code) + " " + text.take(300))
        val element = json.parseToJsonElement(text)
        (element as? JsonObject) ?: throw java.io.IOException("响应不是 JSON 对象")
    }

    suspend fun postJson(
        url: String,
        payload: String,
        headers: Request.Builder,
        form: Boolean = false,
    ): JsonObject = withContext(ioDispatcher) {
        val mediaType = if (form) "application/x-www-form-urlencoded" else "application/json; charset=utf-8"
        var request = headers.url(url)
            .post(payload.toRequestBody(mediaType.toMediaType()))
            .build()
        var (code, text) = execute(request)
        maybeRefreshAndRetry(code, headers) { refreshedRequest ->
            request = refreshedRequest
            val retry = execute(request)
            code = retry.first
            text = retry.second
        }
        log("PixoApi", "POST $url HTTP $code len=${text.length}")
        if (code !in 200..299) throw java.io.IOException(httpError(code) + " " + text.take(300))
        val element = json.parseToJsonElement(text)
        (element as? JsonObject) ?: throw java.io.IOException("响应不是 JSON 对象")
    }

    private fun execute(request: Request): Pair<Int, String> =
        client.newCall(request).execute().use { response ->
            response.code to response.body?.string().orEmpty()
        }

    private val tokenRefreshLock = Mutex()

    /**
     * App API 的 Access Token 过期时返回 400/401；此时用 Refresh Token 换新 Token 后重试一次。
     * 并发请求共享同一把锁，避免重复刷新；重试请求由拦截器在发送时读取最新 Token。
     */
    private suspend fun maybeRefreshAndRetry(code: Int, builder: Request.Builder, retry: (Request) -> Unit) {
        if (code != 400 && code != 401) return
        if (!PixivAuth.isTokenMode || PixivAuth.refreshToken.isBlank()) return
        tokenRefreshLock.withLock {
            val before = PixivAuth.accessToken
            // 刷新失败（refresh_token 也失效）时保持原响应，由上层展示登录失效提示
            if (PixivAuth.refreshAccessToken() != null) return@withLock
            // Access Token 没变化说明刚被刷新过或无须换新，直接重试即可
            if (PixivAuth.accessToken != before || before.isBlank()) {
                retry(builder.build())
            }
        }
    }

    /** 把常见 HTTP 错误转成容易看懂的中文提示，401 尤其要明说。 */
    private fun httpError(code: Int): String = when (code) {
        401 -> "未登录或会话失效，请重新登录（HTTP 401）"
        403 -> "需要登录后才能访问（HTTP 403）"
        400 -> "请求被拒绝，多因未登录或会话过期（HTTP 400）"
        404 -> "接口不存在或已失效（HTTP 404）"
        429 -> "请求过于频繁，请稍后再试（HTTP 429）"
        else -> "HTTP $code"
    }

}

/** JSON 读取辅助扩展。 */
fun JsonObject.str(name: String): String {
    val el = this[name] ?: return ""
    if (el is JsonNull) return ""
    return el.jsonPrimitive.content
}

fun JsonObject.lng(name: String): Long = str(name).toLongOrNull() ?: 0L

fun JsonObject.int(name: String): Int = str(name).toIntOrNull() ?: 0

fun JsonObject.obj(name: String): JsonObject? = this[name] as? JsonObject

fun JsonObject.arr(name: String): kotlinx.serialization.json.JsonArray? =
    this[name] as? kotlinx.serialization.json.JsonArray
