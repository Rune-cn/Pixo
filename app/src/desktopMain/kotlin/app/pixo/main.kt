package app.pixo

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    // 必须在创建任何网络连接之前生效，否则 Java 以直连方式请求已被墙的 pixiv.net
    System.setProperty("java.net.useSystemProxies", "true")
    initCrashLogging()
    AppPrefs.init()
    initImageLoaderOnce()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Pixo",
            state = rememberWindowState(width = 900.dp, height = 720.dp),
        ) {
            CompositionLocalProvider(LocalPixoWindow provides window) {
                App()
            }
        }
    }
}
