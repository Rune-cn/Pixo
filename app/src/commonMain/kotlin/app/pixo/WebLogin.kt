package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Pixiv 网页登录地址。 */
internal const val PIXIV_LOGIN_URL = "https://accounts.pixiv.net/login?lang=zh"

/**
 * 网页内嵌登录页。
 *
 * 平台只负责把「检测到的会话 Cookie」交回来（[PlatformWebLoginContent.onSession]），
 * **不立即启用**；这里等用户确认后再写入 [AppPrefs] 并重建 OkHttp 客户端，
 * 避免自动套用意外会话。平台差异只在 [PlatformWebLoginContent]：
 * Android 内嵌 WebView，桌面端 JVM 退回「打开系统浏览器 + 粘贴 Cookie」。
 *
 * 注意：所有 [Str] 调用都在组合上下文中预先取值，回调里只使用已捕获的字符串，
 * 因为 WebView 的回调并非组合作用域，不能直接调用 @Composable 函数。
 */
@Composable
internal fun WebLoginScreen(onBack: () -> Unit) {
    val confirmText = Str.web_login_confirm()
    val successText = Str.web_login_success()
    var status by remember { mutableStateOf("") }
    var detected by remember { mutableStateOf<String?>(null) }
    var signedIn by remember { mutableStateOf(false) }

    fun apply(raw: String) {
        AppPrefs.cookie = raw.trim()
        AppPrefs.selfUserId = PixivHttp.selfUserId()
        PixivHttp.refreshClient()
        detected = null
        signedIn = true
        status = successText
    }

    PixoSubScreen(title = Str.web_login(), onBack = onBack) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (status.isNotBlank()) {
                Text(
                    text = status,
                    style = MiuixTheme.textStyles.footnote2,
                    color = if (signedIn) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (detected != null && !signedIn) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = { detected?.let(::apply) }) {
                        Text(confirmText)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            PlatformWebLoginContent(
                modifier = Modifier.fillMaxWidth().weight(1f),
                onStatus = { message -> status = message },
                onSession = { raw ->
                    val trimmed = raw.trim()
                    if (!signedIn && trimmed.isNotBlank() && detected != trimmed) {
                        detected = trimmed
                    }
                },
            )
        }
    }
}

/**
 * 平台相关的登录页主体。
 *
 * @param onStatus 上报进度/错误文案
 * @param onSession 检测到可用的会话 Cookie 时上报（此时尚未启用，等用户确认）
 */
@Composable
internal expect fun PlatformWebLoginContent(
    modifier: Modifier,
    onStatus: (String) -> Unit,
    onSession: (String) -> Unit,
)