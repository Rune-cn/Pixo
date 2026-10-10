package app.pixo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Serializable
internal object Home

@Serializable
internal object AppSettings

@Serializable
internal object AppearanceSettings

@Serializable
internal object ContentSettings

@Serializable
internal object DataSettings

@Serializable
internal object Downloads

@Serializable
internal object About

@Serializable
internal object DebugSettings

@Serializable
internal object LogViewer

@Serializable
internal object CrashLog

@Serializable
internal object WebLogin

@Serializable
internal data class User(val userId: Long)

@Serializable
internal data class Follows(val userId: Long, val kind: String)

@Serializable
internal data class Ranking(val mode: String, val content: String = "illust")

@Serializable
internal data class Search(val query: String = "")

@Serializable
internal data class NovelRanking(val mode: String)

@Serializable
internal data class Bookmarks(val restrict: String)

@Serializable
internal data class Detail(val workId: Long, val kind: String)

@Serializable
internal data class Comments(val workId: Long, val kind: String)

/** 深链目标：由 `pixiv://artworks/{id}`、`pixiv://user/{id}` 或对应网页链接解析而来。 */
data class PixivDeepLink(val workId: Long = 0, val kind: String = "", val userId: Long = 0)

@Serializable
internal object Discover

@Serializable
internal object Recommended

private val themeModes = listOf(ColorSchemeMode.System, ColorSchemeMode.Dark, ColorSchemeMode.Light)

/** 动态取色模式：Monet 引擎（Android 12+ 取壁纸主色，桌面端回退默认色板）。 */
private val dynamicThemeModes = listOf(ColorSchemeMode.MonetSystem, ColorSchemeMode.MonetDark, ColorSchemeMode.MonetLight)

@Composable
fun App(deepLinkSignal: State<PixivDeepLink?>? = null) {
    var themeMode by rememberSaveable { mutableStateOf(ColorSchemeMode.System) }
    var dynamicColor by rememberSaveable { mutableStateOf(AppPrefs.dynamicColor) }
    var language by rememberSaveable { mutableStateOf(AppPrefs.language) }
    var floatingBar by rememberSaveable { mutableStateOf(AppPrefs.floatingBar) }
    var liquidGlass by rememberSaveable { mutableStateOf(AppPrefs.liquidGlass) }
    var ugoiraConfirm by rememberSaveable { mutableStateOf(AppPrefs.ugoiraConfirm) }
    val navController = rememberNavController()
    // 深链：首次启动或 App 运行中收到新的 pixiv:// 链接时跳转对应作品详情 / 用户主页
    val deepLink = deepLinkSignal?.value
    LaunchedEffect(deepLink?.workId, deepLink?.kind, deepLink?.userId) {
        deepLink?.let {
            if (it.userId > 0) {
                navController.navigate(User(it.userId)) { launchSingleTop = true }
            } else {
                navController.navigate(Detail(it.workId, it.kind)) { launchSingleTop = true }
            }
        }
    }

    // 主标签页：单个 Pager 承载首页/设置页，路由只代表「是否在标签根页」。
    // 选中态立即更新 + EaseInOut tween 快速切换，见 MainPagerState。
    val mainPagerState = rememberMainPagerState()
    // 子页面「去设置」请求：先回到标签根，再让 Pager 落到设置页
    var pendingTabPage by remember { mutableStateOf<Int?>(null) }
    val openSettingsTabFromSubPage: () -> Unit = {
        pendingTabPage = 1
        navController.navigate(Home) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openRanking: (String, String) -> Unit = { mode, content -> navController.navigate(Ranking(mode, content)) }
    val openNovelRanking: (String) -> Unit = { mode -> navController.navigate(NovelRanking(mode)) }
    val openDiscover: () -> Unit = { navController.navigate(Discover) }
    val openRecommended: () -> Unit = { navController.navigate(Recommended) }
    val openSearch: () -> Unit = { navController.navigate(Search()) }
    val openDetail: (Long, String) -> Unit = { workId, kind -> navController.navigate(Detail(workId, kind)) }
    val openBookmarks: (String) -> Unit = { restrict -> navController.navigate(Bookmarks(restrict)) }
    val openUser: (Long) -> Unit = { id -> navController.navigate(User(id)) }
    val openFollows: (Long, String) -> Unit = { id, kind -> navController.navigate(Follows(id, kind)) }
    val openWebLogin: () -> Unit = { navController.navigate(WebLogin) }
    val openAppSettings: () -> Unit = { navController.navigate(AppSettings) }
    val openDownloads: () -> Unit = { navController.navigate(Downloads) }
    val openAbout: () -> Unit = { navController.navigate(About) }
    val controller = remember(themeMode, dynamicColor) {
        if (dynamicColor && !isDesktop) {
            // Android：Monet 引擎跟随壁纸动态取色
            ThemeController(
                colorSchemeMode = when (themeMode) {
                    ColorSchemeMode.Dark -> ColorSchemeMode.MonetDark
                    ColorSchemeMode.Light -> ColorSchemeMode.MonetLight
                    else -> ColorSchemeMode.MonetSystem
                },
            )
        } else {
            ThemeController(themeMode)
        }
    }
    LaunchedEffect(language) { applyAppLanguage(language) }
    MiuixTheme(controller = controller) {
        AppEnvironment(language = language) {
            applyWindowBackground(MiuixTheme.colorScheme.surface)
            val backStackEntry by navController.currentBackStackEntryAsState()
            val current = backStackEntry?.destination
            val isHomeRoute = current?.route == "app.pixo.Home"
            val isTabPage = isHomeRoute
            Scaffold() { _ ->
                // 底部导航以浮层方式贴在内容之上；NavHost 不再按路由切换内边距，
                // 进入/返回时页面容器尺寸恒定，消除整页上抬/下沉。
                var barHeight by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current
                val backRoute = current?.route
                val isAtRoot = backRoute == "app.pixo.Home"
                // 标签根页且停在设置页：返回手势应先回落首页标签
                val atRootOnSecondTab = isAtRoot && mainPagerState.selectedPage != 0
                val barBackdrop = rememberPixoBlurBackdrop()
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (isDesktop) {
                                Modifier.onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                        when {
                                            !isAtRoot -> { navController.popBackStack(); true }
                                            atRootOnSecondTab -> { mainPagerState.animateToPage(0); true }
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (barBackdrop != null && floatingBar && isTabPage) {
                                    Modifier.layerBackdrop(barBackdrop)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                    NavHost(
                        navController = navController,
                        startDestination = Home,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                    composable<Home>(
                        enterTransition = { miuixFadeEnter() },
                        exitTransition = { miuixFadeExit() },
                        popEnterTransition = { miuixFadeEnter() },
                        popExitTransition = { miuixFadeExit() },
                    ) {
                        MainTabsHost(
                            mainPagerState = mainPagerState,
                            bottomPadding = barHeight,
                            pendingTabPage = pendingTabPage,
                            onPendingTabHandled = { pendingTabPage = null },
                            onGoSettingsTab = { mainPagerState.animateToPage(1) },
                            onOpenRanking = openRanking,
                            onOpenNovelRanking = openNovelRanking,
                            onOpenDiscover = openDiscover,
                            onOpenRecommended = openRecommended,
                            onOpenSearch = openSearch,
                            onOpenDetail = openDetail,
                            onOpenBookmarks = openBookmarks,
                            onOpenAppSettings = openAppSettings,
                            onOpenDownloads = openDownloads,
                            onOpenAbout = openAbout,
                            onOpenUser = openUser,
                            onOpenFollows = openFollows,
                            onOpenWebLogin = openWebLogin,
                        )
                    }
                    composable<AppSettings>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        AppSettingsPage(
                            onOpenAppearance = { navController.navigate(AppearanceSettings) },
                            onOpenContent = { navController.navigate(ContentSettings) },
                            onOpenData = { navController.navigate(DataSettings) },
                            onOpenDebug = { navController.navigate(DebugSettings) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<ContentSettings>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        ContentPage(
                            ugoiraConfirm = ugoiraConfirm,
                            onUgoiraConfirmChange = {
                                ugoiraConfirm = it
                                AppPrefs.ugoiraConfirm = it
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<DataSettings>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        DataPage(onBack = { navController.popBackStack() })
                    }
                    composable<Downloads>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        DownloadsScreen(onBack = { navController.popBackStack() })
                    }
                    composable<About>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        AboutScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<AppearanceSettings>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        AppearancePage(
                            themeMode = themeMode,
                            dynamicColor = dynamicColor,
                            onDynamicColorChange = {
                                dynamicColor = it
                                AppPrefs.dynamicColor = it
                            },
                            onThemeModeChange = { themeMode = it },
                            floatingBar = floatingBar,
                            onFloatingBarChange = {
                                floatingBar = it
                                AppPrefs.floatingBar = it
                            },
                            liquidGlass = liquidGlass,
                            onLiquidGlassChange = {
                                liquidGlass = it
                                AppPrefs.liquidGlass = it
                            },
                            language = language,
                            onLanguageChange = {
                                language = it
                                AppPrefs.language = it
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<DebugSettings>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        DebugPage(
                            onOpenLogs = { navController.navigate(LogViewer) },
                            onOpenCrashLog = { navController.navigate(CrashLog) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<LogViewer>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        LogViewerScreen(onBack = { navController.popBackStack() })
                    }
                    composable<CrashLog>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        CrashLogPage(onBack = { navController.popBackStack() })
                    }
                    composable<WebLogin>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        WebLoginScreen(onBack = { navController.popBackStack() })
                    }
                    composable<Discover>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        DiscoverScreen(
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            onOpenSettings = openSettingsTabFromSubPage,
                        )
                    }
                    composable<Recommended>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) {
                        RecommendedScreen(
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            onOpenSettings = openSettingsTabFromSubPage,
                        )
                    }
                    composable<Ranking>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) { entry ->
                        RankingScreen(
                            mode = entry.toRoute<Ranking>().mode,
                            content = entry.toRoute<Ranking>().content,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                        )
                    }
                    composable<NovelRanking>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) { entry ->
                        NovelRankingScreen(
                            mode = entry.toRoute<NovelRanking>().mode,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                        )
                    }
                    composable<Bookmarks>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) { entry ->
                        BookmarksScreen(
                            restrict = entry.toRoute<Bookmarks>().restrict,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                        )
                    }
                    composable<Search>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) { entry ->
                        SearchScreen(
                            initialQuery = entry.toRoute<Search>().query,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                        )
                    }
                    composable<Detail>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) { entry ->
                        DetailScreen(
                            workId = entry.toRoute<Detail>().workId,
                            kind = entry.toRoute<Detail>().kind,
                            onBack = { navController.popBackStack() },
                            onSearchTag = { tag -> navController.navigate(Search(tag)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            onOpenComments = { workId, kind -> navController.navigate(Comments(workId, kind)) },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) { launchSingleTop = true } },
                        )
                    }
                    composable<Comments>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) { entry ->
                        CommentsScreen(
                            workId = entry.toRoute<Comments>().workId,
                            kind = entry.toRoute<Comments>().kind,
                            onBack = { navController.popBackStack() },
                            onOpenUser = { navController.navigate(User(it)) },
                        )
                    }
                    composable<User>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) { entry ->
                        UserScreen(
                            userId = entry.toRoute<User>().userId,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            onOpenFollows = { id, kind -> navController.navigate(Follows(id, kind)) },
                        )
                    }
                    composable<Follows>(
                        enterTransition = { miuixPushEnter() },
                        exitTransition = { miuixPushExit() },
                        popEnterTransition = { miuixPopEnter() },
                        popExitTransition = { miuixPopExit() },
                    ) { entry ->
                        FollowsScreen(
                            userId = entry.toRoute<Follows>().userId,
                            kind = entry.toRoute<Follows>().kind,
                            onBack = { navController.popBackStack() },
                            onOpenUser = { navController.navigate(User(it)) },
                        )
                    }
                }
                val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
                // 返回手势：拦截后由 App 播放 MIUI 弹回动画（快速、效果一致）；
                // 标签根页停在设置页时，返回手势先回落首页标签。
                NavigationBackHandler(
                    state = navEventState,
                    isBackEnabled = atRootOnSecondTab || !isAtRoot,
                    onBackCompleted = {
                        if (atRootOnSecondTab) mainPagerState.animateToPage(0) else navController.popBackStack()
                    },
                )
                } // 内容盒（带 layerBackdrop 取景）到此结束；两条底栏必须在其外，
                  // 否则悬浮栏 drawBackdrop 会把「包含它自己的整页」递归快照，渲染树无限加深导致栈溢出
                // 悬浮底栏开关切换：两种底栏交叉淡入淡出 + 底部滑入滑出
                if (isTabPage) {
                    AnimatedVisibility(
                        visible = !floatingBar,
                        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it },
                        exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it },
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            NavigationBar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .onSizeChanged {
                                        barHeight = with(density) { it.height.toDp() }
                                    },
                                // 默认“自动”模式：选中图标上移、取消选中回落，文字只在选中时淡入
                                mode = NavigationBarDisplayMode.IconWithSelectedLabel,
                            ) {
                                NavigationBarItem(
                                    selected = mainPagerState.selectedPage == 0,
                                    onClick = { mainPagerState.animateToPage(0) },
                                    icon = MiuixIcons.Home,
                                    label = Str.nav_home(),
                                )
                                NavigationBarItem(
                                    selected = mainPagerState.selectedPage == 1,
                                    onClick = { mainPagerState.animateToPage(1) },
                                    icon = MiuixIcons.Settings,
                                    label = Str.nav_settings(),
                                )
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = floatingBar,
                        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it },
                        exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it },
                    ) {
                        FloatingBottomBar(
                            items = listOf(
                                FloatingBottomBarItem(MiuixIcons.Home, Str.nav_home()),
                                FloatingBottomBarItem(MiuixIcons.Settings, Str.nav_settings()),
                            ),
                            selectedIndex = mainPagerState.selectedPage,
                            onSelected = { index -> mainPagerState.animateToPage(index) },
                            backdrop = barBackdrop,
                            mode = if (liquidGlass) FloatingBottomBarMode.LiquidGlass else FloatingBottomBarMode.None,
                            onSizeChanged = { barHeight = it },
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * 标签页容器：首页与账号页由同一个 HorizontalPager 承载，
 * 左右滑动/悬浮底栏/普通底栏都在页内切换，绝不触发路由跳转；
 * PagerState 提升到 App 层，跨子页面保留在页滚动位。
 */
@Composable
private fun MainTabsHost(
    mainPagerState: MainPagerState,
    bottomPadding: Dp,
    pendingTabPage: Int?,
    onPendingTabHandled: () -> Unit,
    onGoSettingsTab: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenRanking: (String, String) -> Unit,
    onOpenNovelRanking: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenRecommended: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenBookmarks: (String) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenFollows: (Long, String) -> Unit,
    onOpenWebLogin: () -> Unit,
) {
    // 手动滑动落定后同步悬浮底栏高亮
    LaunchedEffect(mainPagerState.pagerState.currentPage) { mainPagerState.syncPage() }
    // 子页面「去设置」：回到标签根后把 Pager 落到设置页
    LaunchedEffect(pendingTabPage) {
        val target = pendingTabPage ?: return@LaunchedEffect
        mainPagerState.animateToPage(target)
        onPendingTabHandled()
    }
    HorizontalPager(
        state = mainPagerState.pagerState,
        userScrollEnabled = true,
        overscrollEffect = null,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        if (page == 0) {
            HomePage(
                onOpenRanking = onOpenRanking,
                onOpenNovelRanking = onOpenNovelRanking,
                onOpenDiscover = onOpenDiscover,
                onOpenRecommended = onOpenRecommended,
                onOpenSearch = onOpenSearch,
                onOpenDetail = onOpenDetail,
                onOpenBookmarks = onOpenBookmarks,
                onOpenSettings = onGoSettingsTab,
                onOpenUser = onOpenUser,
                bottomPadding = bottomPadding,
            )
        } else {
            SettingsPage(
                onOpenWebLogin = onOpenWebLogin,
                onOpenUser = onOpenUser,
                onOpenFollows = onOpenFollows,
                onOpenAppSettings = onOpenAppSettings,
                onOpenDownloads = onOpenDownloads,
                onOpenAbout = onOpenAbout,
                bottomPadding = bottomPadding,
            )
        }
    }
}

@Composable
fun SettingsPage(
    onOpenWebLogin: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenFollows: (Long, String) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
    bottomPadding: Dp = 0.dp,
) {
    var selfId by remember { mutableStateOf(AppPrefs.selfUserId) }
    var selfInfo by remember {
        mutableStateOf(
            if (selfId > 0L) SessionCache.selfInfo?.takeIf { it.id == selfId } else null,
        )
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selfId) {
        selfInfo = if (selfId > 0L) {
            try {
                SessionCache.selfInfo?.takeIf { it.id == selfId }
                    ?: PixivApi.fetchUser(selfId).also { SessionCache.selfInfo = it }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun doLogout() {
        PixivAuth.logout()
        selfId = 0
        selfInfo = null
    }

    val scrollBehavior = MiuixScrollBehavior()
    PixoTabFrame(title = Str.nav_settings(), scrollBehavior = scrollBehavior) { framePadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = framePadding.calculateTopPadding(), bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            overscrollEffect = null,
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    if (selfId <= 0L) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(onClick = onOpenWebLogin, modifier = Modifier.fillMaxWidth()) {
                                Text(Str.login_button())
                            }
                        }
                    } else {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AuthorAvatar(selfInfo?.avatar.orEmpty(), 72.dp, selfId, onOpenUser)
                            Text(selfInfo?.name.orEmpty(), style = MiuixTheme.textStyles.title3)
                            selfInfo?.account?.takeIf { it.isNotBlank() }?.let {
                                Text("@" + it, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                UserStatCard(Str.following(), selfInfo?.followingCount, Modifier.weight(1f)) {
                                    onOpenFollows(selfId, "following")
                                }
                                UserStatCard(Str.follower(), selfInfo?.followerCount, Modifier.weight(1f)) {
                                    onOpenFollows(selfId, "followers")
                                }
                                UserStatCard(Str.works_title(), selfInfo?.let { it.totalIllusts + it.totalNovels }, Modifier.weight(1f)) {
                                    onOpenUser(selfId)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { doLogout() }) {
                                Text(Str.logout_button())
                            }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    ArrowPreference(
                        title = Str.settings_title(),
                        onClick = onOpenAppSettings,
                    )
                    ArrowPreference(
                        title = Str.download(),
                        onClick = onOpenDownloads,
                    )
                    ArrowPreference(
                        title = Str.about(),
                        onClick = onOpenAbout,
                    )
                }
            }
        }
    }
}

/** 设置页：分类入口卡片（外观 / 调试，点进子页设置）。 */
@Composable
fun AppSettingsPage(
    onOpenAppearance: () -> Unit,
    onOpenContent: () -> Unit,
    onOpenData: () -> Unit,
    onOpenDebug: () -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.settings_title(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            overscrollEffect = null,
        ) {
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 4.dp),
                ) {
                    SettingEntryRow(Str.settings_appearance(), onClick = onOpenAppearance)
                    SettingEntryRow(Str.settings_content(), onClick = onOpenContent)
                    SettingEntryRow(Str.settings_data(), onClick = onOpenData)
                    SettingEntryRow(Str.settings_debug(), onClick = onOpenDebug)
                }
            }
        }
    }
}

/** 数据页：导出 / 导入设置、下载目录、清除缓存。 */
@Composable
fun DataPage(onBack: () -> Unit) {
    var toast by remember { mutableStateOf<String?>(null) }
    var downloadDir by remember { mutableStateOf(AppPrefs.downloadDir) }
    val sExportSuccess = Str.export_success()
    val sExportFailed = Str.export_failed()
    val sImportSuccess = Str.import_success()
    val sImportFailed = Str.import_failed()
    val sCacheCleared = Str.cache_cleared()
    val pickFolder = rememberDownloadFolderPicker { dir -> downloadDir = dir }
    val exportFile = rememberExportSettingsFile { ok ->
        toast = if (ok) sExportSuccess else sExportFailed
    }
    val importFile = rememberImportSettingsFile { json ->
        if (json == null) return@rememberImportSettingsFile
        toast = if (importSettingsJson(json)) sImportSuccess else sImportFailed
    }
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.settings_data(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                overscrollEffect = null,
            ) {
                item { SettingsSectionHeader(Str.settings_section_data()) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        ArrowPreference(
                            title = Str.export_settings(),
                            summary = Str.export_settings_summary(),
                            onClick = { exportFile() },
                        )
                        ArrowPreference(
                            title = Str.import_settings(),
                            summary = Str.import_settings_summary(),
                            onClick = { importFile() },
                        )
                        ArrowPreference(
                            title = Str.clear_cache(),
                            summary = Str.clear_cache_summary(),
                            onClick = {
                                clearAppCache()
                                toast = sCacheCleared
                            },
                        )
                    }
                }
                item { SettingsSectionHeader(Str.settings_section_storage()) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        ArrowPreference(
                            title = Str.download_folder(),
                            summary = downloadDir.ifBlank { "Pictures/Pixo" },
                            onClick = { pickFolder() },
                        )
                    }
                }
            }
            BottomToast(toast)
        }
    }
}

/** 设置列表项：标题 + 右箭头（MIUIX ArrowPreference）。 */
@Composable
private fun SettingEntryRow(title: String, onClick: () -> Unit) {
    ArrowPreference(title = title, onClick = onClick)
}

/** 下载页：展示已下载目录里的图片（默认 Pictures/Pixo 或自选 SAF 目录）。 */
@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    var images by remember { mutableStateOf<List<DownloadedImage>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var viewer by remember { mutableStateOf<DownloadedImage?>(null) }
    val sNone = Str.no_downloaded_images()

    LaunchedEffect(refreshKey) {
        images = null
        error = null
        runCatching { listDownloadedImages() }
            .onSuccess { images = it }
            .onFailure { error = it.friendlyMessage() }
    }

    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.download(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            when {
                images == null && error == null ->
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        LoadingState()
                    }
                error != null && images == null ->
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        ErrorState(error ?: "", onRetry = { refreshKey++ })
                    }
                images.orEmpty().isEmpty() ->
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(
                            sNone,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(images.orEmpty(), key = { it.uri }) { img ->
                        DownloadedImageCard(img, onClick = { viewer = img })
                    }
                }
            }
            viewer?.let { img ->
                OverlayDialog(show = true, onDismissRequest = { viewer = null }) {
                    Box(
                        Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = img.uri,
                            contentDescription = img.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 下载页网格项：缩略图 + 文件名 + 大小。 */
@Composable
private fun DownloadedImageCard(img: DownloadedImage, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = img.uri,
            contentDescription = img.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            img.name,
            style = MiuixTheme.textStyles.footnote2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            formatBytes(img.size),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

/** 关于页 Logo：调色板图标（与启动图标一致的插画主题）。 */
private val PaletteIcon: ImageVector by lazy {
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
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
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
private fun AboutContent(
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

/** 关于页右侧值文本：与列表项等高的主题次要色。 */
@Composable
private fun ValueText(text: String) {
    Text(
        text = text,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
    )
}

/** 设置分区标题：小号 + 中等字重 + 次要色（分区标记）。 */
@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
    )
}

/**
 * 下拉选择行（沿用原版 OverlayDropdownPreference）：展开时由本组件接管返回键，
 * 通过重建偏好项复位其内部展开态来关闭浮层（预见式返回），不弹走设置页。
 */
@Composable
private fun PixoDropdownPreference(
    title: String,
    summary: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dropdownOpen by remember { mutableStateOf(false) }
    var closeSignal by remember { mutableIntStateOf(0) }
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
    // 浮层打开时，返回手势由本组件接管：重建偏好项使其内部展开态复位 → 浮层关闭
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = dropdownOpen,
        onBackCompleted = {
            dropdownOpen = false
            closeSignal++
        },
    )
    key(closeSignal) {
        OverlayDropdownPreference(
            items = options,
            selectedIndex = selectedIndex,
            title = title,
            summary = summary,
            onSelectedIndexChange = onSelectedIndexChange,
            onExpandedChange = { dropdownOpen = it },
            modifier = modifier,
        )
    }
}

/** 外观设置页：语言 / 主题 / 动态取色 / 悬浮底栏 / 液态玻璃。 */
@Composable
fun AppearancePage(
    themeMode: ColorSchemeMode,
    onThemeModeChange: (ColorSchemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    floatingBar: Boolean,
    onFloatingBarChange: (Boolean) -> Unit,
    liquidGlass: Boolean,
    onLiquidGlassChange: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    val themeOptions = listOf(Str.theme_follow(), Str.theme_dark(), Str.theme_light())
    val languageOptions = listOf(Str.language_follow(), Str.language_zh(), Str.language_en())
    val languageValues = listOf("system", "zh", "en")
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.settings_appearance(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item { SettingsSectionHeader(Str.settings_section_general()) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    PixoDropdownPreference(
                        title = Str.settings_language(),
                        summary = languageOptions[languageValues.indexOf(language).coerceAtLeast(0)],
                        options = languageOptions,
                        selectedIndex = languageValues.indexOf(language).coerceAtLeast(0),
                        onSelectedIndexChange = { onLanguageChange(languageValues[it]) },
                    )
                    PixoDropdownPreference(
                        title = Str.theme_mode(),
                        summary = themeOptions[themeModes.indexOf(themeMode).coerceAtLeast(0)],
                        options = themeOptions,
                        selectedIndex = themeModes.indexOf(themeMode).coerceAtLeast(0),
                        onSelectedIndexChange = { onThemeModeChange(themeModes[it]) },
                    )
                    SwitchPreference(
                        title = Str.dynamic_color(),
                        summary = Str.dynamic_color_summary(),
                        checked = dynamicColor,
                        onCheckedChange = { onDynamicColorChange(it) },
                    )
                }
            }
            item { SettingsSectionHeader(Str.settings_section_bottom_bar()) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SwitchPreference(
                        title = Str.floating_bar(),
                        summary = Str.floating_bar_summary(),
                        checked = floatingBar,
                        onCheckedChange = { onFloatingBarChange(it) },
                    )
                    // 液态玻璃只对悬浮底栏生效：悬浮底栏关闭时不显示该项
                    if (floatingBar) {
                        SwitchPreference(
                            title = Str.liquid_glass(),
                            summary = Str.liquid_glass_summary(),
                            checked = liquidGlass,
                            onCheckedChange = { onLiquidGlassChange(it) },
                        )
                    }
                }
            }
        }
    }
}

/** 内容设置页：作品浏览相关偏好（动图播放需确认等）。 */
@Composable
fun ContentPage(
    ugoiraConfirm: Boolean,
    onUgoiraConfirmChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.settings_content(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item { SettingsSectionHeader(Str.settings_section_artwork()) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SwitchPreference(
                        title = Str.ugoira_confirm(),
                        summary = Str.ugoira_confirm_summary(),
                        checked = ugoiraConfirm,
                        onCheckedChange = { onUgoiraConfirmChange(it) },
                    )
                }
            }
        }
    }
}

/** 调试设置页：日志开关 / 查看日志 / 崩溃日志。 */
@Composable
fun DebugPage(
    onOpenLogs: () -> Unit,
    onOpenCrashLog: () -> Unit,
    onBack: () -> Unit,
) {
    var debugLog by remember { mutableStateOf(AppPrefs.debugLog) }
    var showExport by remember { mutableStateOf(false) }
    var exportCopied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    fun exportText(): String = buildString {
        appendLine("Pixo login info (debug)")
        appendLine("---- refresh_token ----")
        appendLine(AppPrefs.refreshToken.ifBlank { "(empty)" })
        appendLine("---- access_token ----")
        appendLine(AppPrefs.accessToken.ifBlank { "(empty)" })
        appendLine("---- cookie ----")
        appendLine(AppPrefs.cookie.ifBlank { "(empty)" })
        appendLine("---- self_user_id ----")
        appendLine(AppPrefs.selfUserId.toString())
        appendLine("---- last_login_user ----")
        appendLine(AppPrefs.lastLoginUser.ifBlank { "(empty)" })
    }
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.settings_debug(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item { SettingsSectionHeader(Str.settings_section_logs()) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SwitchPreference(
                        title = Str.debug_log(),
                        summary = Str.debug_log_summary(),
                        checked = debugLog,
                        onCheckedChange = {
                            debugLog = it
                            AppPrefs.debugLog = it
                        },
                    )
                    SettingEntryRow(Str.view_logs(), onClick = onOpenLogs)
                    SettingEntryRow(Str.crash_log(), onClick = onOpenCrashLog)
                }
            }
            item { SettingsSectionHeader(Str.settings_section_login()) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SettingEntryRow(Str.export_login_info(), onClick = { showExport = true })
                    Text(
                        text = Str.export_login_summary(),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }

    OverlayDialog(show = showExport, onDismissRequest = { showExport = false }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Str.export_login_dialog_title(), style = MiuixTheme.textStyles.title3)
            Text(
                text = exportText(),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(exportText()))
                    exportCopied = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(Str.copy_all())
            }
            if (exportCopied) {
                Text(
                    text = Str.export_copied(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            Button(onClick = { showExport = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
        }
    }
}
