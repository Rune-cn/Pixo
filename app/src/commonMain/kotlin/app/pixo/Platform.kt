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
import kotlinx.coroutines.CoroutineDispatcher

/**
 * 阻塞式网络请求使用的调度器。
 * 所有 HTTP 调用都通过它切离主线程，避免 Android 主线程网络操作抛 NetworkOnMainThreadException。
 */
expect val ioDispatcher: CoroutineDispatcher

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
