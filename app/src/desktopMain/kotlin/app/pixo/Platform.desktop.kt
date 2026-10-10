package app.pixo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import java.io.File
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.time.LocalDateTime
import java.util.Locale
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual val isDesktop: Boolean = true


val LocalPixoWindow = staticCompositionLocalOf<java.awt.Window?> { null }

private val defaultLocale: Locale = Locale.getDefault()

actual object AppPrefs {
    private val prefs = Preferences.userRoot().node("app/pixo")

    actual fun init() {}

    actual var cookie: String
        get() = prefs.get("cookie", "")
        set(value) {
            prefs.put("cookie", value)
        }

    actual var refreshToken: String
        get() = prefs.get("refresh_token", "")
        set(value) {
            prefs.put("refresh_token", value)
        }

    actual var accessToken: String
        get() = prefs.get("access_token", "")
        set(value) {
            prefs.put("access_token", value)
        }

    actual var lastLoginUser: String
        get() = prefs.get("last_login_user", "")
        set(value) {
            prefs.put("last_login_user", value)
        }

    actual var selfUserId: Long
        get() = prefs.getLong("self_user_id", 0L)
        set(value) {
            prefs.putLong("self_user_id", value)
        }

    actual var debugLog: Boolean
        get() = prefs.getBoolean("debug_log", false)
        set(value) {
            prefs.putBoolean("debug_log", value)
        }

    actual var language: String
        get() = prefs.get("language", "system")
        set(value) {
            prefs.put("language", value)
        }

    actual var predictiveBack: Boolean
        get() = prefs.getBoolean("predictive_back", true)
        set(value) {
            prefs.putBoolean("predictive_back", value)
        }

    actual var dynamicColor: Boolean
        get() = prefs.getBoolean("dynamic_color", true)
        set(value) {
            prefs.putBoolean("dynamic_color", value)
        }

    actual var floatingBar: Boolean
        get() = prefs.getBoolean("floating_bar", true)
        set(value) {
            prefs.putBoolean("floating_bar", value)
        }

    actual var liquidGlass: Boolean
        get() = prefs.getBoolean("liquid_glass", true)
        set(value) {
            prefs.putBoolean("liquid_glass", value)
        }

    actual var downloadDir: String
        get() = prefs.get("download_dir", "")
        set(value) {
            prefs.put("download_dir", value)
        }

    actual var ugoiraConfirm: Boolean
        get() = prefs.getBoolean("ugoira_confirm", true)
        set(value) {
            prefs.putBoolean("ugoira_confirm", value)
        }

    actual var tagBlockPatterns: String
        get() = prefs.getString("tag_block_patterns", "")
        set(value) {
            prefs.putString("tag_block_patterns", value)
        }

    actual var webdavUrl: String
        get() = prefs.getString("webdav_url", "")
        set(value) {
            prefs.putString("webdav_url", value)
        }

    actual var webdavUser: String
        get() = prefs.getString("webdav_user", "")
        set(value) {
            prefs.putString("webdav_user", value)
        }

    actual var webdavPassword: String
        get() = prefs.getString("webdav_password", "")
        set(value) {
            prefs.putString("webdav_password", value)
        }

    actual var history: String
        get() = prefs.getString("history", "")
        set(value) {
            prefs.putString("history", value)
        }
}

actual object LocalAppLocale {
    private var default: Locale? = null
    private val LocalAppLocale = staticCompositionLocalOf { Locale.getDefault().toString() }

    actual val current: String
        @Composable get() = LocalAppLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (default == null) {
            default = Locale.getDefault()
        }
        val new = when (value) {
            null -> default!!
            else -> Locale(value)
        }
        Locale.setDefault(new)
        return LocalAppLocale.provides(new.toString())
    }
}

actual fun log(tag: String, message: String) {
    if (!AppPrefs.debugLog) return
    LogBuffer.add("[$tag] $message")
    println("[$tag] $message")
    try {
        val dir = File(System.getProperty("user.home"), ".pixo")
        dir.mkdirs()
        File(dir, "pixo.log").appendText("${LocalDateTime.now()} [$tag] $message\n")
    } catch (_: Exception) {
        // 忽略写入失败
    }
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    println("[$tag] ERROR: $message")
    throwable?.printStackTrace()
}

private fun crashFile(): File {
    val dir = File(System.getProperty("user.home"), ".pixo")
    dir.mkdirs()
    return File(dir, "crash.log")
}

actual fun initCrashLogging() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            crashFile().appendText(
                "${LocalDateTime.now()} | ${thread.name}\n${throwable.stackTraceToString()}\n\n",
            )
        } catch (_: Exception) {
            // 忽略写入失败
        }
        println("[Pixo] CRASH in ${thread.name}")
        throwable.printStackTrace()
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
    try {
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
        }
    } catch (_: Exception) {
    }
}

actual fun shareText(text: String) {
    try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    } catch (_: Exception) {
    }
}

actual suspend fun writeDownloadFile(fileName: String, bytes: ByteArray): String? =
    kotlinx.coroutines.withContext(ioDispatcher) {
        try {
            val dir = AppPrefs.downloadDir.ifBlank {
                File(System.getProperty("user.home"), "Downloads/Pixo").absolutePath
            }
            val dirFile = File(dir)
            dirFile.mkdirs()
            val f = File(dirFile, fileName)
            f.writeBytes(bytes)
            f.absolutePath
        } catch (e: Exception) {
            logError("Pixo", "write download fail", e)
            null
        }
    }

actual suspend fun listDownloadedImages(): List<DownloadedImage> =
    kotlinx.coroutines.withContext(ioDispatcher) {
        try {
            val dir = AppPrefs.downloadDir.ifBlank {
                File(System.getProperty("user.home"), "Downloads/Pixo").absolutePath
            }
            val dirFile = File(dir)
            if (!dirFile.isDirectory) return@withContext emptyList()
            dirFile.listFiles()
                ?.filter { it.isFile && isImageFileName(it.name) }
                ?.map { DownloadedImage(it.name, it.toURI().toString(), it.length()) }
                ?.sortedByDescending { it.name }
                ?: emptyList()
        } catch (e: Exception) {
            logError("Pixo", "list downloaded images fail", e)
            emptyList()
        }
    }

actual fun appVersionName(): String = "1.0"

@Composable
actual fun rememberDownloadFolderPicker(onPicked: (String) -> Unit): () -> Unit {
    return {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "选择下载目录"
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            AppPrefs.downloadDir = chooser.selectedFile.absolutePath
            onPicked(chooser.selectedFile.absolutePath)
        }
    }
}

@Composable
actual fun rememberExportSettingsFile(onDone: (Boolean) -> Unit): () -> Unit = {
    val chooser = JFileChooser().apply {
        dialogTitle = "导出设置"
        selectedFile = File(System.getProperty("user.home"), "pixo-settings.json")
    }
    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        try {
            var f = chooser.selectedFile
            if (!f.name.contains('.')) f = File(f.absolutePath + ".json")
            val json = exportSettingsJson()
            if (json.isBlank()) {
                onDone(false)
            } else {
                f.writeText(json)
                onDone(true)
            }
        } catch (e: Exception) {
            logError("Pixo", "export settings fail", e)
            onDone(false)
        }
    }
}

@Composable
actual fun rememberImportSettingsFile(onLoaded: (String?) -> Unit): () -> Unit = {
    val chooser = JFileChooser().apply {
        dialogTitle = "导入设置"
        isAcceptAllFileFilterUsed = true
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        try {
            onLoaded(chooser.selectedFile.readText())
        } catch (e: Exception) {
            logError("Pixo", "import settings read fail", e)
            onLoaded(null)
        }
    }
}

actual fun applyAppLanguage(language: String) {
    val new = when (language) {
        "zh" -> Locale.SIMPLIFIED_CHINESE
        "en" -> Locale.ENGLISH
        else -> defaultLocale
    }
    Locale.setDefault(new)
}

@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop 无系统返回手势
}

@Composable
actual fun applyWindowBackground(background: Color) {
    val window = LocalPixoWindow.current
    SideEffect {
        window?.background = java.awt.Color(background.red, background.green, background.blue, background.alpha)
    }
}

actual suspend fun loadUgoiraFrames(zipUrl: String, frameFiles: List<String>): List<ByteArray>? =
    kotlinx.coroutines.withContext(ioDispatcher) {
        try {
            val bytes = PixivHttp.fetchBytes(PixivHttp.webJsonHeaders().url(zipUrl))
            val wanted = frameFiles.toHashSet()
            val out = LinkedHashMap<String, ByteArray>()
            java.util.zip.ZipInputStream(bytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name in wanted) out[entry.name] = zip.readBytes()
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            frameFiles.mapNotNull { out[it] }.takeIf { it.size == frameFiles.size }
        } catch (e: Exception) {
            logError("Pixo", "ugoira zip fail", e)
            null
        }
    }
