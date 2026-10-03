package app.pixo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Serializable
internal object Home

@Serializable
internal object Settings

@Serializable
internal object AppSettings

@Serializable
internal object AppearanceSettings

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
internal data class Ranking(val mode: String)

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

@Serializable
internal object Discover

@Serializable
internal object Recommended

private val themeModes = listOf(ColorSchemeMode.System, ColorSchemeMode.Dark, ColorSchemeMode.Light)

@Composable
fun App() {
    var themeMode by rememberSaveable { mutableStateOf(ColorSchemeMode.System) }
    var language by rememberSaveable { mutableStateOf(AppPrefs.language) }
    val navController = rememberNavController()
    val controller = remember(themeMode) { ThemeController(themeMode) }
    LaunchedEffect(language) { applyAppLanguage(language) }
    MiuixTheme(controller = controller) {
        AppEnvironment(language = language) {
            applyWindowBackground(MiuixTheme.colorScheme.surface)
            val backStackEntry by navController.currentBackStackEntryAsState()
            val current = backStackEntry?.destination
            val isHomeRoute = current?.route == "app.pixo.Home"
            val isSettingsRoute = current?.route == "app.pixo.Settings"
            val isTabPage = isHomeRoute || isSettingsRoute
            Scaffold() { _ ->
                // 底部导航以浮层方式贴在内容之上；NavHost 不再按路由切换内边距，
                // 进入/返回时页面容器尺寸恒定，消除整页上抬/下沉。
                var barHeight by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current
                Box(Modifier.fillMaxSize()) {
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
                        HomePage(
                            onOpenRanking = { mode -> navController.navigate(Ranking(mode)) },
                            onOpenNovelRanking = { mode -> navController.navigate(NovelRanking(mode)) },
                            onOpenDiscover = { navController.navigate(Discover) },
                            onOpenRecommended = { navController.navigate(Recommended) },
                            onOpenSearch = { navController.navigate(Search()) },
                            onOpenDetail = { workId, kind -> navController.navigate(Detail(workId, kind)) },
                            onOpenBookmarks = { restrict -> navController.navigate(Bookmarks(restrict)) },
                            onOpenSettings = {
                                navController.navigate(Settings) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onOpenUser = { navController.navigate(User(it)) },
                            bottomPadding = barHeight,
                        )
                    }
                    composable<Settings>(
                        enterTransition = { miuixFadeEnter() },
                        exitTransition = { miuixFadeExit() },
                        popEnterTransition = { miuixFadeEnter() },
                        popExitTransition = { miuixFadeExit() },
                    ) {
                        SettingsPage(
                            onOpenWebLogin = { navController.navigate(WebLogin) },
                            onOpenUser = { navController.navigate(User(it)) },
                            onOpenSettings = { navController.navigate(AppSettings) },
                            bottomPadding = barHeight,
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
                            onOpenDebug = { navController.navigate(DebugSettings) },
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
                            onThemeModeChange = { themeMode = it },
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
                        )
                    }
                }
                if (isTabPage) {
                    NavigationBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged {
                                barHeight = with(density) { it.height.toDp() }
                            },
                    ) {
                        NavigationBarItem(
                            selected = isHomeRoute,
                            onClick = {
                                navController.navigate(Home) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = MiuixIcons.Home,
                            label = Str.nav_home(),
                        )
                        NavigationBarItem(
                            selected = isSettingsRoute,
                            onClick = {
                                navController.navigate(Settings) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = MiuixIcons.Settings,
                            label = Str.nav_settings(),
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun SettingsPage(
    onOpenWebLogin: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    bottomPadding: Dp = 0.dp,
) {
    var tokenInput by remember { mutableStateOf("") }
    var loginBusy by remember { mutableStateOf(false) }
    var loginStatus by remember { mutableStateOf<String?>(null) }
    var selfId by remember { mutableStateOf(AppPrefs.selfUserId) }
    var selfInfo by remember { mutableStateOf<PixivUser?>(null) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selfId) {
        selfInfo = if (selfId > 0L) {
            try {
                PixivApi.fetchUser(selfId)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun doLogin() {
        scope.launch {
            loginBusy = true
            val err = PixivAuth.login(tokenInput)
            loginBusy = false
            if (err == null) {
                tokenInput = ""
                selfId = AppPrefs.selfUserId
                loginStatus = PixivAuth.lastLoginUser
                showTokenDialog = false
            } else {
                loginStatus = err
            }
        }
    }

    fun doLogout() {
        PixivAuth.logout()
        tokenInput = ""
        selfId = 0
        selfInfo = null
        loginStatus = null
    }

    PixoTabFrame(title = Str.nav_settings()) { framePadding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = framePadding.calculateTopPadding(), bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    if (selfId <= 0L) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(onClick = { showLoginDialog = true }, modifier = Modifier.fillMaxWidth()) {
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
                            Button(onClick = { doLogout() }) {
                                Text(Str.logout_button())
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenSettings),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Str.settings_title(), style = MiuixTheme.textStyles.body1, modifier = Modifier.weight(1f))
                        Text("›", style = MiuixTheme.textStyles.title3, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
        }

        OverlayDialog(show = showLoginDialog, onDismissRequest = { showLoginDialog = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Str.settings_login(), style = MiuixTheme.textStyles.title3)
                Button(onClick = {
                    showLoginDialog = false
                    onOpenWebLogin()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(Str.web_login())
                }
                Button(onClick = {
                    showLoginDialog = false
                    showTokenDialog = true
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(Str.token_login())
                }
            }
        }
        OverlayDialog(show = showTokenDialog, onDismissRequest = { showTokenDialog = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Str.token_login(), style = MiuixTheme.textStyles.title3)
                Text(
                    Str.token_login_hint(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                TextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it; loginStatus = null },
                    label = Str.refresh_token_label(),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                loginStatus?.let {
                    Text(
                        it,
                        style = MiuixTheme.textStyles.footnote2,
                        color = if (selfId > 0L) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showTokenDialog = false }, modifier = Modifier.weight(1f)) {
                        Text(Str.cancel())
                    }
                    Button(
                        onClick = { doLogin() },
                        enabled = !loginBusy && tokenInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(Str.login_button())
                    }
                }
            }
        }
    }
}

/** 设置页：分类入口卡片（外观 / 调试，点进子页设置）。 */
@Composable
fun AppSettingsPage(
    onOpenAppearance: () -> Unit,
    onOpenDebug: () -> Unit,
    onBack: () -> Unit,
) {
    PixoSubScreen(title = Str.settings_title(), onBack = onBack) { innerPadding ->
        LazyColumn(contentPadding = innerPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 4.dp),
                ) {
                    SettingEntryRow(Str.settings_appearance(), onClick = onOpenAppearance)
                    SettingEntryRow(Str.settings_debug(), onClick = onOpenDebug)
                }
            }
        }
    }
}

/** 设置列表项：标题 + 右箭头。 */
@Composable
private fun SettingEntryRow(title: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MiuixTheme.textStyles.body1, modifier = Modifier.weight(1f))
        Text("›", style = MiuixTheme.textStyles.title3, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

/** 外观设置页：语言 / 主题 / 预见式返回。 */
@Composable
fun AppearancePage(
    themeMode: ColorSchemeMode,
    onThemeModeChange: (ColorSchemeMode) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    val themeOptions = listOf(Str.theme_follow(), Str.theme_dark(), Str.theme_light())
    val languageOptions = listOf(Str.language_follow(), Str.language_zh(), Str.language_en())
    val languageValues = listOf("system", "zh", "en")
    PixoSubScreen(title = Str.settings_appearance(), onBack = onBack) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    OverlayDropdownPreference(
                        items = languageOptions,
                        selectedIndex = languageValues.indexOf(language).coerceAtLeast(0),
                        title = Str.settings_language(),
                        summary = languageOptions[languageValues.indexOf(language).coerceAtLeast(0)],
                        onSelectedIndexChange = { onLanguageChange(languageValues[it]) },
                    )
                    OverlayDropdownPreference(
                        items = themeOptions,
                        selectedIndex = themeModes.indexOf(themeMode).coerceAtLeast(0),
                        title = Str.theme_mode(),
                        summary = themeOptions[themeModes.indexOf(themeMode).coerceAtLeast(0)],
                        onSelectedIndexChange = { onThemeModeChange(themeModes[it]) },
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
    PixoSubScreen(title = Str.settings_debug(), onBack = onBack) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
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
        }
    }
}