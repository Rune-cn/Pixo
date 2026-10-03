package app.pixo

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Android 实现：内嵌 WebView 直接加载 Pixiv 登录页。
 *
 * 每次页面跳转都读一次 Cookie，出现 PHPSESSID 即视为登录成功。
 * 登录页在 accounts.pixiv.net，会话可能只对该子域生效，所以两个域的 Cookie 要合并后再判断。
 * 登录页可能弹出新窗口，[WebChromeClient.onCreateWindow] 把新窗口接回当前 WebView，避免页面丢失。
 */
@Composable
internal actual fun PlatformWebLoginContent(
    modifier: Modifier,
    onStatus: (String) -> Unit,
    onSession: (String) -> Unit,
) {
    val currentOnSession by rememberUpdatedState(onSession)
    val currentOnStatus by rememberUpdatedState(onStatus)

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            fun capture() {
                val raw = mergedPixivCookies(CookieManager.getInstance())
                if (raw.contains("PHPSESSID=")) {
                    currentOnSession(raw)
                }
            }

            @SuppressLint("SetJavaScriptEnabled")
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.userAgentString = PixivHttp.USER_AGENT
                settings.setSupportMultipleWindows(true)
                val cookies = CookieManager.getInstance()
                cookies.setAcceptCookie(true)
                cookies.setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean = false

                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) = capture()

                    override fun onPageFinished(view: WebView, url: String) = capture()

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        if (request.isForMainFrame) {
                            currentOnStatus(error.description.toString())
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message,
                    ): Boolean {
                        (resultMsg.obj as? WebView.WebViewTransport)?.webView = view
                        return true
                    }
                }

                loadUrl(PIXIV_LOGIN_URL)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
    )
}

/**
 * 合并登录页与 www 域的 Cookie。
 *
 * `PHPSESSID` 若只对 accounts.pixiv.net 下发，单独读 www 域会取不到，
 * 这里按「同名时 www 优先」合并，保证拿到完整会话。
 */
private fun mergedPixivCookies(manager: CookieManager): String {
    val www = manager.getCookie("https://www.pixiv.net").orEmpty()
    val accounts = manager.getCookie("https://accounts.pixiv.net").orEmpty()
    if (www.isBlank()) return accounts
    if (accounts.isBlank()) return www
    val merged = LinkedHashMap<String, String>()
    for (pair in "$accounts;$www".split(';')) {
        val index = pair.indexOf('=')
        if (index <= 0) continue
        val name = pair.substring(0, index).trim()
        if (name.isNotEmpty()) merged[name] = pair.substring(index + 1).trim()
    }
    return merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
}
