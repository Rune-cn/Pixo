package app.pixo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 网页/PKCE 登录页。
 *
 * 流程：进入页面时生成 PKCE 验证器；平台加载授权页（[PixivAuth.pkceLoginUrl]）。
 * 用户登录后跳转 pixiv://account/login?code=XXXX：
 *  - Android 内嵌 WebView 拦截该跳转并把 code 交给 [PlatformWebLoginContent.onAuthCode]；
 *  - 桌面端打开系统浏览器，由用户把跳转地址里的 code 粘贴回输入框。
 * 公共层用 code + verifier 调 [PixivAuth.exchangeAuthCode] 换取 Refresh Token，
 * 之后走 App API（Token）通道，收藏/推荐/作者全量作品等都更可靠。
 */
@Composable
internal fun WebLoginScreen(onBack: () -> Unit) {
    val pkce = remember { PixivAuth.pkceChallenge() }
    val successText = Str.web_login_success()
    val hintText = Str.web_login_hint()
    var status by remember { mutableStateOf(hintText) }
    var signedIn by remember { mutableStateOf(false) }
    var exchanging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    PixoSubScreen(title = Str.web_login(), onBack = onBack, blur = false) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Text(
                text = status,
                style = MiuixTheme.textStyles.footnote2,
                color = if (signedIn) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(4.dp))
            PlatformWebLoginContent(
                modifier = Modifier.fillMaxWidth().weight(1f),
                pkceChallenge = pkce.second,
                onStatus = { message -> status = message },
                onAuthCode = { code ->
                    val trimmed = code.trim()
                    if (!signedIn && !exchanging && trimmed.isNotBlank()) {
                        exchanging = true
                        scope.launch {
                            val err = PixivAuth.exchangeAuthCode(trimmed, pkce.first)
                            exchanging = false
                            if (err == null) {
                                signedIn = true
                                status = successText
                            } else {
                                status = err
                            }
                        }
                    }
                },
            )
        }
    }
}

/**
 * 平台相关的登录页主体。
 *
 * @param pkceChallenge 本次 PKCE 的 code_challenge，用于拼授权 URL
 * @param onAuthCode 从授权跳转中捕获的授权码
 */
@Composable
internal expect fun PlatformWebLoginContent(
    modifier: Modifier,
    pkceChallenge: String,
    onStatus: (String) -> Unit,
    onAuthCode: (String) -> Unit,
)