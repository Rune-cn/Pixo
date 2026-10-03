package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.awt.Desktop
import java.net.URI

/**
 * 桌面端实现：JVM 没有可嵌入的浏览器组件，退化为「打开系统浏览器 + 粘贴 Cookie」。
 * 浏览器里的会话无法被程序读取，所以这一步必须由用户手动粘贴。
 */
@Composable
internal actual fun PlatformWebLoginContent(
    modifier: Modifier,
    onStatus: (String) -> Unit,
    onSession: (String) -> Unit,
) {
    val hintText = Str.web_login_browser_hint()
    val openLabel = Str.web_login_open_browser()
    val cookieLabel = Str.cookie_label()
    val loginLabel = Str.login_button()
    var cookie by remember { mutableStateOf("") }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = hintText,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Button(onClick = { openLoginInBrowser(onStatus) }) {
            Text(openLabel)
        }
        TextField(
            value = cookie,
            onValueChange = { cookie = it },
            label = cookieLabel,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSession(cookie) },
            enabled = cookie.isNotBlank(),
        ) {
            Text(loginLabel)
        }
    }
}

private fun openLoginInBrowser(onError: (String) -> Unit) {
    try {
        if (!Desktop.isDesktopSupported()) throw UnsupportedOperationException("Desktop not supported")
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.BROWSE)) throw UnsupportedOperationException("Browse not supported")
        desktop.browse(URI(PIXIV_LOGIN_URL))
    } catch (e: Exception) {
        onError(e.message ?: "open failed")
    }
}
