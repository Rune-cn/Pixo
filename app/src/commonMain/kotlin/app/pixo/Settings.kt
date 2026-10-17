package app.pixo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

/**
 * 键值存储：全项目唯一的平台差异点。
 *
 * 之前每个设置项都要在 `expect object AppPrefs` + 两个 actual 里各写一遍
 * （25 项 × 3 处），新增设置极易漏同步（主题模式就因此漏了持久化）。
 * 现在平台侧只实现这 6 个读写原语，[AppPrefs] 用委托声明属性，
 * 新增设置只需在 [AppPrefs] 加一行。
 */
expect object SettingsStore {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
}

private fun stringPref(key: String, default: String = ""): ReadWriteProperty<Any?, String> =
    object : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = SettingsStore.getString(key, default)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) =
            SettingsStore.putString(key, value)
    }

private fun boolPref(key: String, default: Boolean): ReadWriteProperty<Any?, Boolean> =
    object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = SettingsStore.getBoolean(key, default)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
            SettingsStore.putBoolean(key, value)
    }

private fun longPref(key: String, default: Long): ReadWriteProperty<Any?, Long> =
    object : ReadWriteProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = SettingsStore.getLong(key, default)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) =
            SettingsStore.putLong(key, value)
    }

/**
 * 应用偏好。唯一的设置声明处：键名即存储键，默认值写在这里。
 *
 * 敏感项（cookie / access_token / refresh_token / webdavPassword）目前仍是平台明文存储，
 * 后续可替换 [SettingsStore] 的实现接入 Keystore 加密，调用方无需改动。
 */
object AppPrefs {
    // ---------- 登录凭据 ----------
    var cookie: String by stringPref("cookie")
    var refreshToken: String by stringPref("refresh_token")
    var accessToken: String by stringPref("access_token")
    var lastLoginUser: String by stringPref("last_login_user")
    var selfUserId: Long by longPref("self_user_id", 0L)

    // ---------- 外观 ----------
    /** 主题模式：[ColorSchemeMode] 的 name（System / Dark / Light），由 App 层解析。 */
    var themeMode: String by stringPref("theme_mode", "system")
    var dynamicColor: Boolean by boolPref("dynamic_color", true)
    var floatingBar: Boolean by boolPref("floating_bar", true)
    var liquidGlass: Boolean by boolPref("liquid_glass", true)
    var language: String by stringPref("language", "system")

    // ---------- 内容与交互 ----------
    var ugoiraConfirm: Boolean by boolPref("ugoira_confirm", true)
    var imageQuality: String by stringPref("image_quality", "standard")
    var waterfall: Boolean by boolPref("waterfall", false)
    var disableSwipeBack: Boolean by boolPref("disable_swipe_back", false)

    // ---------- 内容过滤 ----------
    /** 标签屏蔽：正则（每行一条）。 */
    var tagBlockPatterns: String by stringPref("tag_block_patterns")
    /** 标签屏蔽：字面量关键词（每行一条）。 */
    var tagBlockKeywords: String by stringPref("tag_block_keywords")
    /** 人（作者）屏蔽：作者名或 ID，一行一条。 */
    var blockedAuthors: String by stringPref("blocked_authors")

    // ---------- 数据 ----------
    var downloadDir: String by stringPref("download_dir")
    var history: String by stringPref("history")
    var debugLog: Boolean by boolPref("debug_log", false)

    // ---------- WebDAV 备份 ----------
    var webdavUrl: String by stringPref("webdav_url")
    var webdavUser: String by stringPref("webdav_user")
    var webdavPassword: String by stringPref("webdav_password")
}

/**
 * 设置变更版本号。
 *
 * 标签/人屏蔽与画质都在「解析列表时」生效，改完设置后已经缓存的列表不会自动更新。
 * 变更设置时调用 [bump]，列表页把 [current] 作为状态 key，即可在返回列表时重新拉取。
 */
object SettingsRevision {
    var current by mutableIntStateOf(0)
        private set

    fun bump() {
        current++
    }
}

/**
 * 让列表缓存失效：屏蔽规则与画质在「解析列表时」生效，
 * 改完设置必须清掉已缓存的列表并自增版本号，列表页才会重新拉取。
 */
fun invalidateFeedCaches() {
    FeedCache.clearLists()
    SettingsRevision.bump()
}

private val backupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

/**
 * 导出设置为 JSON 文本；失败返回空串。
 *
 * 注意：包含 WebDAV 账号密码（见 [SettingsBackup] 注释），但不含 Pixiv 登录凭据。
 */
fun exportSettingsJson(): String = runCatching {
    backupJson.encodeToString(SettingsBackup.fromPrefs())
}.getOrDefault("")

/**
 * 导入设置 JSON 并立即生效（含缓存失效与网络客户端重建）；成功返回 true。
 */
fun importSettingsJson(json: String): Boolean = runCatching {
    val backup = backupJson.decodeFromString<SettingsBackup>(json)
    backup.applyToPrefs()
    // 画质 / 屏蔽规则 / 下载目录都影响已缓存数据，统一失效
    invalidateFeedCaches()
    refreshNetworkConfig()
    applyAppLanguage(backup.language)
    true
}.getOrDefault(false)
