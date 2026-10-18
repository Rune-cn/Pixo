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

    private val json = Json { ignoreUnknownKeys = true }

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
                text = info.notes.ifBlank { "—" },
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
