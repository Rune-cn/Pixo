package app.pixo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun App(deepLinkSignal: State<PixivDeepLink?>? = null) {
    var themeMode by rememberSaveable { mutableStateOf(savedThemeMode()) }
    var dynamicColor by rememberSaveable { mutableStateOf(AppPrefs.dynamicColor) }
    var language by rememberSaveable { mutableStateOf(AppPrefs.language) }
    var floatingBar by rememberSaveable { mutableStateOf(AppPrefs.floatingBar) }
    var liquidGlass by rememberSaveable { mutableStateOf(AppPrefs.liquidGlass) }
    var ugoiraConfirm by rememberSaveable { mutableStateOf(AppPrefs.ugoiraConfirm) }
    var waterfall by rememberSaveable { mutableStateOf(AppPrefs.waterfall) }
    var imageQuality by rememberSaveable { mutableStateOf(AppPrefs.imageQuality) }
    var disableSwipeBack by rememberSaveable { mutableStateOf(AppPrefs.disableSwipeBack) }
    val navController = rememberNavController()

    // 启动时自动检查更新（设置 → 更新 可关闭）；同一版本只提醒一次
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        if (!AppPrefs.autoCheckUpdate) return@LaunchedEffect
        // 6 小时内只查一次；失败静默（不打扰用户），手动检查在「关于」页
        val info = runCatching { UpdateChecker.check(force = false) }.getOrNull()
            ?: return@LaunchedEffect
        if (UpdateChecker.isNewer(info.version, appVersionName()) &&
            info.version != AppPrefs.lastNotifiedVersion
        ) {
            AppPrefs.lastNotifiedVersion = info.version
            updateInfo = info
        }
    }
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
    val openHistory: () -> Unit = { navController.navigate(History) }
    val openFollow: () -> Unit = { navController.navigate(Follow) }
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
            val isHomeRoute = current?.hasRoute<Home>() == true
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
                    pixoScreenFade<Home> {
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
                            waterfall = waterfall,
                            onOpenFollow = openFollow,
                            onOpenAppSettings = openAppSettings,
                            onOpenHistory = openHistory,
                            onOpenDownloads = openDownloads,
                            onOpenAbout = openAbout,
                            onOpenUser = openUser,
                            onOpenFollows = openFollows,
                            onOpenWebLogin = openWebLogin,
                        )
                    }
                    pixoScreen<AppSettings> {
                        AppSettingsPage(
                            onOpenAppearance = { navController.navigate(AppearanceSettings) },
                            onOpenContent = { navController.navigate(ContentSettings) },
                            onOpenData = { navController.navigate(DataSettings) },
                            onOpenDebug = { navController.navigate(DebugSettings) },
                            onOpenUpdate = { navController.navigate(UpdateSettings) },
                            onOpenLogin = { navController.navigate(LoginSettings) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    pixoScreen<ContentSettings> {
                        ContentPage(
                            ugoiraConfirm = ugoiraConfirm,
                            onUgoiraConfirmChange = {
                                ugoiraConfirm = it
                                AppPrefs.ugoiraConfirm = it
                            },
                            waterfall = waterfall,
                            onWaterfallChange = {
                                waterfall = it
                                AppPrefs.waterfall = it
                            },
                            imageQuality = imageQuality,
                            onImageQualityChange = {
                                imageQuality = it
                                AppPrefs.imageQuality = it
                                invalidateFeedCaches()
                            },
                            disableSwipeBack = disableSwipeBack,
                            onDisableSwipeBackChange = {
                                disableSwipeBack = it
                                AppPrefs.disableSwipeBack = it
                            },
                            onOpenTagBlock = { navController.navigate(TagBlockSettings) },
                            onOpenAuthorBlock = { navController.navigate(AuthorBlockSettings) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    pixoScreen<AuthorBlockSettings> {
                        AuthorBlockScreen(onBack = { navController.popBackStack() })
                    }
                    pixoScreen<TagBlockSettings> {
                        TagBlockScreen(onBack = { navController.popBackStack() })
                    }
                    pixoScreen<DataSettings> {
                        DataPage(onBack = { navController.popBackStack() })
                    }
                    pixoScreen<Downloads> {
                        DownloadsScreen(
                            onBack = { navController.popBackStack() },
                            waterfall = waterfall,
                        )
                    }
                    pixoScreen<History> {
                        HistoryScreen(
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            waterfall = waterfall,
                        )
                    }
                    pixoScreen<Follow> {
                        FollowScreen(
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            waterfall = waterfall,
                        )
                    }
                    pixoScreen<About> {
                        AboutScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                    pixoScreen<AppearanceSettings> {
                        AppearancePage(
                            themeMode = themeMode,
                            dynamicColor = dynamicColor,
                            onDynamicColorChange = {
                                dynamicColor = it
                                AppPrefs.dynamicColor = it
                            },
                            onThemeModeChange = {
                                themeMode = it
                                AppPrefs.themeMode = it.name
                            },
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
                    pixoScreen<LoginSettings> {
                        LoginPage(onBack = { navController.popBackStack() })
                    }
                    pixoScreen<UpdateSettings> {
                        UpdatePage(onBack = { navController.popBackStack() })
                    }
                    pixoScreen<DebugSettings> {
                        DebugPage(
                            onOpenLogs = { navController.navigate(LogViewer) },
                            onOpenCrashLog = { navController.navigate(CrashLog) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    pixoScreen<LogViewer> {
                        LogViewerScreen(onBack = { navController.popBackStack() })
                    }
                    pixoScreen<CrashLog> {
                        CrashLogPage(onBack = { navController.popBackStack() })
                    }
                    pixoScreen<WebLogin> {
                        WebLoginScreen(onBack = { navController.popBackStack() })
                    }
                    pixoScreen<Discover> {
                        DiscoverScreen(
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            onOpenSettings = openSettingsTabFromSubPage,
                            waterfall = waterfall,
                        )
                    }
                    pixoScreen<Recommended> {
                        RecommendedScreen(
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            onOpenSettings = openSettingsTabFromSubPage,
                            waterfall = waterfall,
                        )
                    }
                    pixoScreen<Ranking> { entry ->
                        RankingScreen(
                            mode = entry.toRoute<Ranking>().mode,
                            content = entry.toRoute<Ranking>().content,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            waterfall = waterfall,
                        )
                    }
                    pixoScreen<NovelRanking> { entry ->
                        NovelRankingScreen(
                            mode = entry.toRoute<NovelRanking>().mode,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            waterfall = waterfall,
                        )
                    }
                    pixoScreen<Bookmarks> { entry ->
                        BookmarksScreen(
                            restrict = entry.toRoute<Bookmarks>().restrict,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            waterfall = waterfall,
                        )
                    }
                    pixoScreen<Search> { entry ->
                        SearchScreen(
                            initialQuery = entry.toRoute<Search>().query,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            waterfall = waterfall,
                        )
                    }
                    pixoScreen<Detail> { entry ->
                        DetailScreen(
                            workId = entry.toRoute<Detail>().workId,
                            kind = entry.toRoute<Detail>().kind,
                            onBack = { navController.popBackStack() },
                            onSearchTag = { tag -> navController.navigate(Search(tag)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            onOpenComments = { workId, kind -> navController.navigate(Comments(workId, kind)) },
                            onOpenDetail = { workId, kind ->
                                // 相关作品：标准入栈（规范：navigate 入栈、返回 pop 栈顶回到上一个详情）
                                navController.navigate(Detail(workId, kind))
                            },
                        )
                    }
                    pixoScreen<Comments> { entry ->
                        CommentsScreen(
                            workId = entry.toRoute<Comments>().workId,
                            kind = entry.toRoute<Comments>().kind,
                            onBack = { navController.popBackStack() },
                            onOpenUser = { navController.navigate(User(it)) },
                        )
                    }
                    pixoScreen<User> { entry ->
                        UserScreen(
                            userId = entry.toRoute<User>().userId,
                            onBack = { navController.popBackStack() },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenUser = { navController.navigate(User(it)) },
                            onOpenFollows = { id, kind -> navController.navigate(Follows(id, kind)) },
                            waterfall = waterfall,
                        )
                    }
                    pixoScreen<Follows> { entry ->
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
                    AnimatedVisibility(
                        visible = !floatingBar && isTabPage,
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
                        visible = floatingBar && isTabPage,
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
                    // 启动时检查到新版本：在 Scaffold 内弹提示（OverlayDialog 需要 Scaffold 祖先）
                    updateInfo?.let { info ->
                        UpdateAvailableSheet(info = info, onDismiss = { updateInfo = null })
                    }
                // 底栏只在标签根页（首页/设置标签）展示；子页面（详情/设置子页等）不显示底栏
                  // 否则悬浮栏 drawBackdrop 会把「包含它自己的整页」递归快照，渲染树无限加深导致栈溢出
                // 悬浮底栏开关切换：两种底栏交叉淡入淡出 + 底部滑入滑出
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
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenRanking: (String, String) -> Unit,
    onOpenNovelRanking: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenRecommended: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenBookmarks: (String) -> Unit,
    waterfall: Boolean,
    onOpenFollow: () -> Unit,
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
                waterfall = waterfall,
                onOpenFollow = onOpenFollow,
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
                onOpenHistory = onOpenHistory,
                onOpenDownloads = onOpenDownloads,
                onOpenAbout = onOpenAbout,
                bottomPadding = bottomPadding,
            )
        }
    }
}
