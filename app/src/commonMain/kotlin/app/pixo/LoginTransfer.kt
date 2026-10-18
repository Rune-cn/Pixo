package app.pixo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 可迁移的登录信息。
 *
 * 只包含「恢复登录所需的最小集合」，不含任何偏好设置；
 * 与 [SettingsBackup]（设置备份）分开，避免把凭据混进设置备份里。
 */
@Serializable
data class LoginBackup(
    val refreshToken: String = "",
    val accessToken: String = "",
    val cookie: String = "",
    val selfUserId: Long = 0L,
    val lastLoginUser: String = "",
)

/**
 * 登录信息的导出 / 导入。
 *
 * 导出为 JSON（稳定、可解析）；导入同时兼容：
 * - 本应用导出的 JSON
 * - 旧版调试页那套 `---- refresh_token ----` 分段文本
 *   （用户可能已经把那段文本存下来了，不兼容就等于丢掉旧凭据）
 */
object LoginTransfer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** 导出当前登录信息（JSON 文本）。 */
    fun export(): String = json.encodeToString(
        LoginBackup(
            refreshToken = AppPrefs.refreshToken,
            accessToken = AppPrefs.accessToken,
            cookie = AppPrefs.cookie,
            selfUserId = AppPrefs.selfUserId,
            lastLoginUser = AppPrefs.lastLoginUser,
        ),
    )

    /**
     * 导入登录信息并立即生效。
     *
     * @return null 表示成功；否则返回可直接展示的错误描述
     */
    fun import(text: String): String? {
        val backup = parse(text) ?: return if (text.isBlank()) "内容为空" else "无法识别的登录信息格式"
        if (backup.refreshToken.isBlank() && backup.cookie.isBlank()) {
            return "缺少 refresh_token 或 cookie"
        }

        AppPrefs.refreshToken = backup.refreshToken
        AppPrefs.accessToken = backup.accessToken
        AppPrefs.cookie = backup.cookie
        AppPrefs.selfUserId = backup.selfUserId
        AppPrefs.lastLoginUser = backup.lastLoginUser
        // 账号变了：清掉会话缓存并重建网络客户端（拦截器会读取新的 Cookie / Token）
        SessionCache.selfInfo = null
        refreshNetworkConfig()
        return null
    }

    /**
     * 兼容旧格式：
     *
     * ```
     * Pixo login info (debug)
     * ---- refresh_token ----
     * xxxxx
     * ---- access_token ----
     * xxxxx
     * ```
     */
    /** 解析登录信息文本：先按 JSON，再退回旧版分段文本。解析失败返回 null。 */
    internal fun parse(text: String): LoginBackup? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { json.decodeFromString<LoginBackup>(trimmed) }.getOrNull()
            ?: parseLegacy(trimmed)
    }

    private fun parseLegacy(text: String): LoginBackup? {
        val values = mutableMapOf<String, String>()
        var key: String? = null
        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("----") && trimmed.endsWith("----") && trimmed.length > 8) {
                key = trimmed.removePrefix("----").removeSuffix("----").trim()
            } else if (key != null && trimmed.isNotEmpty()) {
                values[key!!] = if (trimmed == "(empty)") "" else trimmed
            }
        }
        if (values.isEmpty()) return null
        return LoginBackup(
            refreshToken = values["refresh_token"].orEmpty(),
            accessToken = values["access_token"].orEmpty(),
            cookie = values["cookie"].orEmpty(),
            selfUserId = values["self_user_id"]?.toLongOrNull() ?: 0L,
            lastLoginUser = values["last_login_user"].orEmpty(),
        )
    }
}
