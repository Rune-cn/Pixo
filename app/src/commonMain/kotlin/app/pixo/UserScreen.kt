package app.pixo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 作者主页：资料头 + 插画/小说作品列表。 */
@Composable
fun UserScreen(
    userId: Long,
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenFollows: (Long, String) -> Unit,
    waterfall: Boolean = false,
) {
    // 注意：作者主页没有走 rememberPagedFeed —— 它一次要并发取「插画 / 漫画 / 小说」
    // 三个标签页的数据，并且用户资料与作品列表分属两条通道（Token / 网页），
    // 与其它单列表页面的形状不同；这里保留针对性实现，避免为统一而牺牲首屏体验。
    val illustKey = "userIllusts:$userId"
    val mangaKey = "userManga:$userId"
    val novelKey = "userNovels:$userId"
    val cachedUser = remember(userId) { FeedCache.userProfiles[userId] }
    var user by remember { mutableStateOf(cachedUser) }
    var kind by rememberSaveable { mutableStateOf("illust") }
    var illustItems by remember(userId, SettingsRevision.current) {
        mutableStateOf<List<PixivWork>?>(FeedCache.illust[illustKey] as? List<PixivWork>)
    }
    var mangaItems by remember(userId, SettingsRevision.current) {
        mutableStateOf<List<PixivWork>?>(FeedCache.illust[mangaKey] as? List<PixivWork>)
    }
    var novelItems by remember(userId) { mutableStateOf<List<PixivNovel>?>(FeedCache.novels[novelKey] as? List<PixivNovel>) }
    var loading by remember { mutableStateOf(illustItems == null || novelItems == null) }
    var loadingMore by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var followBusy by remember { mutableStateOf(false) }
    var followMenu by remember { mutableStateOf(false) }
    var followToast by remember { mutableStateOf<String?>(null) }
    var avatarPreview by remember { mutableStateOf(false) }
    var bioDialog by remember { mutableStateOf<String?>(null) }
    var copyMenu by remember { mutableStateOf<String?>(null) }
    var idCopyMenu by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var idSelect by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val clipboard = LocalClipboardManager.current
    val copyAllLabel = Str.copy_all()
    val sFollowSuccess = Str.follow_success()
    val sUnfollowSuccess = Str.unfollow_success()
    val sFollowFailed = Str.follow_failed()
    val cacheKey = when (kind) {
        "novel" -> novelKey
        "manga" -> mangaKey
        else -> illustKey
    }
    val listState = key(cacheKey) { rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0) }

    suspend fun fetchMore(kind: String, offset: Int): List<Any> =
        when (kind) {
            "novel" -> PixivApi.fetchUserNovels(userId, offset, 30)
            "manga" -> PixivApi.fetchUserIllusts(userId, offset, 30, "manga")
            else -> PixivApi.fetchUserIllusts(userId, offset, 30, "illust")
        }

    LaunchedEffect(userId, retryKey) {
        // 全部数据已有缓存：直接返回，不置 loading、不重新拉取（避免退出详情返回时主页刷新闪烁）
        val tokenMode = PixivAuth.isTokenMode
        val willFetch = (user == null) ||
            if (tokenMode) (illustItems == null || novelItems == null || mangaItems == null)
            else (illustItems == null || novelItems == null)
        if (retryKey == 0 && !willFetch) return@LaunchedEffect
        loading = true
        error = null
        try {
            if (user == null) user = PixivApi.fetchUser(userId)
            val tokenMode = PixivAuth.isTokenMode
            if (tokenMode) {
                if (illustItems == null) {
                    val list = PixivApi.fetchUserIllusts(userId, 0, 30)
                    FeedCache.illust[illustKey] = list
                    illustItems = list
                }
                if (novelItems == null) {
                    val list = PixivApi.fetchUserNovels(userId, 0, 30)
                    FeedCache.novels[novelKey] = list
                    novelItems = list
                }
                if (mangaItems == null) {
                    val list = PixivApi.fetchUserIllusts(userId, 0, 30, "manga")
                    FeedCache.illust[mangaKey] = list
                    mangaItems = list
                }
            } else {
                // 网页端没有稳定的作者作品分页接口，用 works/latest 展示最新作品
                val w = PixivApi.fetchUserWorks(userId)
                if (illustItems == null) {
                    FeedCache.illust[illustKey] = w.illusts
                    illustItems = w.illusts
                }
                if (novelItems == null) {
                    FeedCache.novels[novelKey] = w.novels
                    novelItems = w.novels
                }
                if (mangaItems == null) {
                    val manga = w.illusts.filter { it.illustType == 1 }
                    FeedCache.illust[mangaKey] = manga
                    mangaItems = manga
                }
            }
            hasMore = tokenMode
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "user works load failed", e)
        }
        loading = false
    }

    fun refresh() {
        if (refreshing) return
        scope.launch {
            refreshing = true
            try {
                val u = PixivApi.fetchUser(userId)
                FeedCache.userProfiles[userId] = u
                user = u
                val tokenMode = PixivAuth.isTokenMode
                if (tokenMode) {
                    val il = PixivApi.fetchUserIllusts(userId, 0, 30)
                    FeedCache.illust[illustKey] = il
                    illustItems = il
                    val ma = PixivApi.fetchUserIllusts(userId, 0, 30, "manga")
                    FeedCache.illust[mangaKey] = ma
                    mangaItems = ma
                    val nl = PixivApi.fetchUserNovels(userId, 0, 30)
                    FeedCache.novels[novelKey] = nl
                    novelItems = nl
                    hasMore = true
                } else {
                    val w = PixivApi.fetchUserWorks(userId)
                    FeedCache.illust[illustKey] = w.illusts
                    illustItems = w.illusts
                    val manga = w.illusts.filter { it.illustType == 1 }
                    FeedCache.illust[mangaKey] = manga
                    mangaItems = manga
                    FeedCache.novels[novelKey] = w.novels
                    novelItems = w.novels
                    hasMore = false
                }
                error = null
            } catch (e: Exception) {
                error = e.friendlyMessage()
                logError("Pixo", "refresh user failed", e)
            }
            refreshing = false
        }
    }

    fun doFollow(restrict: Int) {
        if (followBusy) return
        scope.launch {
            followBusy = true
            try {
                // 已关注时切换公开/私密：先取消再按新分级关注
                if (user?.isFollowed == true) {
                    runCatching { PixivApi.unfollowUser(userId) }
                }
                PixivApi.followUser(userId, restrict)
                val u = user?.copy(isFollowed = true)
                user = u
                u?.let { FeedCache.userProfiles[userId] = it }
                followToast = sFollowSuccess
            } catch (e: Exception) {
                logError("Pixo", "follow failed", e)
                followToast = sFollowFailed + "：" + (e.message ?: "")
            }
            followBusy = false
        }
    }

    fun doUnfollow() {
        if (followBusy) return
        scope.launch {
            followBusy = true
            try {
                PixivApi.unfollowUser(userId)
                val u = user?.copy(isFollowed = false)
                user = u
                u?.let { FeedCache.userProfiles[userId] = it }
                followToast = sUnfollowSuccess
            } catch (e: Exception) {
                logError("Pixo", "unfollow failed", e)
                followToast = sFollowFailed + "：" + (e.message ?: "")
            }
            followBusy = false
        }
    }

    val currentList: List<Any> = when (kind) {
        "novel" -> novelItems.orEmpty()
        "manga" -> mangaItems.orEmpty()
        else -> illustItems.orEmpty()
    }

    fun loadMore() {
        if (loadingMore || !hasMore || loading) return
        scope.launch {
            loadingMore = true
            try {
                val more = fetchMore(kind, currentList.size)
                val seen = HashSet(currentList.map { (it as? PixivWork)?.id ?: (it as PixivNovel).id })
                val merged = currentList + more.filter {
                    val id = (it as? PixivWork)?.id ?: (it as PixivNovel).id
                    id !in seen
                }
                if (kind == "novel") {
                    FeedCache.novels[novelKey] = merged.filterIsInstance<PixivNovel>()
                    novelItems = merged.filterIsInstance<PixivNovel>()
                } else if (kind == "manga") {
                    FeedCache.illust[mangaKey] = merged.filterIsInstance<PixivWork>()
                    mangaItems = merged.filterIsInstance<PixivWork>()
                } else {
                    FeedCache.illust[illustKey] = merged.filterIsInstance<PixivWork>()
                    illustItems = merged.filterIsInstance<PixivWork>()
                }
                hasMore = more.isNotEmpty()
            } catch (_: Exception) {
            }
            loadingMore = false
        }
    }

    LaunchedEffect(listState, currentList.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && last >= total - 3
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }
    SideEffect { FeedCache.indexes[cacheKey] = listState.firstVisibleItemIndex }

    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.author_page(), onBack = onBack, scrollBehavior = scrollBehavior, actions = {
        var showMenu by remember { mutableStateOf(false) }
        IconButton(onClick = { showMenu = true }) {
            Icon(imageVector = MiuixIcons.More, contentDescription = Str.more_actions())
        }
        UserActionDialog(show = showMenu, userId = userId, onDismiss = { showMenu = false })
    }) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            when {
                loading && user == null && currentList.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    LoadingState()
                }
                error != null && user == null && currentList.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    ErrorState(error ?: "", onRetry = { retryKey++ })
                }
                else -> PullToRefresh(
                    isRefreshing = refreshing,
                    onRefresh = { refresh() },
                    refreshTexts = refreshTexts(),
                    contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
                ) {
            if (waterfall && kind != "novel") {
                StaggeredWorkGrid(
                    works = if (kind == "manga") mangaItems.orEmpty() else illustItems.orEmpty(),
                    onOpenDetail = { onOpenDetail(it, "illust") },
                    onOpenUser = onOpenUser,
                    modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = innerPadding,
                    loadingMore = loadingMore,
                    hasMore = hasMore,
                    onLoadMore = { loadMore() },
                    header = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UserHeader(
                                user = user,
                                followBusy = followBusy,
                                canFollow = userId != AppPrefs.selfUserId,
                                onFollowClick = { followMenu = true },
                                onPreviewAvatar = { avatarPreview = true },
                                onCopyBio = { bio -> copyMenu = bio },
                                onCopyId = { account, id -> idCopyMenu = account to id },
                                onOpenFollowing = { onOpenFollows(userId, "following") },
                                onOpenWorks = { kind = "illust" },
                            )
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ModeChip("illust", Str.illust(), kind) { kind = "illust" }
                                ModeChip("manga", Str.manga(), kind) { kind = "manga" }
                                ModeChip("novel", Str.novel(), kind) { kind = "novel" }
                            }
                        }
                    },
                    error = error,
                    onRetry = { retryKey++ },
                    loading = loading && currentList.isEmpty(),
                    emptyHint = { EmptyHint() },
                )
            } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = innerPadding,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                        overscrollEffect = null,
                    ) {
                        item {
                            UserHeader(
                                user = user,
                                followBusy = followBusy,
                                canFollow = userId != AppPrefs.selfUserId,
                                onFollowClick = { followMenu = true },
                                onPreviewAvatar = { avatarPreview = true },
                                onCopyBio = { bio -> copyMenu = bio },
                                onCopyId = { account, id -> idCopyMenu = account to id },
                                onOpenFollowing = { onOpenFollows(userId, "following") },
                                onOpenWorks = { kind = "illust" },
                            )
                        }
                        item {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ModeChip("illust", Str.illust(), kind) { kind = "illust" }
                                ModeChip("manga", Str.manga(), kind) { kind = "manga" }
                                ModeChip("novel", Str.novel(), kind) { kind = "novel" }
                            }
                        }
                        if (currentList.isEmpty() && !loading) {
                            item { EmptyHint() }
                        } else if (kind == "novel") {
                            items(novelItems.orEmpty(), key = { it.id }) { n ->
                                NovelRow(n, onClick = { onOpenDetail(n.id, "novel") }, onOpenUser = onOpenUser)
                            }
                            item { EndItem(loadingMore, hasMore, onRetry = {}) }
                        } else if (kind == "manga") {
                            items(mangaItems.orEmpty(), key = { it.id }) { w ->
                                PixivWorkRow(w, onClick = { onOpenDetail(w.id, "illust") }, onOpenUser = onOpenUser)
                            }
                            item { EndItem(loadingMore, hasMore, onRetry = {}) }
                        } else {
                            items(illustItems.orEmpty(), key = { it.id }) { w ->
                                PixivWorkRow(w, onClick = { onOpenDetail(w.id, "illust") }, onOpenUser = onOpenUser)
                            }
                            item { EndItem(loadingMore, hasMore, onRetry = {}) }
                        }
                    }
            }
                }
            }

                OverlayDialog(show = copyMenu != null, onDismissRequest = { copyMenu = null }) {
                    val menuText = copyMenu.orEmpty()
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Str.copy_title(), style = MiuixTheme.textStyles.title3)
                        Button(onClick = {
                            clipboard.setText(AnnotatedString(menuText))
                            copyMenu = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(Str.copy_all())
                        }
                        Button(onClick = {
                            bioDialog = menuText
                            copyMenu = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(Str.copy_select())
                        }
                    }
                }
                OverlayDialog(show = bioDialog != null, onDismissRequest = { bioDialog = null }) {
                    val text = bioDialog.orEmpty()
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Str.copy_title(), style = MiuixTheme.textStyles.title3)
                        SelectionContainer {
                            Text(text, style = MiuixTheme.textStyles.body2)
                        }
                    }
                }
                OverlayDialog(show = idCopyMenu != null, onDismissRequest = { idCopyMenu = null }) {
                    val menu = idCopyMenu ?: return@OverlayDialog
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Str.copy_title(), style = MiuixTheme.textStyles.title3)
                        Button(onClick = {
                            clipboard.setText(AnnotatedString("@" + menu.first))
                            idCopyMenu = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(Str.copy_account())
                        }
                        Button(onClick = {
                            clipboard.setText(AnnotatedString(menu.second.toString()))
                            idCopyMenu = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(Str.copy_user_id())
                        }
                        Button(onClick = {
                            idSelect = menu
                            idCopyMenu = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(Str.copy_select())
                        }
                    }
                }
                OverlayDialog(show = idSelect != null, onDismissRequest = { idSelect = null }) {
                    val menu = idSelect ?: return@OverlayDialog
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Str.copy_title(), style = MiuixTheme.textStyles.title3)
                        SelectionContainer {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("@" + menu.first, style = MiuixTheme.textStyles.body2)
                                Text(Str.user_id_label() + "：" + menu.second, style = MiuixTheme.textStyles.body2)
                            }
                        }
                    }
                }
                // 关注操作：未关注选公开/私密；已关注可切换分级或取消
                OverlayDialog(show = followMenu, onDismissRequest = { followMenu = false }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Str.follow(), style = MiuixTheme.textStyles.title3)
                        Button(onClick = {
                            followMenu = false
                            doFollow(0)
                        }, modifier = Modifier.fillMaxWidth()) { Text(Str.follow_public()) }
                        Button(onClick = {
                            followMenu = false
                            doFollow(1)
                        }, modifier = Modifier.fillMaxWidth()) { Text(Str.follow_private()) }
                        if (user?.isFollowed == true) {
                            Button(onClick = {
                                followMenu = false
                                doUnfollow()
                            }, modifier = Modifier.fillMaxWidth()) { Text(Str.unfollow()) }
                        }
                        Button(onClick = { followMenu = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
                    }
                }
                BottomToast(followToast)
                val previewUser = user
                if (avatarPreview && previewUser != null) {
                    AvatarPreview(url = previewUser.avatar, onClose = { avatarPreview = false })
                }
        }
    }
}


@Composable
private fun UserHeader(
    user: PixivUser?,
    followBusy: Boolean = false,
    canFollow: Boolean = true,
    onFollowClick: () -> Unit = {},
    onPreviewAvatar: () -> Unit = {},
    onCopyBio: (String) -> Unit = {},
    onCopyId: (account: String, id: Long) -> Unit = { _, _ -> },
    onOpenFollowing: () -> Unit = {},
    onOpenWorks: () -> Unit = {},
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 基本信息卡
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                AuthorAvatar(user?.avatar.orEmpty(), 68.dp, user?.id ?: 0, onOpenUser = { onPreviewAvatar() })
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user?.name.orEmpty(), style = MiuixTheme.textStyles.title3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (user?.premium == true) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Premium",
                                style = MiuixTheme.textStyles.footnote1,
                                color = Color(0xFFE0A72B),
                            )
                        }
                    }
                    user?.account?.takeIf { it.isNotBlank() }?.let { account ->
                        Text(
                            "@" + account,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onCopyId(account, user.id) },
                                )
                                .padding(vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                if (canFollow) {
                    Button(
                        onClick = onFollowClick,
                        enabled = !followBusy,
                        modifier = Modifier.heightIn(min = 36.dp),
                    ) {
                        Text(
                            if (user?.isFollowed == true) Str.followed() else Str.follow(),
                            style = MiuixTheme.textStyles.footnote2,
                        )
                    }
                }
            }
        }
        // 关注统计（官方 App API 没有粉丝数字段，粉丝项已移除）
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserStatCard(Str.following(), user?.followingCount, Modifier.weight(1f), onOpenFollowing)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserStatCard(Str.illust(), user?.totalIllusts, Modifier.weight(1f), onOpenWorks)
            UserStatCard(Str.manga(), user?.totalManga, Modifier.weight(1f), onOpenWorks)
            UserStatCard(Str.novel(), user?.totalNovels, Modifier.weight(1f), onOpenWorks)
        }
        // 简介卡：长按可复制（全部或部分）
        user?.comment?.takeIf { it.isNotBlank() }?.let { bio ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .combinedClickable(onClick = {}, onLongClick = { onCopyBio(bio) }),
            ) {
                Box(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(bio, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
    }
}


/** 关注/粉丝列表页。[kind] = "following" / "followers"。 */
@Composable
fun FollowsScreen(
    userId: Long,
    kind: String,
    onBack: () -> Unit,
    onOpenUser: (Long) -> Unit,
) {
    val isFollowing = kind == "following"
    var currentRestrict by remember { mutableStateOf("show") }
    var items by remember { mutableStateOf<List<PixivUser>?>(null) }
    var total by remember { mutableStateOf<Int?>(null) }
    var nextUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val label = if (isFollowing) Str.following() else Str.follower()

    suspend fun fetchPage(offset: Int, cursor: String?): PixivRelationPage =
        if (isFollowing) PixivApi.fetchUserFollowing(userId, offset, cursor, currentRestrict)
        else PixivApi.fetchUserFollowers(userId, offset, cursor)

    LaunchedEffect(userId, kind, currentRestrict, retryKey) {
        loading = items == null
        error = null
        try {
            val page = fetchPage(0, null)
            items = page.users
            total = page.total
            nextUrl = page.nextUrl
            hasMore = page.hasMore
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "follows load failed", e)
        }
        loading = false
    }

    fun loadMore() {
        if (loadingMore || !hasMore || loading) return
        scope.launch {
            loadingMore = true
            try {
                val cur = items.orEmpty()
                val page = fetchPage(cur.size, nextUrl)
                val seen = HashSet(cur.map { it.id }.toSet())
                val merged = cur + page.users.filter { it.id !in seen }
                items = merged
                nextUrl = page.nextUrl
                total = page.total ?: total
                hasMore = page.hasMore
            } catch (_: Exception) {
            }
            loadingMore = false
        }
    }

    LaunchedEffect(listState, items?.size ?: 0) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }

    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(
        title = if (isFollowing) Str.following() + (total?.let { " $it" } ?: "") else label + (total?.let { " $it" } ?: ""),
        onBack = onBack,
        scrollBehavior = scrollBehavior,
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            when {
                loading && items == null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    LoadingState()
                }
                error != null && items == null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    ErrorState(error ?: "", onRetry = { retryKey++ })
                }
                else -> {
                    val list = items.orEmpty()
                    if (list.isEmpty() && !loading) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding(),
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                            overscrollEffect = null,
                        ) {
                            if (isFollowing) {
                                item("followRestrict") {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        ModeChip("show", Str.following_public(), currentRestrict) { currentRestrict = "show" }
                                        ModeChip("hide", Str.following_private(), currentRestrict) { currentRestrict = "hide" }
                                    }
                                }
                            }
                            item {
                                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        if (isFollowing) Str.empty_following() else Str.empty_follower(),
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding(),
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                            overscrollEffect = null,
                        ) {
                            if (isFollowing) {
                                item("followRestrict") {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        ModeChip("show", Str.following_public(), currentRestrict) { currentRestrict = "show" }
                                        ModeChip("hide", Str.following_private(), currentRestrict) { currentRestrict = "hide" }
                                    }
                                }
                            }
                            items(list, key = { "${currentRestrict}:${it.id}" }) { u ->
                                FollowUserRow(u, onClick = { onOpenUser(u.id) })
                            }
                            item { EndItem(loadingMore, hasMore, onRetry = { retryKey++ }) }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun FollowUserRow(user: PixivUser, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuthorAvatar(user.avatar, 44.dp, user.id, { onClick() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, style = MiuixTheme.textStyles.body2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (user.account.isNotBlank()) {
                    Text(
                        "@" + user.account,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (user.comment.isNotBlank()) {
                    Text(
                        user.comment,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
