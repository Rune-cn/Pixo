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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer

/** MIUIX 页面切换动效：对齐 miuix-nav 的 MiuixDefault 过渡。 */
private const val MIUIX_NAV_MS = 500

/**
 * miuix-nav 程序化过渡曲线：把欠阻尼弹簧（response=0.8、damping=0.95）烘进固定时长 tween。
 * 中段敏捷（约 1/5 时长走到 48%）、收尾悠长，500ms 内精确收束。
 */
private class NavProgrammaticEasing(
    response: Float,
    damping: Float,
) : Easing {
    private val r: Float
    private val w: Float
    private val c2: Float

    init {
        val omega = 2.0 * PI / response
        val k = omega * omega
        val c = damping * 4.0 * PI / response
        w = (sqrt(4.0 * k - c * c) / 2.0).toFloat()
        r = (-c / 2.0).toFloat()
        c2 = r / w
    }

    override fun transform(fraction: Float): Float {
        val t = fraction.toDouble()
        val decay = exp(r * t)
        return (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
    }
}

private val miuixNavEasing = NavProgrammaticEasing(response = 0.8f, damping = 0.95f)

/** 进入页：从右侧全宽滑入，无淡入（对应 miuix-nav 顶层 d<=0 段）。 */
fun miuixPushEnter(): EnterTransition =
    slideInHorizontally(tween(MIUIX_NAV_MS, easing = miuixNavEasing)) { it }

/** 被覆盖页：向左侧 1/4 宽度视差 + 透明度降至 0.45（0.9 透明度回退 × 50% 暗幕的合成视觉）。 */
fun miuixPushExit(): ExitTransition =
    slideOutHorizontally(tween(MIUIX_NAV_MS, easing = miuixNavEasing)) { -it / 4 } +
        fadeOut(tween(MIUIX_NAV_MS, easing = miuixNavEasing), targetAlpha = 0.45f)

/** 返回时被覆盖页：从 -1/4 视差 + 0.45 透明度复位。 */
fun miuixPopEnter(): EnterTransition =
    // 返回时底层页全宽从右侧滑入：右滑返回时能清晰预览「上一层页面」内容
    slideInHorizontally(tween(MIUIX_NAV_MS, easing = miuixNavEasing)) { -it } +
        fadeIn(tween(MIUIX_NAV_MS, easing = miuixNavEasing), initialAlpha = 0.3f)

/** 返回时顶层页：全宽滑向右侧退出，无淡出。 */
fun miuixPopExit(): ExitTransition =
    slideOutHorizontally(tween(MIUIX_NAV_MS, easing = miuixNavEasing)) { it }

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
 * 毛玻璃背景源：捕获页面内容给顶栏做实时模糊。
 * 无渲染能力（低版本 Android / 软件渲染桌面）时返回 null，调用方回退到纯色顶栏。
 */
@Composable
internal fun rememberPixoBlurBackdrop(): LayerBackdrop? {
    if (!isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/** 顶栏毛玻璃：25dp 高斯模糊 + 80% surface 混色。 */
@Composable
internal fun Modifier.pixoTopBarBlur(backdrop: LayerBackdrop?): Modifier {
    if (backdrop == null) return this
    val blendColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.8f)
    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = 25f,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = blendColor),
                ),
            ),
        ),
    )
}

/** 页面顶层背景同步顶栏模糊（需要透明顶栏时），否则用主题 surface。 */
@Composable
private fun pixoTopBarColor(backdrop: LayerBackdrop?): Color =
    if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

/**
 * 全局子页面框架：统一的小标题栏 + 返回按钮。
 */

@Composable
fun PixoSubScreen(
    title: String,
    onBack: () -> Unit,
    scrollBehavior: ScrollBehavior? = null,
    blur: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    SwipeBackContainer(onBack = onBack) {
    // 内嵌 WebView 等平台视图页面关掉 blur：毛玻璃层反复采样会把 WebView 拉进图形层导致界面抖动
    val backdrop = if (blur) rememberPixoBlurBackdrop() else null
    Scaffold(
        topBar = {
            if (scrollBehavior != null) {
                TopAppBar(
                    title = title,
                    largeTitle = title,
                    scrollBehavior = scrollBehavior,
                    color = pixoTopBarColor(backdrop),
                    modifier = Modifier.pixoTopBarBlur(backdrop),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                        }
                    },
                    actions = actions,
                )
            } else {
                SmallTopAppBar(
                    title = title,
                    color = pixoTopBarColor(backdrop),
                    modifier = Modifier.pixoTopBarBlur(backdrop),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                        }
                    },
                    actions = actions,
                )
            }
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            content(innerPadding)
        }
    }
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
    scrollBehavior: ScrollBehavior? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberPixoBlurBackdrop()
    Scaffold(
        topBar = {
            if (scrollBehavior != null) {
                TopAppBar(
                    title = title,
                    largeTitle = title,
                    scrollBehavior = scrollBehavior,
                    color = pixoTopBarColor(backdrop),
                    modifier = Modifier.pixoTopBarBlur(backdrop),
                )
            } else {
                SmallTopAppBar(
                    title = title,
                    color = pixoTopBarColor(backdrop),
                    modifier = Modifier.pixoTopBarBlur(backdrop),
                )
            }
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            content(innerPadding)
        }
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



/**
 * 全屏右滑返回容器（miuix 0.9.4 未提供滑退组件，用基础 Compose 手势实现，KMP 通用）：
 * 内容随手指右移跟手；松手时超过屏幕 35% 或快速右滑（>1500px/s）→ 滑出并触发 onBack，否则回弹。
 * 属于"额外行为"包裹层，不替换任何 miuix 组件；与 App 级 NavigationBackHandler 返回规则联动。
 */
@Composable
fun SwipeBackContainer(
    onBack: () -> Unit,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) }
    var screenW by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screenW = it.width.toFloat() }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var drag = 0f
                    var consumed = false
                    val velocityTracker = VelocityTracker()
                    drag(down.id) { change ->
                        val dx = change.position.x - down.position.x
                        if (dx > 0f && !consumed) {
                            drag = dx
                            offsetX = drag
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                        }
                    }
                    val velocity = velocityTracker.calculateVelocity().x
                    val passed = drag > screenW * 0.35f || velocity > 1500f
                    scope.launch {
                        if (passed) {
                            // 立即触发导航返回：NavHost 的 pop 过渡会从右滑入上一页（真预览）
                            // 同时本容器把当前页继续右滑出，两者叠加视觉连续
                            onBack()
                            animate(
                                initialValue = offsetX,
                                targetValue = screenW,
                                animationSpec = tween(200, easing = FastOutSlowInEasing),
                            ) { v, _ -> offsetX = v }
                        } else {
                            animate(
                                initialValue = offsetX,
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                            ) { v, _ -> offsetX = v }
                        }
                    }
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .graphicsLayer { shadowElevation = 8f * (offsetX / screenW).coerceIn(0f, 1f) },
        ) {
            content()
        }
    }
}

