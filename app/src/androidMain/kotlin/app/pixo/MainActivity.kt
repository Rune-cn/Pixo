package app.pixo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    /** 深链信号：App 首屏与 onNewIntent 都通过它把 pixiv:// 链接导航到作品详情。 */
    private val pendingDeepLink = mutableStateOf<PixivDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissionsIfNeeded()
        attachAndroidContext(applicationContext)
        applyAppLanguage(AppPrefs.language)
        initCrashLogging()
        initImageLoaderOnce()
        pendingDeepLink.value = parseDeepLink(intent)
        setContent {
            App(deepLinkSignal = pendingDeepLink)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseDeepLink(intent)?.let { pendingDeepLink.value = it }
    }

    /** 打开应用时动态申请存储权限：Android 11+ 引导到系统页授予「所有文件访问」，旧系统弹运行时权限。 */
    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            // 「所有文件访问」是特殊权限，不能走运行时权限弹窗，只能跳到系统设置页
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: Exception) {
                try {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } catch (_: Exception) {
                }
            }
            return
        }
        val perms = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
        }
    }

    /** 解析 `pixiv://artworks/{id}`、`pixiv://novel/{id}` 与网页链接为深链目标。 */
    private fun parseDeepLink(intent: Intent?): PixivDeepLink? {
        val data = intent?.data ?: return null
        val host = data.host ?: return null
        val path = data.path ?: return null
        return try {
            when {
                host == "artworks" -> PixivDeepLink(path.removePrefix("/").substringBefore('/').toLong(), "illust")
                host == "novel" -> PixivDeepLink(path.removePrefix("/").substringBefore('/').toLong(), "novel")
                host == "user" -> PixivDeepLink(userId = path.removePrefix("/").substringBefore('/').toLong())
                host == "www.pixiv.net" && path.startsWith("/artworks/") ->
                    PixivDeepLink(path.removePrefix("/artworks/").substringBefore('/').toLong(), "illust")
                host == "www.pixiv.net" && path.startsWith("/novel/show.php") ->
                    PixivDeepLink(data.getQueryParameter("id")?.toLong() ?: return null, "novel")
                host == "www.pixiv.net" && path.startsWith("/users/") ->
                    PixivDeepLink(userId = path.removePrefix("/users/").substringBefore('/').toLong())
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
