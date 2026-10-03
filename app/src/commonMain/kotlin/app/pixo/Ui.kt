package app.pixo

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/** 进入页：纯淡入（无位移动画，避免进入时页面整体上抬/波动的观感）。 */
fun miuixPushEnter(): EnterTransition = fadeIn(tween(220))

/** 被覆盖页：纯淡出。 */
fun miuixPushExit(): ExitTransition = fadeOut(tween(180))

/** 返回时下层页：淡入恢复（不位移）。 */
fun miuixPopEnter(): EnterTransition = slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 4 } + fadeIn(tween(220))

/** 返回时顶层页：向右滑出 + 淡出。 */
fun miuixPopExit(): ExitTransition =
    slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(250))

/** 底部标签切换：柔和淡入淡出。 */
fun miuixFadeEnter(): EnterTransition = fadeIn(tween(220))

fun miuixFadeExit(): ExitTransition = fadeOut(tween(180))

/**
 * 用户可读的错误描述。
 * 部分底层异常（如直连被墙时的 java.net.ConnectException）message 为 null，
 * 直接显示会变成笼统的"网络错误"，这里回退到异常类型名便于定位。
 */
internal fun Throwable.friendlyMessage(): String {
    val text = message?.trim().orEmpty()
    return text.ifEmpty { this::class.simpleName ?: "网络错误" }
}

/**
 * 全局子页面框架：统一的小标题栏 + 返回按钮。
 */
@Composable
fun PixoSubScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        content(innerPadding)
    }
}

/**
 * 底部标签页框架：给首页/账号这类宿主页单独加一个顶栏（无返回键），
 * 使每个页面自带状态栏内边距，外层 NavHost 不必随路由切换内边距，
 * 从而消除进入/返回时的整页上跳或下沉。
 */
@Composable
fun PixoTabFrame(
    title: String,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(title = title)
        },
    ) { innerPadding ->
        content(innerPadding)
    }
}

/** 全局加载状态。 */
@Composable
fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InfiniteProgressIndicator()
    }
}

/** 全局错误状态（带重试）。 */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = Str.load_failed(message))
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(text = Str.retry())
        }
    }
}
