package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowBottomSheet
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

    /**
     * 更新清单（legado-with-MD3 的做法）：
     * `raw.githubusercontent.com` 不属于 GitHub API，没有 60 次/小时限流，
     * 而且由仓库维护者随 Release 一起更新（见 .github/workflows/update-manifest.yml），
     * 所以更新说明（Markdown 原文）任何时候都能拿到。
     */
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/$OWNER/$REPO/main/update-manifests/latest.json"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 检查更新（对外入口）。
     *
     * 每次冷启动 / 手动点击都会真正发起检查（不节流）：
     * 清单走 `raw.githubusercontent.com`，不属于 GitHub API，没有 60 次/小时限流；
     * 是否提示由调用方决定（版本比较 + 已提醒去重）。
     *
     * 顺序：清单（含完整说明）→ GitHub API → 网页重定向（只剩版本号）。
     */
    suspend fun check(): UpdateInfo {
        val info = try {
            fetchFromManifest() ?: fetchLatest()
        } catch (e: Exception) {
            logError("Update", "update check failed, fallback to web redirect", e)
            val version = runCatching { fetchLatestVersionByRedirect() }.getOrNull()
                ?: throw e
            UpdateInfo(
                version = version,
                notes = "",
                pageUrl = RELEASES_PAGE,
                apkUrls = emptyMap(),
            )
        }
        return info
    }

    /**
     * 按版本号取该版本 Release 的说明（「更新日志」用）。
     *
     * 当前版本恰好等于清单里的最新版时直接用清单（避免 API 限流）；
     * 否则走 `GET /releases/tags/vX.Y.Z`。找不到返回 null。
     */
    suspend fun fetchByTag(tag: String): UpdateInfo? {
        val normalized = normalize(tag)
        val manifest = runCatching { fetchFromManifest() }.getOrNull()
        if (manifest != null && manifest.version == normalized) return manifest

        return withContext(ioDispatcher) {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$OWNER/$REPO/releases/tags/v$normalized")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Pixo/${appVersionName()}")
                .build()
            val text = PixivHttp.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use ""
                response.body?.string().orEmpty()
            }
            if (text.isBlank()) return@withContext null
            val root = json.parseToJsonElement(text).jsonObject
            val tagName = root["tag_name"]?.jsonPrimitive?.content.orEmpty()
            if (tagName.isBlank()) return@withContext null
            UpdateInfo(
                version = normalize(tagName),
                notes = root["body"]?.jsonPrimitive?.content.orEmpty(),
                pageUrl = root["html_url"]?.jsonPrimitive?.content.orEmpty()
                    .ifBlank { RELEASES_PAGE },
                apkUrls = emptyMap(),
            )
        }
    }

    /**
     * 优先读更新清单（含完整说明）；清单缺失时退回 API。
     */
    private suspend fun fetchFromManifest(): UpdateInfo? = withContext(ioDispatcher) {
        val request = Request.Builder()
            .url(MANIFEST_URL)
            .header("User-Agent", "Pixo/${appVersionName()}")
            .build()
        try {
            val text = PixivHttp.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use ""
                response.body?.string().orEmpty()
            }
            val root = json.parseToJsonElement(text).jsonObject
            val version = root["version"]?.jsonPrimitive?.content.orEmpty()
            if (version.isBlank()) return@withContext null
            UpdateInfo(
                version = normalize(version),
                notes = root["notes"]?.jsonPrimitive?.content.orEmpty(),
                pageUrl = root["page_url"]?.jsonPrimitive?.content.orEmpty()
                    .ifBlank { RELEASES_PAGE },
                apkUrls = buildMap {
                    root["apk_urls"]?.jsonObject?.forEach { (name, element) ->
                        val url = (element as? JsonPrimitive)?.content.orEmpty()
                        if (url.isNotBlank()) put(name, url)
                    }
                },
            )
        } catch (_: Exception) {
            null
        }
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
 * 「发现新版本」底部弹窗（对齐 legado-with-MD3 的 UpdateSheet）。
 *
 * 结构：当前版本 / 新版本 / ABI 三行信息 → 可长按复制的 Markdown 更新说明
 * → 「下载更新」主按钮（下载 APK 并拉起系统安装器）→ 「稍后」。
 */
@Composable
internal fun UpdateAvailableSheet(info: UpdateInfo, onDismiss: () -> Unit) {
    var show by remember { mutableStateOf(true) }
    var installing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val sCurrent = Str.update_current_version()
    val sNew = Str.update_new_version()
    val sDownload = Str.update_download()
    val sDownloading = Str.update_downloading()
    val sNotesUnavailable = Str.update_notes_unavailable()

    WindowBottomSheet(
        show = show,
        title = Str.check_update(),
        onDismissRequest = { show = false },
        onDismissFinished = onDismiss,
        insideMargin = DpSize(0.dp, 0.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        ) {
            // 可滚动区：版本信息 + 更新说明（对齐 legado：内容滚动、按钮固定底部）
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                VersionRow(label = sCurrent, value = appVersionName(), bottomPadding = 4.dp)
                VersionRow(label = sNew, value = info.version, highlight = true, bottomPadding = 4.dp)
                if (deviceAbi.isNotBlank()) VersionRow(label = "ABI", value = deviceAbi, bottomPadding = 8.dp)

                SelectionContainer {
                    MarkdownBlock(
                        content = info.notes.ifBlank { sNotesUnavailable },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!installing) {
                        installing = true
                        error = null
                        scope.launch {
                            val message = installUpdate(info)
                            installing = false
                            if (message != null) error = message
                        }
                    }
                },
                enabled = !installing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (installing) sDownloading else sDownload) }
            error?.let {
                Text(
                    text = it,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** 版本信息行：label + value（新版本用主题色高亮），间距对齐 legado。 */
@Composable
private fun VersionRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    bottomPadding: Dp = 4.dp,
) {
    Row(
        Modifier.padding(bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = if (highlight) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        )
    }
}
/** 版本信息行：label + value（新版本用主题色高亮）。 */
@Composable
private fun VersionRow(label: String, value: String, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = if (highlight) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 下载并安装：按当前 ABI 挑 APK 直链；没有直链时退回浏览器打开下载页。
 *
 * @return null 表示成功；否则返回错误描述
 */
private suspend fun installUpdate(info: UpdateInfo): String? {
    val abi = deviceAbi
    val url = info.apkUrls[abi]
        ?: info.apkUrls.entries.firstOrNull { abi.isNotEmpty() && it.key.contains(abi) }?.value
    if (url.isNullOrBlank()) {
        if (info.pageUrl.isNotBlank()) openUrlInBrowser(info.pageUrl)
        return if (abi.isEmpty()) "桌面端请在浏览器下载安装包" else "当前通道未提供 APK 直链，请在浏览器下载"
    }
    return installApkFromUrl(url, "pixo-${info.version}-$abi.apk")
}
