package app.pixo

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLDecoder

/**
 * Android 实现：内嵌 WebView 加载 PKCE 授权页。
 * 登录成功后跳转 pixiv://account/login?code=XXXX，在 shouldOverrideUrlLoading 里拦截并提取 code。
 */
@Composable
internal actual fun PlatformWebLoginContent(
    modifier: Modifier,
    pkceChallenge: String,
    onStatus: (String) -> Unit,
    onAuthCode: (String) -> Unit,
) {
    val currentOnAuthCode by rememberUpdatedState(onAuthCode)

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            @SuppressLint("SetJavaScriptEnabled")
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.userAgentString = PixivHttp.USER_AGENT
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val url = request.url?.toString().orEmpty()
                        if (url.startsWith("pixiv://account/login")) {
                            val code = extractCode(url)
                            if (!code.isNullOrBlank()) currentOnAuthCode(code)
                            return true
                        }
                        return false
                    }
                }
                loadUrl(PixivAuth.pkceLoginUrl(pkceChallenge))
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
    )
}

private fun extractCode(url: String): String? {
    val idx = url.indexOf("code=")
    if (idx < 0) return null
    var raw = url.substring(idx + 5)
    val amp = raw.indexOf('&')
    if (amp >= 0) raw = raw.substring(0, amp)
    return try {
        URLDecoder.decode(raw, "UTF-8")
    } catch (_: Exception) {
        raw
    }
}
