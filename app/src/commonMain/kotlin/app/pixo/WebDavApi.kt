package app.pixo

import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * WebDAV 设置备份：把「导出设置」的 JSON 上传为 pixo-settings.json，或拉回并立即生效。
 * 认证走 Basic（服务器地址 / 用户名 / 密码来自 AppPrefs）。
 */
object WebDavApi {

    private const val FILE = "pixo-settings.json"

    private fun baseUrl(): String {
        var u = AppPrefs.webdavUrl.trim().trimEnd('/')
        if (u.isBlank()) throw IOException("WebDAV 服务器地址为空")
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
        return u
    }

    private fun authHeader(): String =
        "Basic " + Base64.getEncoder()
            .encodeToString("${AppPrefs.webdavUser}:${AppPrefs.webdavPassword}".toByteArray())

    private fun builder(): Request.Builder =
        Request.Builder()
            .header("Authorization", authHeader())
            .header("User-Agent", PixivHttp.USER_AGENT)

    /** 备份：把设置 JSON PUT 到服务器。成功返回 true。 */
    suspend fun backup(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = builder()
                .url("${baseUrl()}/$FILE")
                .put(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            PixivHttp.client.newCall(req).execute().use { resp ->
                resp.isSuccessful || resp.code == 201
            }
        } catch (e: Exception) {
            logError("WebDav", "backup fail", e)
            false
        }
    }

    /** 恢复：GET 服务器上的 pixo-settings.json，返回原始 JSON；失败返回 null。 */
    suspend fun restore(): String? = withContext(Dispatchers.IO) {
        try {
            val req = builder().url("${baseUrl()}/$FILE").get().build()
            PixivHttp.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.string()
            }
        } catch (e: Exception) {
            logError("WebDav", "restore fail", e)
            null
        }
    }
}
