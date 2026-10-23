package app.pixo

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(
    onOpenWebLogin: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenFollows: (Long, String) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
    bottomPadding: Dp = 0.dp,
) {
    // 以设置版本号为 key：导入登录信息后账号卡片立即刷新，不用重启
    var selfId by remember(SettingsRevision.current) { mutableStateOf(AppPrefs.selfUserId) }
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
    val listState = rememberLazyListState()
    TrackBottomBarScroll { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
    PixoTabFrame(title = Str.nav_settings(), scrollBehavior = scrollBehavior) { framePadding ->
        LazyColumn(
            state = listState,
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
                        title = Str.history_title(),
                        onClick = onOpenHistory,
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
    onOpenUpdate: () -> Unit,
    onOpenLogin: () -> Unit,
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
                    SettingEntryRow(Str.settings_login(), onClick = onOpenLogin)
                    SettingEntryRow(Str.settings_appearance(), onClick = onOpenAppearance)
                    SettingEntryRow(Str.settings_content(), onClick = onOpenContent)
                    SettingEntryRow(Str.settings_data(), onClick = onOpenData)
                    SettingEntryRow(Str.update(), onClick = onOpenUpdate)
                    SettingEntryRow(Str.settings_debug(), onClick = onOpenDebug)
                }
            }
        }
    }
}


/** 设置列表项：标题 + 右箭头（MIUIX ArrowPreference）。 */
@Composable
internal fun SettingEntryRow(title: String, onClick: () -> Unit) {
    ArrowPreference(title = title, onClick = onClick)
}


/** 设置分区标题：小号 + 中等字重 + 次要色（分区标记）。 */
/**
 * 设置分区标题：使用 Miuix 的 [SmallTitle]（`subtitle` 字阶 + `onBackgroundVariant`），
 * 只把内边距调成本项目既有节奏（20/6dp），与 12dp 的 Card 分组对齐。
 */
@Composable
internal fun PixoDropdownPreference(
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
