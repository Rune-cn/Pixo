package app.pixo

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual val isDesktop: Boolean = false


internal lateinit var androidAppContext: Context

actual object AppPrefs {
    private lateinit var appContext: Context
    private val prefs get() = appContext.getSharedPreferences("pixo", Context.MODE_PRIVATE)

    fun attach(context: Context) {
        appContext = context
        androidAppContext = context
    }

    actual fun init() {}

    actual var cookie: String
        get() = prefs.getString("cookie", "") ?: ""
        set(value) {
            prefs.edit().putString("cookie", value).apply()
        }

    actual var refreshToken: String
        get() = prefs.getString("refresh_token", "") ?: ""
        set(value) {
            prefs.edit().putString("refresh_token", value).apply()
        }

    actual var accessToken: String
        get() = prefs.getString("access_token", "") ?: ""
        set(value) {
            prefs.edit().putString("access_token", value).apply()
        }

    actual var lastLoginUser: String
        get() = prefs.getString("last_login_user", "") ?: ""
        set(value) {
            prefs.edit().putString("last_login_user", value).apply()
        }

    actual var selfUserId: Long
        get() = prefs.getLong("self_user_id", 0L)
        set(value) {
            prefs.edit().putLong("self_user_id", value).apply()
        }

    actual var debugLog: Boolean
        get() = prefs.getBoolean("debug_log", false)
        set(value) {
            prefs.edit().putBoolean("debug_log", value).apply()
        }

    actual var language: String
        get() = prefs.getString("language", "system") ?: "system"
        set(value) {
            prefs.edit().putString("language", value).apply()
        }

    actual var predictiveBack: Boolean
        get() = prefs.getBoolean("predictive_back", true)
        set(value) {
            prefs.edit().putBoolean("predictive_back", value).apply()
        }

    actual var dynamicColor: Boolean
        get() = prefs.getBoolean("dynamic_color", true)
        set(value) {
            prefs.edit().putBoolean("dynamic_color", value).apply()
        }

    actual var floatingBar: Boolean
        get() = prefs.getBoolean("floating_bar", true)
        set(value) {
            prefs.edit().putBoolean("floating_bar", value).apply()
        }

    actual var liquidGlass: Boolean
        get() = prefs.getBoolean("liquid_glass", true)
        set(value) {
            prefs.edit().putBoolean("liquid_glass", value).apply()
        }
}

actual object LocalAppLocale {
    actual val current: String
        @Composable get() = Locale.getDefault().toLanguageTag()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        // 应用语言已由 AppCompatDelegate/LocaleManager（Google 标准）管理，
        // 这里不再手动改 Configuration，仅原样透传，交由标准资源管线响应语言变化。
        return LocalConfiguration.provides(LocalConfiguration.current)
    }
}

actual fun applyAppLanguage(language: String) {
    val locales = when (language) {
        "zh" -> LocaleListCompat.forLanguageTags("zh")
        "en" -> LocaleListCompat.forLanguageTags("en")
        else -> LocaleListCompat.getEmptyLocaleList()
    }
    AppCompatDelegate.setApplicationLocales(locales)
}

actual fun log(tag: String, message: String) {
    if (!AppPrefs.debugLog) return
    LogBuffer.add("[$tag] $message")
    Log.d(tag, message)
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        Log.e(tag, message, throwable)
    } else {
        Log.e(tag, message)
    }
}

/** 崩溃日志文件（应用私有目录，无需权限）。 */
private fun crashFile(): File = File(androidAppContext.filesDir, "crash.log")

actual fun initCrashLogging() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val text = "$stamp | ${thread.name}\n${throwable.stackTraceToString()}\n\n"
            crashFile().appendText(text)
            // 同步写入公共「下载」目录（API 29+，无需权限）：无 adb 时也能从文件管理器取日志
            try {
                val resolver = androidAppContext.contentResolver
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "pixo_crash.log")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                }
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
        }
        Log.e("Pixo", "CRASH in ${thread.name}", throwable)
        previous?.uncaughtException(thread, throwable)
    }
}

actual fun readCrashLog(): String = try {
    crashFile().readText()
} catch (_: Exception) {
    ""
}

actual fun clearCrashLog() {
    try {
        crashFile().delete()
    } catch (_: Exception) {
    }
}

actual fun openUrlInBrowser(url: String) {
    if (!::androidAppContext.isInitialized) return
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        androidAppContext.startActivity(intent)
    } catch (_: Exception) {
    }
}

@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun applyWindowBackground(background: Color) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.decorView.setBackgroundColor(background.toArgb())
            val isDark = background.luminance() < 0.5f
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }
}
