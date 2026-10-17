package app.pixo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 关于页：沉浸式滚动顶栏 + 背景光效 + 浮动 Logo + 卡片列表。 */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    var consoleDoc by remember { mutableStateOf<Pair<String, String>?>(null) }
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current

    // 滚动进度：以透明 logoSpacer 的实际高度为分母
    val scrollProgress by remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer =
                        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size).coerceIn(0f, 1f)
                    } else 0f
                }
            }
        }
    }

    val backdrop = rememberPixoBlurBackdrop()
    val blurActive = backdrop != null && scrollProgress == 1f

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = Str.about(),
                scrollBehavior = topAppBarScrollBehavior,
                // 未滚完：透明顶栏让背景光效透出来；滚到底：有模糊就全透明玻璃，否则纯色
                color = if (blurActive) Color.Transparent
                else if (scrollProgress == 1f) MiuixTheme.colorScheme.surface
                else Color.Transparent,
                // 标题随滚动淡入（0.35~1.0 区间）
                titleColor = MiuixTheme.colorScheme.onSurface.copy(
                    alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
                ),
                modifier = if (blurActive) Modifier.pixoTopBarBlur(backdrop) else Modifier,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = Str.action_back())
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            AboutContent(
                padding = innerPadding,
                topAppBarScrollBehavior = topAppBarScrollBehavior,
                lazyListState = lazyListState,
                scrollProgress = scrollProgress,
                onOpenDoc = { title, content -> consoleDoc = title to content },
            )
        }
    }
    // 文档弹窗：Miuix 原生 WindowBottomSheet（从下往上滑出）渲染 Markdown
    consoleDoc?.let { (title, content) ->
        DocSheet(
            title = title,
            content = content,
            onDismiss = { consoleDoc = null },
        )
    }
}


@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun AboutContent(
    padding: PaddingValues,
    topAppBarScrollBehavior: ScrollBehavior,
    lazyListState: LazyListState,
    scrollProgress: Float,
    onOpenDoc: (String, String) -> Unit,
) {
    val density = LocalDensity.current
    val sPrivacy = Str.privacy_policy()
    val sPrivacyContent = Str.privacy_policy_content()
    val sLicense = Str.license()
    val sLicenseContent = Str.license_content()
    val sDisclaimer = Str.disclaimer()
    val sDisclaimerContent = Str.disclaimer_content()

    val backdrop = rememberPixoBlurBackdrop()
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f

    // 玻璃卡片配色（亮/暗两套）
    val cardBlend = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0x4DA9A9A9), BlurBlendMode.Luminosity),
                BlendColorEntry(Color(0x1A9C9C9C), BlurBlendMode.PlusDarker),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0x340034F9), BlurBlendMode.Overlay),
                BlendColorEntry(Color(0xB3FFFFFF), BlurBlendMode.HardLight),
            )
        }
    }
    val glassCard: Modifier = if (backdrop != null) {
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = RoundedCornerShape(16.dp),
            blurRadius = 60f,
            colors = BlurColors(blendColors = cardBlend),
        )
    } else {
        Modifier
    }
    val cardColors = CardDefaults.defaultColors(
        if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
        Color.Transparent,
    )

    val scrollPadding = PaddingValues(
        top = padding.calculateTopPadding(),
        start = padding.calculateLeftPadding(LayoutDirection.Ltr) + 16.dp,
        end = padding.calculateRightPadding(LayoutDirection.Ltr) + 16.dp,
    )
    val logoPadding = PaddingValues(
        top = padding.calculateTopPadding() + 40.dp,
        start = padding.calculateLeftPadding(LayoutDirection.Ltr) + 24.dp,
        end = padding.calculateRightPadding(LayoutDirection.Ltr) + 24.dp,
    )
    var logoHeightDp by remember { mutableStateOf(300.dp) }

    // 分三段淡出：版本号 → 项目名 → Logo
    val versionCodeProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
    val projectNameProgress = ((scrollProgress - 0.20f) / 0.15f).coerceIn(0f, 1f)
    val iconProgress = ((scrollProgress - 0.35f) / 0.15f).coerceIn(0f, 1f)

    // 流光背景容器：背景随滚动淡出，并被上层玻璃卡片采样
    BgEffectBackground(
        dynamicBackground = isRuntimeShaderSupported(),
        modifier = Modifier.fillMaxSize(),
        bgModifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
        isFullSize = true,
        alpha = { 1f - scrollProgress },
    ) {
        // 浮动 Logo 区：叠在内容上方，随滚动上浮、分段淡出缩小
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = logoPadding.calculateTopPadding() + 52.dp,
                    start = logoPadding.calculateLeftPadding(LayoutDirection.Ltr),
                    end = logoPadding.calculateRightPadding(LayoutDirection.Ltr),
                )
                .onSizeChanged { size ->
                    with(density) { logoHeightDp = size.height.toDp() }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo：88dp 主题色圆角方牌 + 调色板图标
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer {
                        clip = true
                        shape = RoundedCornerShape(24.dp)
                        alpha = 1f - iconProgress
                        scaleX = 1f - iconProgress * 0.05f
                        scaleY = 1f - iconProgress * 0.05f
                    }
                    .background(MiuixTheme.colorScheme.primary),
            ) {
                Icon(
                    imageVector = PaletteIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }
            // 项目名
            Text(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 5.dp)
                    .graphicsLayer {
                        alpha = 1f - projectNameProgress
                        scaleX = 1f - projectNameProgress * 0.05f
                        scaleY = 1f - projectNameProgress * 0.05f
                    },
                text = "Pixo",
                color = MiuixTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = 1f - versionCodeProgress
                        scaleX = 1f - versionCodeProgress * 0.05f
                        scaleY = 1f - versionCodeProgress * 0.05f
                    },
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                text = appVersionName(),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }

        // 滚动内容：透明占位（匹配 Logo 高度）+ 玻璃卡片列表
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = scrollPadding.calculateTopPadding(),
                start = scrollPadding.calculateLeftPadding(LayoutDirection.Ltr),
                end = scrollPadding.calculateRightPadding(LayoutDirection.Ltr),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "logoSpacer") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(
                            logoHeightDp + 52.dp + logoPadding.calculateTopPadding() -
                                scrollPadding.calculateTopPadding() + 126.dp,
                        ),
                )
            }
            // 内容列填满剩余高度：保证在任何屏幕上都可向下滚动到底
            item(key = "about") {
                Column(
                    modifier = Modifier
                        .fillParentMaxHeight()
                        .padding(bottom = scrollPadding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().then(glassCard),
                        colors = cardColors,
                    ) {
                        ArrowPreference(
                            title = Str.open_source(),
                            endActions = {
                                ValueText("GitHub")
                            },
                            onClick = { openUrlInBrowser("https://github.com/Rune-cn/Pixo") },
                        )
                        ArrowPreference(
                            title = Str.check_update(),
                            endActions = {
                                ValueText(appVersionName())
                            },
                            onClick = { openUrlInBrowser("https://github.com/Rune-cn/Pixo/releases") },
                        )
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().then(glassCard),
                        colors = cardColors,
                    ) {
                        ArrowPreference(
                            title = sPrivacy,
                            onClick = { onOpenDoc(sPrivacy, sPrivacyContent) },
                        )
                        ArrowPreference(
                            title = sLicense,
                            onClick = { onOpenDoc(sLicense, sLicenseContent) },
                        )
                        ArrowPreference(
                            title = sDisclaimer,
                            onClick = { onOpenDoc(sDisclaimer, sDisclaimerContent) },
                        )
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().then(glassCard),
                        colors = cardColors,
                    ) {
                        ArrowPreference(
                            title = Str.feedback(),
                            endActions = {
                                ValueText("Issues")
                            },
                            onClick = { openUrlInBrowser("https://github.com/Rune-cn/Pixo/issues") },
                        )
                    }
                }
            }
        }

        // 滚动条
        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(lazyListState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            trackPadding = scrollPadding,
        )
    }
}


/** 关于页 Logo：调色板图标（与启动图标一致的插画主题）。 */
internal val PaletteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Palette",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M12 21a9 9 0 0 1 0 -18c4.97 0 9 3.582 9 8c0 1.06 -.474 2.078 -1.318 2.828c-.844 .75 -1.989 1.172 -3.182 1.172h-2.5a2 2 0 0 0 -1 3.75a1.3 1.3 0 0 1 -1 2.25",
            ),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        addPath(
            pathData = addPathNodes("M7.5 10.5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0"),
            fill = SolidColor(Color.White),
        )
    }.build()
}
