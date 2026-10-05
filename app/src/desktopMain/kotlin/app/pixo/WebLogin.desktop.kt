package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
 * 桌面端实现：JVM 无内置 WebView，打开系统浏览器登录授权页。
 * 用户把跳转地址 pixiv://account/login?code=XXXX 里的 code 粘回换 token；
 * 之后可在同页粘贴网页会话 Cookie，解锁评论/收藏/推荐等网页读接口。
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
    val cookieHint = Str.cookie_paste_hint()
    val saveLabel = Str.cookie_save()
    val savedLabel = Str.cookie_saved()
    var codeInput by remember { mutableStateOf("") }
    var cookieInput by remember { mutableStateOf(AppPrefs.cookie) }
    var cookieStatus by remember { mutableStateOf("") }
    var cookieJustSaved by remember { mutableLongStateOf(0L) }

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

        Text(
            text = cookieHint,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        TextField(
            value = cookieInput,
            onValueChange = { cookieInput = it },
            label = Str.cookie_label(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                AppPrefs.cookie = cookieInput.trim()
                AppPrefs.selfUserId = PixivHttp.selfUserId().takeIf { it > 0L } ?: AppPrefs.selfUserId
                PixivHttp.refreshClient()
                cookieJustSaved = System.currentTimeMillis()
            },
            enabled = cookieInput.trim().isNotBlank(),
        ) {
            Text(saveLabel)
        }
        if (cookieJustSaved > 0L) {
            Text(
                text = savedLabel,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}


