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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
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

/** MIUIX 页面切换动效：对齐示例应用（InstallerX）的 miuix-nav MiuixDefault 过渡。 */
private const val MIUIX_NAV_MS = 500

/**
 * miuix-nav 程序化过渡曲线：把欠阻尼弹簧（response=0.8、damping=0.95）烘进固定时长 tween。
 * 中段敏捷（约 1/5 时长走到 48%）、收尾悠长，500ms 内精确收束，与示例应用逐点一致。
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
    slideInHorizontally(tween(MIUIX_NAV_MS, easing = miuixNavEasing)) { -it / 4 } +
        fadeIn(tween(MIUIX_NAV_MS, easing = miuixNavEasing), initialAlpha = 0.45f)

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

/** 顶栏毛玻璃：25dp 高斯模糊 + 80% surface 混色（对齐 InstallerX/legado 的玻璃拟态规范）。 */
@Composable
private fun Modifier.pixoTopBarBlur(backdrop: LayerBackdrop?): Modifier {
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
    content: @Composable (PaddingValues) -> Unit,
) {
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


/** iOS 式液态玻璃高光（对齐 InstallerX 的 iosIndicatorSpecular：上主光 + 下补光双峰 Bloom）。 */
private val iOSIndicatorSpecular: Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

/** 胶囊按压放大倍率：按下时从 56dp 撑到 78dp（示例应用 pressedScale）。 */
private const val PILL_PRESSED_SCALE = 78f / 56f

/** 标签内容按压放大倍率：对齐 InstallerX LocalFloatingBottomBarTabScale（1f → 1.2f）。 */
private const val TAB_PRESSED_SCALE = 1.2f

/** 纯函数插值（avoid internal lerp）。 */
private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

/**
 * 悬浮底栏（示例应用同款）：悬浮胶囊 + 滑动胶囊指示器 + 玻璃质感。
 * - 液态玻璃开：容器用 drawBackdrop 采样页面做 vibrancy + 4dp 模糊 + 24dp 折射透镜 + iOS 高光 + 40% 容器色罩；
 *   关闭则为纯色 surfaceContainer 胶囊。指示器按按压进度放大透镜 / 内阴影 / 高光，还原 InstallerX 质感。
 * - 按下/长按：胶囊指示器弹簧放大（1→78/56）、高光增强、包边增亮、内部压暗；按住不放即保持放大。
 * - 横向拖拽：胶囊指示器跟手滑动，松手弹簧吸附最近槽位并切换（由 onPageSelect 驱动 Pager）。
 * - 点击：直接切换到按下位置的标签。
 */
@Composable
fun PixoFloatingBar(
    backdrop: LayerBackdrop?,
    liquidGlass: Boolean,
    selectedIndex: Int,
    onPageSelect: (Int) -> Unit,
    onSizeChanged: (Dp) -> Unit,
) {
    val density = LocalDensity.current
    val scheme = MiuixTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f
    val scope = rememberCoroutineScope()
    val glassOn = liquidGlass && backdrop != null
    val containerShape = RoundedCornerShape(32.dp)
    val pillShape = RoundedCornerShape(28.dp)
    val indicatorPadding = with(density) { 4.dp.toPx() }
    val barMargin = 14.dp

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    val pillIndex = remember { Animatable(selectedIndex.toFloat().coerceIn(0f, 1f)) }
    val pressProgress = remember { Animatable(0f) }

    // 外部选中变化（路由/落定变化）→ 胶囊弹簧滑到对应槽位
    LaunchedEffect(selectedIndex) {
        val target = selectedIndex.toFloat().coerceIn(0f, 1f)
        if (pillIndex.value != target) {
            pillIndex.animateTo(target, spring(dampingRatio = 0.7f, stiffness = 420f))
        }
    }

    val pressIn = spring<Float>(dampingRatio = 1f, stiffness = 700f)
    val pressOut = spring<Float>(dampingRatio = 1f, stiffness = 400f)
    val sliderSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 420f)

    fun indexAt(x: Float): Int {
        if (tabWidthPx <= 0f) return 0
        return ((x - indicatorPadding) / tabWidthPx).toInt().coerceIn(0, 1)
    }


    val containerBackground = when {
        // 液态玻璃（InstallerX base 配方）：vibrancy 提饱和 + 4dp 模糊 + 24dp 折射透镜 + iOS 高光 + 40% 容器色罩
        glassOn -> Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { containerShape },
            effects = {
                vibrancy()
                blur(radiusX = 4.dp.toPx(), radiusY = 4.dp.toPx())
                lens(
                    refractionHeight = 24.dp.toPx(),
                    refractionAmount = 24.dp.toPx(),
                )
            },
            highlight = { iOSIndicatorSpecular.copy(alpha = 0.75f) },
            layerBlock = {
                val width = size.width.coerceAtLeast(1f)
                val s = lerpFloat(1f, 1f + 16.dp.toPx() / width, pressProgress.value)
                scaleX = s
                scaleY = s
            },
            onDrawSurface = { drawRect(scheme.surfaceContainer.copy(alpha = 0.4f)) },
        )
        liquidGlass -> Modifier.background(scheme.surface.copy(alpha = 0.85f), containerShape)
        // 仅悬浮底栏（无液态玻璃）：纯色胶囊
        else -> Modifier.background(scheme.surfaceContainer, containerShape)
    }

    val indicatorBackground = if (glassOn) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { pillShape },
            effects = {
                vibrancy()
                blur(radiusX = 4.dp.toPx(), radiusY = 4.dp.toPx())
                // 按压进度驱动透镜：深度折射 + 0.5 色散，像手指把玻璃压凹
                lens(
                    refractionHeight = 10.dp.toPx() * pressProgress.value,
                    refractionAmount = 14.dp.toPx() * pressProgress.value,
                    depthEffect = true,
                    chromaticAberration = 0.5f,
                )
            },
            // 按压/长按时高光随进度增强（InstallerX：pillHighlight.copy(alpha = pressProgress)）
            highlight = { iOSIndicatorSpecular.copy(alpha = pressProgress.value) },
            onDrawSurface = {
                val progress = pressProgress.value
                drawRect(
                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f),
                    alpha = 1f - progress,
                )
                drawRect(Color.Black.copy(alpha = 0.03f * progress))
            },
        )
    } else {
        Modifier
            .clip(pillShape)
            .background(
                if (liquidGlass) scheme.primary.copy(alpha = 0.2f)
                else scheme.primary.copy(alpha = 0.16f),
            )
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = barMargin)
                .onSizeChanged { size ->
                    onSizeChanged(with(density) { size.height.toDp() } + barMargin)
                }
                .shadow(
                    10.dp,
                    containerShape,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.24f else 0.1f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.24f else 0.1f),
                )
                .then(containerBackground)
                .pointerInput(Unit) {
                    // 统一手势：按下放大胶囊（长按保持放大）→ 横向拖拽跟手 → 抬起吸附/点击切换
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        scope.launch { pressProgress.animateTo(1f, pressIn) }
                        var dragTotal = 0f
                        var dragging = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                val tapped = !dragging
                                // 抬起立即切换：先切标签再并行播按压回弹，避免切换被回弹动画拖慢半拍
                                val target = if (tapped) indexAt(down.position.x) else pillIndex.value.roundToInt().coerceIn(0, 1)
                                onPageSelect(target)
                                if (!tapped && pillIndex.value != target.toFloat()) {
                                    // 拖拽抬起需要胶囊自己吸附回槽位（选中态没变时外部不会触发回位动画）
                                    scope.launch {
                                        pillIndex.animateTo(target.toFloat(), sliderSpring)
                                    }
                                }
                                scope.launch {
                                    pressProgress.animateTo(0f, pressOut)
                                }
                                break
                            }
                            val delta = change.positionChange()
                            if (delta.x != 0f || delta.y != 0f) {
                                change.consume()
                                dragTotal += delta.x
                                if (!dragging && abs(dragTotal) > viewConfiguration.touchSlop) {
                                    dragging = true
                                }
                                if (dragging && tabWidthPx > 0f) {
                                    scope.launch {
                                        pillIndex.snapTo((pillIndex.value + delta.x / tabWidthPx).coerceIn(0f, 1f))
                                    }
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            // 滑动胶囊指示器：宽度 = 单个标签槽宽，切换时弹簧滑动，按下时放大
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val s = lerpFloat(1f, PILL_PRESSED_SCALE, pressProgress.value)
                        scaleX = s
                        scaleY = s
                        translationX = pillIndex.value * tabWidthPx + indicatorPadding
                    }
                    .then(indicatorBackground)
                    .innerShadow(shape = pillShape) {
                        InnerShadow(
                            radius = 8.dp * pressProgress.value,
                            color = Color.Black.copy(alpha = 0.15f),
                            alpha = pressProgress.value,
                        )
                    }
                    .height(56.dp)
                    .width(with(density) { (tabWidthPx - indicatorPadding * 2f).toDp() }),
            )
            Row(
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .onSizeChanged { size ->
                        tabWidthPx = ((size.width - indicatorPadding * 2f) / 2f).coerceAtLeast(0f)
                    }
                    .height(64.dp)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixoFloatingBarItem(
                    selected = selectedIndex == 0,
                    icon = MiuixIcons.Home,
                    label = Str.nav_home(),
                    scale = lerpFloat(1f, TAB_PRESSED_SCALE, pressProgress.value),
                )
                PixoFloatingBarItem(
                    selected = selectedIndex == 1,
                    icon = MiuixIcons.Settings,
                    label = Str.nav_settings(),
                    scale = lerpFloat(1f, TAB_PRESSED_SCALE, pressProgress.value),
                )
            }
        }
    }
}
/**
 * 悬浮底栏单项：图标 + 文字，选中项由滑动胶囊指示器高亮。
 * [scale] 按压时 >1（对齐 InstallerX LocalFloatingBottomBarTabScale 1→1.2），
 * 让「里面的内容」跟着撑出来；图标本身保持 22dp 布局大小，由 graphicsLayer 放大。
 */
@Composable
private fun RowScope.PixoFloatingBarItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    scale: Float,
) {
    val scheme = MiuixTheme.colorScheme
    val contentColor = if (selected) scheme.primary else scheme.onSurfaceVariantSummary
    Column(
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minWidth = 88.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = contentColor,
        )
    }
}
