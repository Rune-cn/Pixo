package app.pixo

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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

    actual var downloadDir: String
        get() = prefs.getString("download_dir", "") ?: ""
        set(value) {
            prefs.edit().putString("download_dir", value).apply()
        }

    actual var ugoiraConfirm: Boolean
        get() = prefs.getBoolean("ugoira_confirm", true)
        set(value) {
            prefs.edit().putBoolean("ugoira_confirm", value).apply()
        }

    actual var tagBlockPatterns: String
        get() = prefs.getString("tag_block_patterns", "").orEmpty()
        set(value) {
            prefs.edit().putString("tag_block_patterns", value).apply()
        }

    actual var webdavUrl: String
        get() = prefs.getString("webdav_url", "").orEmpty()
        set(value) {
            prefs.edit().putString("webdav_url", value).apply()
        }

    actual var webdavUser: String
        get() = prefs.getString("webdav_user", "").orEmpty()
        set(value) {
            prefs.edit().putString("webdav_user", value).apply()
        }

    actual var webdavPassword: String
        get() = prefs.getString("webdav_password", "").orEmpty()
        set(value) {
            prefs.edit().putString("webdav_password", value).apply()
        }

    actual var history: String
        get() = prefs.getString("history", "").orEmpty()
        set(value) {
            prefs.edit().putString("history", value).apply()
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
    LogBuffer.add("openUrlInBrowser: $url")
    try {
        val uri = android.net.Uri.parse(url)
        // 用系统“默认浏览器”主选择器直达浏览器，绕过本应用注册的 pixiv 网页深链过滤器
        val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
        intent.data = uri
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            androidAppContext.startActivity(intent)
            return
        } catch (e: Exception) {
            LogBuffer.add("openUrlInBrowser: default browser failed (${e.message}), fallback to VIEW")
        }
        // 兜底：ACTION_VIEW 解析浏览器候选，排除自身深链
        val fallback = Intent(Intent.ACTION_VIEW, uri)
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val candidates = androidAppContext.packageManager.queryIntentActivities(
            fallback,
            android.content.pm.PackageManager.MATCH_ALL,
        )
        val target = candidates.firstOrNull { it.activityInfo.packageName != androidAppContext.packageName }
        if (target != null) {
            fallback.setClassName(target.activityInfo.packageName, target.activityInfo.name)
            androidAppContext.startActivity(fallback)
        } else {
            val chooser = Intent.createChooser(fallback, "打开链接")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            androidAppContext.startActivity(chooser)
        }
    } catch (e: Exception) {
        LogBuffer.add("openUrlInBrowser: failed (${e.message})")
    }
}

actual fun shareText(text: String) {
    if (!::androidAppContext.isInitialized) return
    try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, "分享")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        androidAppContext.startActivity(chooser)
    } catch (_: Exception) {
    }
}

/** 按文件名后缀猜 MIME 类型（写 MediaStore / SAF 用）。 */
private fun mimeTypeFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "zip" -> "application/zip"
    "mp4" -> "video/mp4"
    "jpeg", "jpg" -> "image/jpeg"
    else -> "application/octet-stream"
}

/** 默认目录 Pictures/Pixo：API 29+ 走 MediaStore（无需任何权限）。 */
private suspend fun writeDefaultDir(fileName: String, bytes: ByteArray): String? {
    if (Build.VERSION.SDK_INT >= 29) {
        val resolver = androidAppContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeTypeFor(fileName))
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Pixo")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("MediaStore insert returned null")
        try {
            val out = resolver.openOutputStream(uri)
                ?: throw IllegalStateException("openOutputStream returned null")
            out.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return "Pictures/Pixo/$fileName"
        } catch (e: Exception) {
            try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            throw e
        }
    } else {
        // 旧系统：写公共 Pictures/Pixo 目录（需要存储权限）
        val dirFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Pixo",
        )
        dirFile.mkdirs()
        val f = File(dirFile, fileName)
        f.writeBytes(bytes)
        return f.absolutePath
    }
}

actual suspend fun writeDownloadFile(fileName: String, bytes: ByteArray): String? =
    kotlinx.coroutines.withContext(ioDispatcher) {
        try {
            val dir = AppPrefs.downloadDir
            if (dir.isBlank()) {
                return@withContext writeDefaultDir(fileName, bytes)
            }
            // 用户自选目录（SAF）：优先通过 DocumentsContract 写入
            val uri = Uri.parse(dir)
            try {
                val resolver = androidAppContext.contentResolver
                val doc = DocumentsContract.createDocument(resolver, uri, mimeTypeFor(fileName), fileName)
                    ?: throw IllegalStateException("SAF createDocument returned null")
                val out = resolver.openOutputStream(doc)
                    ?: throw IllegalStateException("SAF openOutputStream returned null")
                out.use { it.write(bytes) }
                return@withContext doc.toString()
            } catch (e: Exception) {
                // 部分厂商设备（vivo 等）对某些卷的 tree uri 在 provider 端无效（Invalid URI），
                // SAF 写入会失败。此时**不清用户目录设置**，改用“所有文件访问”权限
                // 直接写 tree uri 对应的真实文件路径；仍失败就如实抛错，让界面提示原因
                val dirFile = safTreeToFileDir(uri)
                if (dirFile != null && Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) {
                    try {
                        val f = File(dirFile, fileName)
                        f.writeBytes(bytes)
                        return@withContext f.absolutePath
                    } catch (raw: Exception) {
                        logError("Pixo", "download dir raw write failed", raw)
                    }
                }
                throw e
            }
        } catch (e: Exception) {
            logError("Pixo", "write download fail", e)
            null
        }
    }
actual suspend fun listDownloadedImages(): List<DownloadedImage> =
    kotlinx.coroutines.withContext(ioDispatcher) {
        try {
            val dir = AppPrefs.downloadDir
            if (dir.isBlank()) {
                // 默认目录：MediaStore 里 Pictures/Pixo 相对路径的图片
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_ADDED,
                )
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val args = arrayOf(Environment.DIRECTORY_PICTURES + "/Pixo%")
                val out = mutableListOf<DownloadedImage>()
                androidAppContext.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    args,
                    "${MediaStore.MediaColumns.DATE_ADDED} DESC",
                )?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val name = c.getString(nameCol) ?: continue
                        val size = c.getLong(sizeCol)
                        out += DownloadedImage(name, "content://media/external/images/media/$id", size)
                    }
                }
                return@withContext out
            }
            // 自选目录：SAF tree 下枚举子文件
            val treeUri = Uri.parse(dir)
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )
            val out = mutableListOf<DownloadedImage>()
            androidAppContext.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val mimeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (c.moveToNext()) {
                    val name = c.getString(nameCol) ?: continue
                    if (!isImageFileName(name)) continue
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, c.getString(idCol))
                    out += DownloadedImage(name, docUri.toString(), c.getLong(sizeCol))
                }
            }
            out.sortByDescending { it.name }
            out
        } catch (e: Exception) {
            // SAF/MediaStore 枚举失败（部分厂商卷 provider 拒绝 tree 查询）：
            // 不清用户目录设置，改为直接从文件系统枚举（需要所有文件访问权限）
            logError("Pixo", "list downloaded images fail", e)
            try {
                listImagesFromFileSystem()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

/** 直接从文件系统枚举下载目录图片（SAF/MediaStore 枚举失败时的兜底；需要所有文件访问权限）。 */
private fun listImagesFromFileSystem(): List<DownloadedImage> {
    val dir = AppPrefs.downloadDir
    val dirFile = if (dir.isBlank()) {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Pixo",
        )
    } else {
        safTreeToFileDir(Uri.parse(dir))
    } ?: return emptyList()
    if (!dirFile.isDirectory) return emptyList()
    return dirFile.listFiles()
        ?.filter { it.isFile && isImageFileName(it.name) }
        ?.sortedByDescending { it.lastModified() }
        ?.map { DownloadedImage(it.name, Uri.fromFile(it).toString(), it.length()) }
        .orEmpty()
}

/** SAF tree uri → 真实文件系统目录（形如 primary:MT2/pixo → /storage/emulated/0/MT2/pixo）。 */
private fun safTreeToFileDir(treeUri: Uri): File? = try {
    val docId = DocumentsContract.getTreeDocumentId(treeUri)
    val sep = docId.indexOf(":")
    val volume = if (sep >= 0) docId.substring(0, sep) else "primary"
    val rel = if (sep >= 0) docId.substring(sep + 1) else docId
    val base = when {
        volume == "primary" -> Environment.getExternalStorageDirectory().absolutePath
        else -> {
            val sm = androidAppContext.getSystemService(android.content.Context.STORAGE_SERVICE)
                as? android.os.storage.StorageManager
            sm?.storageVolumes?.firstOrNull { it.uuid == volume }?.directory?.absolutePath
        }
    } ?: return null
    val f = File(base, rel)
    if (f.isDirectory || f.mkdirs()) f else null
} catch (e: Exception) {
    null
}

actual fun appVersionName(): String = try {
    androidAppContext.packageManager.getPackageInfo(androidAppContext.packageName, 0).versionName ?: "1.0"
} catch (_: Exception) {
    "1.0"
}

@Composable
actual fun rememberDownloadFolderPicker(onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (e: Exception) {
                // 个别厂商（vivo/OPPO 等）对部分卷的持久化授权会抛异常：
                // 仍保存本轮会话可用的目录；下次启动若失效则下载会提示，重新选择即可
                logError("Pixo", "persist download dir permission failed", e)
            }
            AppPrefs.downloadDir = uri.toString()
            onPicked(uri.toString())
        }
    }
    return { launcher.launch(null) }
}

@Composable
actual fun rememberExportSettingsFile(onDone: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val json = exportSettingsJson()
            if (json.isBlank()) {
                onDone(false)
                return@rememberLauncherForActivityResult
            }
            val out = context.contentResolver.openOutputStream(uri)
                ?: run {
                    onDone(false)
                    return@rememberLauncherForActivityResult
                }
            out.use { it.write(json.toByteArray()) }
            onDone(true)
        } catch (e: Exception) {
            logError("Pixo", "export settings fail", e)
            onDone(false)
        }
    }
    return { launcher.launch("pixo-settings.json") }
}

@Composable
actual fun rememberImportSettingsFile(onLoaded: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?.toString(Charsets.UTF_8)
            onLoaded(text)
        } catch (e: Exception) {
            logError("Pixo", "import settings read fail", e)
            onLoaded(null)
        }
    }
    return { launcher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
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
