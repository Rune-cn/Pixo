package app.pixo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.TimeSource
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 悬浮底栏：液态玻璃材质 + 滑动胶囊指示器 + 三层结构（基础层 / 激活层 / 指示器层）。
//
// KMP 适配：高光用 drawWithContent + 径向渐变；帧等待用 withFrameNanos；手势用 awaitEachGesture；
// 时间源用 kotlin.time。玻璃材质复用本工程 LiquidGlass.kt（vibrancy / lens / InnerShadow / rememberCombinedBackdrop）。


/** 底栏渲染模式。 */
enum class FloatingBottomBarMode {
    /** 液态玻璃：vibrancy + 模糊 + 折射透镜 + iOS 高光 + 选中层叠（指示器折射选中内容）。 */
    LiquidGlass,

    /** 纯毛玻璃：仅模糊 + 容器色罩，无折射透镜。 */
    Blur,

    /** 无玻璃：纯色胶囊 + 半透明指示器。 */
    None,
}

/** 底栏配色。 */
@Immutable
data class FloatingBottomBarColors(
    val containerColor: Color,
    val indicatorColor: Color,
    val contentColor: Color,
    val activeContentColor: Color,
)

object FloatingBottomBarDefaults {
    @Composable
    fun colors(
        containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
        indicatorColor: Color = MiuixTheme.colorScheme.primary,
        contentColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        activeContentColor: Color = indicatorColor,
    ): FloatingBottomBarColors = FloatingBottomBarColors(
        containerColor = containerColor,
        indicatorColor = indicatorColor,
        contentColor = contentColor,
        activeContentColor = activeContentColor,
    )
}

/** 底栏单项：图标 + 文案。 */
@Immutable
data class FloatingBottomBarItem(
    val icon: ImageVector,
    val label: String,
)

/** iOS 式液态玻璃高光（上主光 + 下补光双峰 Bloom）。 */
private val iosIndicatorSpecular: Highlight = Highlight(
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

/** 胶囊按压放大倍率（78/56≈1.393）。 */
private const val PILL_PRESSED_SCALE = 1.393f

/** 容器按压放大附加量：整层（玻璃+阴影）一起放大。 */
private const val CONTAINER_PRESS_SCALE_DP = 16f

/** 标签内容按压放大倍率（1f → 1.2f）。 */
private const val TAB_PRESSED_SCALE = 1.2f

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

/**
 * 悬浮底栏：
 * - 三层结构：基础层（全宽内容）→ 激活层（透明、采样进 tabsBackdrop，渲染选中色与放大的内容）→
 *   指示器层（combinedBackdrop = 页面 + 激活层，故玻璃里折射出选中内容）。
 * - 按压：胶囊弹簧放大（撑出容器）、高光增强、内阴影压暗；速度挤压（落定时的惯性形变）。
 * - 按下：胶囊弹簧「瞬移」到手指正下方（连续位置，不再只是原地放大），随后跟手拖拽，
 *   松手吸附最近槽位并切换（对齐 InstallerX-Revived 的按下即抓取）。
 * - 横向拖拽：胶囊跟手滑动，松手弹簧吸附最近槽位并切换；点击：直接切换到按下槽位。
 * - RTL：isLtr 镜像槽位坐标。
 * - 无障碍：每个标签 Role.Tab + selected + onClick 语义，回车/空格键激活。
 *
 * [backdrop] 为空时不支持渲染，自动降级为 None 模式纯色胶囊。
 */
@Composable
fun FloatingBottomBar(
    items: List<FloatingBottomBarItem>,
    selectedIndex: Int,
    onSelected: (index: Int) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    mode: FloatingBottomBarMode = FloatingBottomBarMode.LiquidGlass,
    colors: FloatingBottomBarColors = FloatingBottomBarDefaults.colors(),
    onSizeChanged: (heightDp: Dp) -> Unit = {},
) {
    val scheme = MiuixTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val scope = rememberCoroutineScope()

    val containerShape = RoundedCornerShape(32.dp)
    val pillShape = RoundedCornerShape(28.dp)
    val indicatorPaddingPx = with(density) { 4.dp.toPx() }
    val tabsCount = items.size.coerceAtLeast(1)

    val effectiveMode = if (backdrop == null) FloatingBottomBarMode.None else mode
    val isLiquidGlass = effectiveMode == FloatingBottomBarMode.LiquidGlass
    val isBlur = effectiveMode == FloatingBottomBarMode.Blur
    val containerColor =
        if (isLiquidGlass) colors.containerColor.copy(alpha = 0.4f) else colors.containerColor

    // 激活层 backdrop：捕获选中态内容供指示器 combinedBackdrop 采样
    val tabsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = if (backdrop != null) rememberCombinedBackdrop(backdrop, tabsBackdrop) else null

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    val pillWidthPx = (tabWidthPx - indicatorPaddingPx * 2f).coerceAtLeast(0f)

    val pillIndex = remember { Animatable(selectedIndex.toFloat()) }
    val pressProgress = remember { Animatable(0f) }
    val indicatorVelocity = remember { mutableFloatStateOf(0f) }
    val velocityTracker = remember { VelocityTracker() }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onSelectedUpdated by rememberUpdatedState(onSelected)

    fun pressScale(): Float = lerpFloat(1f, TAB_PRESSED_SCALE, pressProgress.value)

    // 落定时跟踪胶囊动画速度 → 指示器速度挤压形变
    suspend fun trackVelocity(target: Float) {
        velocityTracker.resetTracking()
        val mark = TimeSource.Monotonic.markNow()
        pillIndex.animateTo(target, spring(dampingRatio = 0.8f, stiffness = 700f)) {
            velocityTracker.addPosition(
                mark.elapsedNow().inWholeMilliseconds,
                Offset(value * tabWidthPx, 0f),
            )
            indicatorVelocity.value = velocityTracker.calculateVelocity().x / tabWidthPx
        }
        indicatorVelocity.value = 0f
    }

    // 外部选中变化（路由/落定/手动滑动）→ 同步选中态并弹簧滑到对应槽位（含速度跟踪）
    LaunchedEffect(selectedIndex) {
        currentIndex = selectedIndex
        val target = selectedIndex.toFloat().coerceIn(0f, (tabsCount - 1).toFloat())
        if (pillIndex.value != target) {
            trackVelocity(target)
        }
    }

    val pressIn = spring<Float>(dampingRatio = 1f, stiffness = 700f)
    val pressOut = spring<Float>(dampingRatio = 1f, stiffness = 400f)

    fun indexAt(x: Float): Int {
        if (tabWidthPx <= 0f) return currentIndex
        val logicalX = if (isLtr) x else totalWidthPx - x
        return ((logicalX - indicatorPaddingPx) / tabWidthPx).toInt().coerceIn(0, tabsCount - 1)
    }

    /**
     * 手指横坐标 → 胶囊的连续位置（0f..tabsCount-1f）。
     *
     * 胶囊中心落在 `(index + 0.5) * tabWidth + padding`，反解得到连续 index。
     * 长按「瞬移」用连续值，胶囊才会正好落在手指下方；若用 [indexAt] 取整，
     * 只有两个标签时胶囊会直接跳到某一侧槽位中心，看着像「跳档」而不是跟手。
     */
    fun positionAt(x: Float): Float {
        if (tabWidthPx <= 0f) return currentIndex.toFloat()
        val logicalX = if (isLtr) x else totalWidthPx - x
        return ((logicalX - indicatorPaddingPx) / tabWidthPx - 0.5f)
            .coerceIn(0f, (tabsCount - 1).toFloat())
    }

    fun activate(index: Int) {
        if (currentIndex != index) {
            currentIndex = index
            onSelectedUpdated(index)
        }
    }

    // 基础层内容：液态玻璃=全未选中色（激活层补选中色）；Blur/None=选中感知色
    val baseTabs: @Composable RowScope.() -> Unit = {
        items.forEachIndexed { index, item ->
            val (color, scale) = if (isLiquidGlass) {
                colors.contentColor to 1f
            } else {
                (if (index == currentIndex) colors.activeContentColor else colors.contentColor) to
                    (if (index == currentIndex) pressScale() else 1f)
            }
            PixoNavTab(
                item = item,
                selected = index == currentIndex,
                color = color,
                scale = scale,
                onActivate = { activate(index) },
            )
        }
    }

    // 激活层内容（液态玻璃专属）：全选中色 + 按压放大，采样进 tabsBackdrop
    val activeTabs: @Composable RowScope.() -> Unit = {
        items.forEachIndexed { index, item ->
            PixoNavTab(
                item = item,
                selected = index == currentIndex,
                color = colors.activeContentColor,
                scale = pressScale(),
                onActivate = { activate(index) },
            )
        }
    }

    var glowCenterX by remember { mutableFloatStateOf(0f) }

    val containerBackground = when {
        isLiquidGlass && backdrop != null -> Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { containerShape },
            effects = {
                vibrancy()
                blur(radiusX = 4.dp.toPx(), radiusY = 4.dp.toPx())
                lens(refractionHeight = 24.dp.toPx(), refractionAmount = 24.dp.toPx())
            },
            highlight = { iosIndicatorSpecular.copy(alpha = 0.75f) },
            onDrawSurface = { drawRect(containerColor) },
        )

        isBlur && backdrop != null -> Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { containerShape },
            effects = { blur(radiusX = 25.dp.toPx(), radiusY = 25.dp.toPx()) },
            onDrawSurface = { drawRect(colors.containerColor.copy(alpha = 0.65f)) },
        )

        else -> Modifier.clip(containerShape).background(colors.containerColor, containerShape)
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 14.dp)
                .fillMaxWidth()
                .onSizeChanged { onSizeChanged(with(density) { it.height.toDp() } + 14.dp) },
            contentAlignment = Alignment.CenterStart,
        ) {
            // 1) 基础层：全宽内容 + 玻璃/纯色背景 + 手势
            Row(
                modifier = Modifier
                    .onSizeChanged {
                        totalWidthPx = it.width.toFloat()
                        tabWidthPx = ((totalWidthPx - indicatorPaddingPx * 2f) / tabsCount).coerceAtLeast(0f)
                    }
                    .graphicsLayer {
                        val w = size.width.coerceAtLeast(1f)
                        val s = lerpFloat(1f, 1f + CONTAINER_PRESS_SCALE_DP.dp.toPx() / w, pressProgress.value)
                        scaleX = s
                        scaleY = s
                    }
                    .shadow(
                        10.dp,
                        containerShape,
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.2f else 0.1f),
                        spotColor = Color.Black.copy(alpha = if (isDark) 0.2f else 0.1f),
                    )
                    .then(containerBackground)
                    .then(
                        Modifier.pressGlow(
                            progress = { pressProgress.value },
                            glowCenter = { Offset(glowCenterX, size.height / 2f) },
                        ),
                    )
                    .pointerInput(tabsCount) {
                        // 手势对齐 InstallerX-Revived（inspectDragGestures + DampedDragAnimation）：
                        // 1) 按下（onDragStart）立刻把胶囊弹簧「瞬移」到手指正下方，而不是原地放大；
                        // 2) 拖动按位移跟手（onDrag）；
                        // 3) 抬起吸附最近槽位并切换（onDragEnd → targetValue.round）。
                        // 注意：那边是「按下即抓取」，不等长按超时；等长按会被
                        // PointerEventTimeoutCancellationException 打断，实测表现为「还是原地放大」。
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            scope.launch { pressProgress.animateTo(1f, pressIn) }
                            glowCenterX = down.position.x
                            // 胶囊瞬移到手指正下方（连续位置 + 弹簧过渡）
                            scope.launch { trackVelocity(positionAt(down.position.x)) }

                            var dragTotal = 0f
                            var dragging = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) {
                                    // 指针意外消失：按抬起处理，避免按压态卡住
                                    scope.launch { trackVelocity(currentIndex.toFloat()) }
                                    scope.launch { pressProgress.animateTo(0f, pressOut) }
                                    break
                                }
                                if (!change.pressed) {
                                    val target = pillIndex.value.roundToInt().coerceIn(0, tabsCount - 1)
                                    if (currentIndex != target) {
                                        activate(target)
                                    } else {
                                        // 同档位：外部选中态不变，仍需把胶囊吸附回槽位中心
                                        scope.launch { trackVelocity(target.toFloat()) }
                                    }
                                    scope.launch { pressProgress.animateTo(0f, pressOut) }
                                    break
                                }
                                val delta = change.positionChange()
                                if (delta.x != 0f || delta.y != 0f) {
                                    change.consume()
                                    dragTotal += delta.x
                                    if (!dragging && abs(dragTotal) > viewConfiguration.touchSlop) dragging = true
                                    if (dragging && tabWidthPx > 0f) {
                                        glowCenterX = change.position.x
                                        val next = (pillIndex.value + delta.x / tabWidthPx * if (isLtr) 1f else -1f)
                                            .coerceIn(0f, (tabsCount - 1).toFloat())
                                        scope.launch { pillIndex.snapTo(next) }
                                    }
                                }
                            }
                        }
                    }
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = baseTabs,
            )

            // 2) 激活层（液态玻璃专属）：透明、采样进 tabsBackdrop，渲染选中色 + 放大的全宽内容；
            //    指示器 combinedBackdrop = backdrop + tabsBackdrop，故玻璃里折射出选中内容。
            if (isLiquidGlass && backdrop != null) {
                Row(
                    modifier = Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { containerShape },
                            effects = {
                                vibrancy()
                                blur(radiusX = 4.dp.toPx(), radiusY = 4.dp.toPx())
                                lens(refractionHeight = 24.dp.toPx(), refractionAmount = 24.dp.toPx())
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = activeTabs,
                )
            }

            // 3) 指示器层
            if (tabWidthPx > 0f) {
                if (isLiquidGlass && combinedBackdrop != null) {
                    Box(
                        Modifier
                            .graphicsLayer {
                                val ps = pressProgress.value
                                val s = lerpFloat(1f, PILL_PRESSED_SCALE, ps)
                                val v = indicatorVelocity.value / 10f
                                scaleX = s / (1f - (v * 0.75f).coerceIn(-0.2f, 0.2f))
                                scaleY = s * (1f - (v * 0.25f).coerceIn(-0.2f, 0.2f))
                                val progress = pillIndex.value * tabWidthPx
                                translationX = (if (isLtr) progress else -progress) +
                                    indicatorPaddingPx + (tabWidthPx - pillWidthPx) / 2f
                            }
                            .drawBackdrop(
                                backdrop = combinedBackdrop,
                                shape = { pillShape },
                                effects = {
                                    val ps = pressProgress.value
                                    lens(
                                        refractionHeight = 10.dp.toPx() * ps,
                                        refractionAmount = 14.dp.toPx() * ps,
                                        depthEffect = true,
                                        chromaticAberration = 0.5f,
                                    )
                                },
                                highlight = { iosIndicatorSpecular.copy(alpha = pressProgress.value) },
                                onDrawSurface = {
                                    val ps = pressProgress.value
                                    drawRect(
                                        color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f),
                                        alpha = 1f - ps,
                                    )
                                    drawRect(Color.Black.copy(alpha = 0.03f * ps))
                                },
                            )
                            .innerShadow(shape = pillShape) {
                                InnerShadow(
                                    radius = 8.dp * pressProgress.value,
                                    color = Color.Black.copy(alpha = 0.15f),
                                    alpha = pressProgress.value,
                                )
                            }
                            .height(56.dp)
                            .width(with(density) { pillWidthPx.toDp() }),
                    )
                } else {
                    // Blur / None：半透明指示器胶囊（选中内容已由基础层选中感知色呈现）
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val ps = pressProgress.value
                                val s = lerpFloat(1f, PILL_PRESSED_SCALE, ps)
                                scaleX = s
                                scaleY = s
                                val progress = pillIndex.value * tabWidthPx
                                translationX = (if (isLtr) progress else -progress) + indicatorPaddingPx
                            }
                            .clip(pillShape)
                            .background(colors.indicatorColor.copy(alpha = 0.15f), pillShape)
                            .height(56.dp)
                            .width(with(density) { pillWidthPx.toDp() }),
                    )
                }
            }
        }
    }
}

/** 底栏单项：图标 + 文案，带无障碍语义与键盘激活。颜色/缩放由调用方传入。 */
@Composable
private fun RowScope.PixoNavTab(
    item: FloatingBottomBarItem,
    selected: Boolean,
    color: Color,
    scale: Float,
    onActivate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 76.dp)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.Tab
                onClick { onActivate(); true }
            }
            .onKeyEvent { event ->
                val isActivation = event.key == Key.Enter ||
                    event.key == Key.NumPadEnter ||
                    event.key == Key.Spacebar
                if (isActivation && event.type == KeyEventType.KeyUp) onActivate()
                isActivation
            }
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = item.label,
            fontSize = 11.sp,
            color = color,
        )
    }
}

/** 按压高光：白晕 + 径向渐变，跟随手指。 */
private fun Modifier.pressGlow(
    progress: () -> Float,
    glowCenter: DrawScope.() -> Offset,
): Modifier = drawWithContent {
    val p = progress()
    if (p > 0f) {
        drawRect(Color.White.copy(alpha = 0.06f * p), blendMode = BlendMode.Plus)
        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f * p),
                    Color.White.copy(alpha = 0f),
                ),
                center = glowCenter(),
                radius = size.minDimension * 1.2f,
            ),
            blendMode = BlendMode.Plus,
        )
    }
    drawContent()
}
