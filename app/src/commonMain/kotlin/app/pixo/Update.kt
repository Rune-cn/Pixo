package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 一条可用的更新信息（来自 GitHub Releases）。 */
data class UpdateInfo(
    /** 版本号，已去掉前缀 `v`，例如 `1.1.1`。 */
    val version: String,
    /** Release 说明（Markdown 原文）。 */
    val notes: String,
    /** Release 页面地址。 */
    val pageUrl: String,
    /** ABI/文件名 → APK 直链。 */
    val apkUrls: Map<String, String>,
)

/**
 * 检查更新：直接读 GitHub 官方 Releases API。
 *
 * `GET https://api.github.com/repos/{owner}/{repo}/releases/latest`
 * - 只返回最新**正式版**，draft / prerelease 自动排除（所以 nightly 不会误报）
 * - `User-Agent` 是 GitHub 强制要求的请求头，缺失会 403
 * - 未认证调用限流 60 次/小时（按 IP），对「启动时检查一次 + 手动检查」足够
 * - 建议带上 `X-GitHub-Api-Version: 2022-11-28` 固定 API 版本
 */
object UpdateChecker {
    private const val OWNER = "Rune-cn"
    private const val REPO = "Pixo"
    private const val LATEST_RELEASE = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private const val RELEASES_PAGE = "https://github.com/$OWNER/$REPO/releases"

    /** 自动检查的最小间隔：GitHub 未认证接口只有 60 次/小时（按 IP，共享网络更容易耗尽）。 */
    private const val AUTO_CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 检查更新（对外入口）。
     *
     * - 自动检查（[force] = false）6 小时内只跑一次，避免撞限流
     * - API 失败（常见：403 限流）时退回「网页重定向」探测版本：
     *   `GET https://github.com/{owner}/{repo}/releases/latest` 会 302 到 `/releases/tag/vX.Y.Z`，
     *   这条走的是网页，不消耗 API 配额，只是拿不到更新说明
     *
     * @return 最新版本信息；6 小时内已检查过且非强制时返回 null
     */
    suspend fun check(force: Boolean = false): UpdateInfo? {
        val now = System.currentTimeMillis()
        if (!force && now - AppPrefs.updateCheckedAt < AUTO_CHECK_INTERVAL_MS) return null
        val info = try {
            fetchLatest()
        } catch (e: Exception) {
            logError("Update", "GitHub API failed, fallback to web redirect", e)
            val version = runCatching { fetchLatestVersionByRedirect() }.getOrNull()
                ?: throw e
            UpdateInfo(
                version = version,
                notes = "",
                pageUrl = RELEASES_PAGE,
                apkUrls = emptyMap(),
            )
        }
        AppPrefs.updateCheckedAt = System.currentTimeMillis()
        AppPrefs.updateLatestVersion = info.version
        return info
    }

    /**
     * 网页兜底：跟随 `releases/latest` 的重定向，从最终 URL 的 `/tag/vX.Y.Z` 里取版本号。
     * 不消耗 GitHub API 配额。
     */
    private suspend fun fetchLatestVersionByRedirect(): String? = withContext(ioDispatcher) {
        val request = Request.Builder()
            .url("$RELEASES_PAGE/latest")
            .header("User-Agent", "Pixo/${appVersionName()}")
            .build()
        PixivHttp.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val finalUrl = response.request.url.toString()
            Regex("/tag/v?([0-9][0-9.]*)/?$").find(finalUrl)?.groupValues?.get(1)
        }
    }

    suspend fun fetchLatest(): UpdateInfo = withContext(ioDispatcher) {
        val request = Request.Builder()
            .url(LATEST_RELEASE)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Pixo/${appVersionName()}")
            .build()

        val text = PixivHttp.client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw java.io.IOException(
                    when (response.code) {
                        403 -> "GitHub 接口限流（60 次/小时），请稍后再试"
                        404 -> "未找到发布信息"
                        else -> "HTTP ${response.code}"
                    },
                )
            }
            body
        }

        val root = json.parseToJsonElement(text).jsonObject
        val tag = root["tag_name"]?.jsonPrimitive?.content.orEmpty()
        val apks = buildMap {
            root["assets"]?.jsonArray?.forEach { element ->
                val asset = element.jsonObject
                val name = asset["name"]?.jsonPrimitive?.content.orEmpty()
                val url = asset["browser_download_url"]?.jsonPrimitive?.content.orEmpty()
                if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) put(name, url)
            }
        }
        UpdateInfo(
            version = normalize(tag),
            notes = root["body"]?.jsonPrimitive?.content.orEmpty(),
            pageUrl = root["html_url"]?.jsonPrimitive?.content.orEmpty(),
            apkUrls = apks,
        )
    }

    /** `v1.1.1` → `1.1.1`。 */
    fun normalize(version: String): String = version.trim().removePrefix("v").removePrefix("V")

    /**
     * 语义化版本比较：`latest` 比 `current` 新时返回 true。
     *
     * 逐段按数字比较，缺段按 0 处理（`1.2` == `1.2.0`），非数字段忽略。
     */
    fun isNewer(latest: String, current: String): Boolean {
        val a = normalize(latest).split('.').mapNotNull { it.toIntOrNull() }
        val b = normalize(current).split('.').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

/**
 * 「发现新版本」对话框：展示版本号与 Release 说明，可跳转下载页。
 *
 * 说明用 Markdown 原文展示（不做渲染）——这里只是更新提示，保持轻量。
 */
@Composable
internal fun UpdateAvailableDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    OverlayDialog(show = true, onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = Str.update_available(info.version),
                style = MiuixTheme.textStyles.title3,
            )
            Text(
                text = info.notes.ifBlank { Str.update_notes_unavailable() },
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
            )
            Button(
                onClick = {
                    if (info.pageUrl.isNotBlank()) openUrlInBrowser(info.pageUrl)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(Str.open_in_browser()) }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(Str.cancel())
            }
        }
    }
}
