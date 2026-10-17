package app.pixo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineDispatcher

/**
 * 阻塞式网络请求使用的调度器。
 * 所有 HTTP 调用都通过它切离主线程，避免 Android 主线程网络操作抛 NetworkOnMainThreadException。
 */
expect val ioDispatcher: CoroutineDispatcher

/** 是否为桌面端（JVM）。桌面端用 ESC 键模拟系统返回。 */
expect val isDesktop: Boolean

expect object AppPrefs {
    fun init()
    var cookie: String
    var refreshToken: String
    var accessToken: String
    var lastLoginUser: String
    var selfUserId: Long
    var debugLog: Boolean
    var language: String
    var predictiveBack: Boolean
    var dynamicColor: Boolean
    var floatingBar: Boolean
    var liquidGlass: Boolean
    /** 下载目录（空 = 平台默认目录，如 Android 的 Pictures/Pixo）。 */
    var downloadDir: String
    /** 动图（ugoira）播放前是否需要手动确认（避免详情页自动轮播造成闪烁）。 */
    var ugoiraConfirm: Boolean
    /** 标签屏蔽规则：每行一条正则，匹配到作品的任一标签即过滤。 */
    var tagBlockPatterns: String
    /** WebDAV 备份服务器地址 / 用户名 / 密码。 */
    var webdavUrl: String
    var webdavUser: String
    var webdavPassword: String
    /** 标签屏蔽：正则（每行一条）。 */
    var tagBlockKeywords: String

    /** 作者屏蔽：每行一条（作者名或 ID，支持正则）。 */
    var blockedAuthors: String

    /** 本地观看历史（JSON 序列化）。 */
    var history: String
    /** 首页作品列表是否用两列瀑布流展示。 */
    var waterfall: Boolean
}

expect object LocalAppLocale {
    val current: String
        @Composable get
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
fun AppEnvironment(language: String, content: @Composable () -> Unit) {
    val locale = when (language) {
        "zh" -> "zh"
        "en" -> "en"
        else -> null
    }
    CompositionLocalProvider(LocalAppLocale provides locale) {
        key(locale) {
            content()
        }
    }
}

expect fun log(tag: String, message: String)
expect fun logError(tag: String, message: String, throwable: Throwable? = null)

/** 安装全局未捕获异常处理器，把崩溃堆栈持久化到本地文件。 */
expect fun initCrashLogging()

/** 读取已持久化的崩溃日志，无则返回空串。 */
expect fun readCrashLog(): String

/** 清空已持久化的崩溃日志。 */
expect fun clearCrashLog()

/** 在系统浏览器中打开链接（用于登录与核实网页端功能）。 */
expect fun openUrlInBrowser(url: String)

/** 分享文本/链接：Android 弹系统分享面板，桌面端复制到剪贴板。 */
expect fun shareText(text: String)

/** 把下载的图片字节写入下载目录（返回保存位置说明，失败返回 null）。 */
expect suspend fun writeDownloadFile(fileName: String, bytes: ByteArray): String?

/** 列出已下载目录中的图片（Android 读 MediaStore 或 SAF 目录，桌面读本地目录）。 */
expect suspend fun listDownloadedImages(): List<DownloadedImage>

/** 应用版本名（关于页显示）。 */
expect fun appVersionName(): String

/** 弹出「选择下载目录」界面，选中后持久化到 [AppPrefs.downloadDir] 并回调目录值。返回可触发选择的回调。 */
@Composable
expect fun rememberDownloadFolderPicker(onPicked: (String) -> Unit): () -> Unit

/** 弹出「导出设置」保存界面，写出 JSON 备份；成功回调 true。返回可触发选择的回调。 */
@Composable
expect fun rememberExportSettingsFile(onDone: (Boolean) -> Unit): () -> Unit

/** 弹出「导入设置」选择界面；读取到文件文本后回调（取消回调 null）。返回可触发选择的回调。 */
@Composable
expect fun rememberImportSettingsFile(onLoaded: (String?) -> Unit): () -> Unit

/**
 * 下载 ugoira zip 并解压出帧图片字节（顺序与 [frameFiles] 一致）。
 * 任一帧缺失或下载失败返回 null，由调用方回退到静态图。
 */
expect suspend fun loadUgoiraFrames(zipUrl: String, frameFiles: List<String>): List<ByteArray>?

@Composable
expect fun applyWindowBackground(background: Color)

/** 按 Google 标准切换应用语言（Android 走 AppCompatDelegate/LocaleManager，桌面走 Locale.setDefault）。 */
expect fun applyAppLanguage(language: String)

@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)

object LogBuffer {
    private const val MAX = 300
    private val lines = mutableListOf<String>()

    fun add(line: String) {
        synchronized(lines) {
            lines.add(line)
            if (lines.size > MAX) lines.removeAt(0)
        }
    }

    fun snapshot(): List<String> = synchronized(lines) { lines.toList() }

    fun clear() {
        synchronized(lines) { lines.clear() }
    }
}

internal var loaderContext: PlatformContext? = null
    private set

@OptIn(DelicateCoilApi::class)
internal fun initImageLoaderOnce() {
    SingletonImageLoader.setSafe { context ->
        loaderContext = context
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(PixivHttp.client)) }
            // coil 默认自带内存缓存；头像列表数据由 FeedCache 缓存，避免重复请求
            .build()
    }
}

@OptIn(DelicateCoilApi::class)
internal fun refreshNetworkConfig() {
    PixivHttp.refreshClient()
    val context = loaderContext ?: return
    SingletonImageLoader.setUnsafe(
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(PixivHttp.client)) }
            .build()
    )
}

/** 导出全部设置（含登录凭据）为 JSON 文本；失败返回空串。 */
fun exportSettingsJson(): String = runCatching {
    Json.encodeToString(
        SettingsBackup(
            debugLog = AppPrefs.debugLog,
            language = AppPrefs.language,
            predictiveBack = AppPrefs.predictiveBack,
            dynamicColor = AppPrefs.dynamicColor,
            floatingBar = AppPrefs.floatingBar,
            liquidGlass = AppPrefs.liquidGlass,
            downloadDir = AppPrefs.downloadDir,
            ugoiraConfirm = AppPrefs.ugoiraConfirm,
            tagBlockPatterns = AppPrefs.tagBlockPatterns,
            tagBlockKeywords = AppPrefs.tagBlockKeywords,
            blockedAuthors = AppPrefs.blockedAuthors,
            webdavUrl = AppPrefs.webdavUrl,
            webdavUser = AppPrefs.webdavUser,
            webdavPassword = AppPrefs.webdavPassword,
            waterfall = AppPrefs.waterfall,
        ),
    )
}.getOrDefault("")

/** 导入设置 JSON 文本并立即生效；成功返回 true。 */
fun importSettingsJson(json: String): Boolean = runCatching {
    val b = Json.decodeFromString<SettingsBackup>(json)
    AppPrefs.debugLog = b.debugLog
    AppPrefs.language = b.language
    AppPrefs.predictiveBack = b.predictiveBack
    AppPrefs.dynamicColor = b.dynamicColor
    AppPrefs.floatingBar = b.floatingBar
    AppPrefs.liquidGlass = b.liquidGlass
    AppPrefs.downloadDir = b.downloadDir
    AppPrefs.ugoiraConfirm = b.ugoiraConfirm
    AppPrefs.tagBlockPatterns = b.tagBlockPatterns
    AppPrefs.tagBlockKeywords = b.tagBlockKeywords
    AppPrefs.blockedAuthors = b.blockedAuthors
    AppPrefs.webdavUrl = b.webdavUrl
    AppPrefs.webdavUser = b.webdavUser
    AppPrefs.webdavPassword = b.webdavPassword
    AppPrefs.waterfall = b.waterfall
    PixivApi.refreshClient()
    refreshNetworkConfig()
    applyAppLanguage(b.language)
    true
}.getOrDefault(false)

/** 清除内存缓存：列表 / 详情 / 评论缓存与图片内存、磁盘缓存。 */
fun clearAppCache() {
    FeedCache.clear()
    val ctx = loaderContext
    if (ctx != null) {
        runCatching {
            val loader = SingletonImageLoader.get(ctx)
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
        }
    }
}

/** 是否为常见图片扩展名（下载页列表过滤用）。 */
internal fun isImageFileName(name: String): Boolean =
    name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "avif")
