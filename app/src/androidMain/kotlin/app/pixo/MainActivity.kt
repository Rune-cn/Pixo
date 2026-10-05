package app.pixo

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf

class MainActivity : AppCompatActivity() {
    /** 深链信号：App 首屏与 onNewIntent 都通过它把 pixiv:// 链接导航到作品详情。 */
    private val pendingDeepLink = mutableStateOf<PixivDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissionsIfNeeded()
        AppPrefs.attach(applicationContext)
        AppPrefs.init()
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

    /** 打开应用时动态申请存储读写权限（仅旧系统生效；API 33+ 系统自动忽略）。 */
    private fun requestPermissionsIfNeeded() {
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
