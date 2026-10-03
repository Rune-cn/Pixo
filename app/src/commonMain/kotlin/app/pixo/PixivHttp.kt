package app.pixo

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext

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
                if (req.header("Cookie") == null) cookieHeader()?.let { b.header("Cookie", it) }
                if (req.header("Authorization") == null) {
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

    suspend fun getJson(builder: Request.Builder): JsonObject = withContext(ioDispatcher) {
        val url = builder.build().url.toString()
        log("PixoApi", "GET $url | auth=${if (PixivAuth.isTokenMode) "token" else if (AppPrefs.cookie.isBlank()) "none" else "cookie"}")
        val request = builder.build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            log("PixoApi", "HTTP ${response.code} len=${text.length}")
            if (!response.isSuccessful) throw java.io.IOException(httpError(response.code))
            val element = json.parseToJsonElement(text)
            (element as? JsonObject) ?: throw java.io.IOException("响应不是 JSON 对象")
        }
    }

    suspend fun postJson(
        url: String,
        payload: String,
        headers: Request.Builder,
        form: Boolean = false,
    ): JsonObject = withContext(ioDispatcher) {
        val mediaType = if (form) "application/x-www-form-urlencoded" else "application/json; charset=utf-8"
        val request = headers.url(url)
            .post(payload.toRequestBody(mediaType.toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            log("PixoApi", "POST $url HTTP ${response.code} len=${text.length}")
            if (!response.isSuccessful) throw java.io.IOException(httpError(response.code))
            val element = json.parseToJsonElement(text)
            (element as? JsonObject) ?: throw java.io.IOException("响应不是 JSON 对象")
        }
    }

    /** 把常见 HTTP 错误转成容易看懂的中文提示，401 尤其要明说。 */
    private fun httpError(code: Int): String = when (code) {
        401 -> "未登录或会话失效，请重新登录（HTTP 401）"
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
