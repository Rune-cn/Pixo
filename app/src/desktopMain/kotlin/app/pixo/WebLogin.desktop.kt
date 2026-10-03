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

/**
 * 桌面端实现：JVM 无内置 WebView，打开系统浏览器登录授权页，
 * 用户把跳转地址 pixiv://account/login?code=XXXX 里的 code 粘贴回来完成换 token。
 */
@Composable
internal actual fun PlatformWebLoginContent(
    modifier: Modifier,
    pkceChallenge: String,
    onStatus: (String) -> Unit,
    onAuthCode: (String) -> Unit,
) {
    val hintText = Str.web_login_browser_hint()
    val openLabel = Str.web_login_open_browser()
    var codeInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = hintText,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Button(onClick = { openUrlInBrowser(PixivAuth.pkceLoginUrl(pkceChallenge)) }) {
            Text(openLabel)
        }
        TextField(
            value = codeInput,
            onValueChange = { codeInput = it },
            label = Str.copy_title(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onAuthCode(codeInput) },
            enabled = codeInput.isNotBlank(),
        ) {
            Text(Str.login_button())
        }
    }
}
