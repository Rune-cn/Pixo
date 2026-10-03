package app.pixo

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Weeks
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 内存级分页/滚动缓存：从详情返回时恢复到原滚动位置，详情等避免重复加载。 */
object FeedCache {
    val illust = HashMap<String, List<PixivWork>>()
    val novels = HashMap<String, List<PixivNovel>>()
    val indexes = HashMap<String, Int>()

    /** 详情与作品分页：按作品 id 缓存，重开不再请求。 */
    val illustDetail = HashMap<Long, PixivIllustDetail>()
    val novelDetail = HashMap<Long, PixivNovelDetail>()
    val illustPages = HashMap<Long, List<PixivPage>>()

    /** 评论：key = "$kind:$workId"。 */
    val comments = HashMap<String, List<PixivComment>>()

    /** 用户资料与主页作品：按用户 id 缓存。 */
    val userProfiles = HashMap<Long, PixivUser>()
    val userWorksCache = HashMap<Long, PixivUserWorks>()
}

@Composable
private fun refreshTexts() = listOf(
    Str.refresh_pull(),
    Str.refresh_release(),
    Str.refresh_loading(),
    Str.refresh_done(),
)

@Composable
private fun EndItem(loadingMore: Boolean, hasMore: Boolean, onRetry: () -> Unit) {
    when {
        loadingMore -> Box(
            Modifier.fillMaxWidth().padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(Str.load_more(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }

        !hasMore -> Box(
            Modifier.fillMaxWidth().padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(Str.no_more(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

@Composable
fun HomePage(
    onOpenRanking: (String) -> Unit,
    onOpenNovelRanking: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenRecommended: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenBookmarks: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUser: (Long) -> Unit,
    bottomPadding: Dp = 0.dp,
) {
    val cacheKey = "home:feed"
    var items by remember { mutableStateOf<List<PixivWork>?>(FeedCache.illust[cacheKey]) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0)
    var loading by remember { mutableStateOf(items == null) }
    var loadingMore by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val initial = items
    val loggedIn = AppPrefs.cookie.isNotBlank() || PixivAuth.isTokenMode
    LaunchedEffect(cacheKey, retryKey, loggedIn) {
        if (initial != null) return@LaunchedEffect
        loading = true
        error = null
        try {
            val list = PixivApi.fetchTopFeed(0, 30)
            FeedCache.illust[cacheKey] = list
            items = list
            hasMore = list.size >= 30
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load failed", e)
        }
        loading = false
    }

    fun refresh() {
        scope.launch {
            refreshing = true
            try {
                val list = PixivApi.fetchTopFeed(0, 30)
                FeedCache.illust[cacheKey] = list
                items = list
                hasMore = list.size >= 30
                error = null
            } catch (e: Exception) {
                error = e.friendlyMessage()
                logError("Pixo", "load failed", e)
            }
            refreshing = false
        }
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            val n = items?.size ?: 0
            try {
                val more = PixivApi.fetchTopFeed(n, 30)
                val cur = (items ?: emptyList()).toMutableList()
                val seen = HashSet(cur.map { it.id })
                cur.addAll(more.filter { it.id !in seen })
                FeedCache.illust[cacheKey] = cur
                items = cur
                hasMore = more.size >= 30
            } catch (_: Exception) {
            }
            loadingMore = false
        }
    }

    val list = items.orEmpty()
    LaunchedEffect(listState, list.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && last >= total - 3
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }
    SideEffect {
        FeedCache.indexes[cacheKey] = listState.firstVisibleItemIndex
    }

    PixoTabFrame(title = Str.nav_home()) { frame ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = { refresh() },
            refreshTexts = refreshTexts(),
            contentPadding = PaddingValues(top = frame.calculateTopPadding()),
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = frame.calculateTopPadding(), bottom = bottomPadding),
            ) {
            item {
                HomeEntries(onOpenRanking, onOpenNovelRanking, onOpenDiscover, onOpenRecommended, onOpenSearch, onOpenBookmarks)
            }
            item { SmallTitle(text = Str.home_title()) }
            when {
                error != null && list.isEmpty() -> item {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (AppPrefs.cookie.isBlank() && !PixivAuth.isLoggedIn) {
                            Text(Str.need_cookie())
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = Str.need_cookie_hint(),
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onOpenSettings) { Text(Str.go_settings()) }
                        } else {
                            ErrorState(error ?: "", onRetry = { retryKey++ })
                        }
                    }
                }

                loading && list.isEmpty() -> item { LoadingState() }
            }
            if (list.isNotEmpty()) {
                items(list, key = { it.id }) { work ->
                    PixivWorkRow(work, onClick = { onOpenDetail(work.id, "illust") }, onOpenUser = onOpenUser)
                }
                item { EndItem(loadingMore, hasMore, onRetry = {}) }
            }
        }
    }
    }
}

@Composable
private fun HomeEntries(
    onOpenRanking: (String) -> Unit,
    onOpenNovelRanking: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenRecommended: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryCard(MiuixIcons.All, Str.illust_ranking()) { onOpenRanking("daily") }
            EntryCard(MiuixIcons.Notes, Str.novel_ranking()) { onOpenNovelRanking("daily") }
            EntryCard(MiuixIcons.Image, Str.discover()) { onOpenDiscover() }
            EntryCard(MiuixIcons.Promotions, Str.recommended()) { onOpenRecommended() }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryCard(MiuixIcons.Search, Str.search()) { onOpenSearch() }
            EntryCard(MiuixIcons.Favorites, Str.bookmark_public_label()) { onOpenBookmarks("show") }
            EntryCard(MiuixIcons.Lock, Str.bookmark_private_label()) { onOpenBookmarks("hide") }
        }
    }
}

@Composable
fun RowScope.EntryCard(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = label, tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
fun RankingScreen(
    mode: String,
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
) {
    var currentMode by rememberSaveable { mutableStateOf(mode) }
    val cacheKey = "rank:$currentMode"
    var items by remember(cacheKey) { mutableStateOf<List<PixivWork>?>(FeedCache.illust[cacheKey]) }
    val listState = key(cacheKey) { rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0) }
    var loading by remember { mutableStateOf(items == null) }
    var loadingMore by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val initial = items
    LaunchedEffect(cacheKey, retryKey) {
        if (initial != null) return@LaunchedEffect
        loading = true
        error = null
        try {
            val list = PixivApi.fetchRanking(currentMode, 1)
            FeedCache.illust[cacheKey] = list
            items = list
            hasMore = list.size >= 50
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load failed", e)
        }
        loading = false
    }

    fun refresh() {
        scope.launch {
            refreshing = true
            try {
                val list = PixivApi.fetchRanking(currentMode, 1)
                FeedCache.illust[cacheKey] = list
                items = list
                hasMore = list.size >= 50
                error = null
            } catch (e: Exception) {
                error = e.friendlyMessage()
                logError("Pixo", "load failed", e)
            }
            refreshing = false
        }
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            val page = ((items?.size ?: 0) / 50) + 1
            try {
                val more = PixivApi.fetchRanking(currentMode, page)
                val cur = (items ?: emptyList()).toMutableList()
                val seen = HashSet(cur.map { it.id })
                cur.addAll(more.filter { it.id !in seen })
                items = cur
                FeedCache.illust[cacheKey] = cur
                hasMore = more.size >= 50
            } catch (_: Exception) {
            }
            loadingMore = false
        }
    }

    val list = items.orEmpty()
    LaunchedEffect(listState, list.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && last >= total - 3
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }
    SideEffect { FeedCache.indexes[cacheKey] = listState.firstVisibleItemIndex }

    PixoSubScreen(title = Str.ranking(), onBack = onBack) { innerPadding ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = { refresh() },
            refreshTexts = refreshTexts(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeChip("daily", Str.daily(), currentMode) { currentMode = "daily" }
                        ModeChip("weekly", Str.weekly(), currentMode) { currentMode = "weekly" }
                        ModeChip("monthly", Str.monthly(), currentMode) { currentMode = "monthly" }
                    }
                }
                when {
                    loading && list.isEmpty() -> item { LoadingState() }
                    error != null && list.isEmpty() -> item { ErrorState(error ?: "", onRetry = { retryKey++ }) }
                }
                if (list.isNotEmpty()) {
                    items(list, key = { it.id }) { work ->
                        PixivWorkRow(work, onClick = { onOpenDetail(work.id, "illust") }, onOpenUser = onOpenUser)
                    }
                    item { EndItem(loadingMore, hasMore, onRetry = {}) }
                }
            }
        }
    }
}

@Composable
fun NovelRankingScreen(
    mode: String,
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
) {
    var currentMode by rememberSaveable { mutableStateOf(mode) }
    val cacheKey = "novelRank:$currentMode"
    var items by remember(cacheKey) { mutableStateOf<List<PixivNovel>?>(FeedCache.novels[cacheKey]) }
    val listState = key(cacheKey) { rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0) }
    var loading by remember { mutableStateOf(items == null) }
    var loadingMore by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val initial = items
    LaunchedEffect(cacheKey, retryKey) {
        if (initial != null) return@LaunchedEffect
        loading = true
        error = null
        try {
            val list = PixivApi.fetchNovelRanking(currentMode, 1)
            FeedCache.novels[cacheKey] = list
            items = list
            hasMore = list.size >= 50
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load failed", e)
        }
        loading = false
    }

    fun refresh() {
        scope.launch {
            refreshing = true
            try {
                val list = PixivApi.fetchNovelRanking(currentMode, 1)
                FeedCache.novels[cacheKey] = list
                items = list
                hasMore = list.size >= 50
                error = null
            } catch (e: Exception) {
                error = e.friendlyMessage()
                logError("Pixo", "load failed", e)
            }
            refreshing = false
        }
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            val page = ((items?.size ?: 0) / 50) + 1
            try {
                val more = PixivApi.fetchNovelRanking(currentMode, page)
                val cur = (items ?: emptyList()).toMutableList()
                val seen = HashSet(cur.map { it.id })
                cur.addAll(more.filter { it.id !in seen })
                items = cur
                FeedCache.novels[cacheKey] = cur
                hasMore = more.size >= 50
            } catch (_: Exception) {
            }
            loadingMore = false
        }
    }

    val list = items.orEmpty()
    LaunchedEffect(listState, list.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && last >= total - 3
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }
    SideEffect { FeedCache.indexes[cacheKey] = listState.firstVisibleItemIndex }

    PixoSubScreen(title = Str.novel_ranking(), onBack = onBack) { innerPadding ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = { refresh() },
            refreshTexts = refreshTexts(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeChip("daily", Str.daily(), currentMode) { currentMode = "daily" }
                        ModeChip("weekly", Str.weekly(), currentMode) { currentMode = "weekly" }
                        ModeChip("monthly", Str.monthly(), currentMode) { currentMode = "monthly" }
                    }
                }
                when {
                    loading && list.isEmpty() -> item { LoadingState() }
                    error != null && list.isEmpty() -> item { ErrorState(error ?: "", onRetry = { retryKey++ }) }
                }
                if (list.isNotEmpty()) {
                    items(list, key = { it.id }) { n ->
                        NovelRow(n, onClick = { onOpenDetail(n.id, "novel") }, onOpenUser = onOpenUser)
                    }
                    item { EndItem(loadingMore, hasMore, onRetry = {}) }
                }
            }
        }
    }
}

@Composable
fun BookmarksScreen(
    restrict: String,
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
) {
    val cacheKey = "bookmarks:$restrict"
    var items by remember(cacheKey) { mutableStateOf<List<PixivWork>?>(FeedCache.illust[cacheKey]) }
    val listState = key(cacheKey) { rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0) }
    var offset by remember { mutableIntStateOf(items?.size ?: 0) }
    var loading by remember { mutableStateOf(items == null) }
    var loadingMore by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val initial = items
    LaunchedEffect(cacheKey, retryKey) {
        if (initial != null && initial.isNotEmpty()) return@LaunchedEffect
        loading = true
        error = null
        try {
            val list = PixivApi.fetchIllustBookmarks(restrict, 0, 24)
            FeedCache.illust[cacheKey] = list
            items = list
            offset = list.size
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load failed", e)
        }
        loading = false
    }

    fun refresh() {
        scope.launch {
            refreshing = true
            try {
                val list = PixivApi.fetchIllustBookmarks(restrict, 0, 24)
                FeedCache.illust[cacheKey] = list
                items = list
                offset = list.size
                error = null
            } catch (e: Exception) {
                error = e.friendlyMessage()
                logError("Pixo", "load failed", e)
            }
            refreshing = false
        }
    }

    fun loadMore() {
        if (loadingMore) return
        scope.launch {
            loadingMore = true
            try {
                val more = PixivApi.fetchIllustBookmarks(restrict, offset, 24)
                val cur = (items ?: emptyList()).toMutableList()
                val seen = HashSet(cur.map { it.id })
                cur.addAll(more.filter { it.id !in seen })
                items = cur
                FeedCache.illust[cacheKey] = cur
                offset = cur.size
            } catch (_: Exception) {
            }
            loadingMore = false
        }
    }

    val list = items.orEmpty()
    LaunchedEffect(listState, list.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && last >= total - 3
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }
    SideEffect { FeedCache.indexes[cacheKey] = listState.firstVisibleItemIndex }

    val title = if (restrict == "show") Str.bookmark_public_label() else Str.bookmark_private_label()
    PixoSubScreen(title = title, onBack = onBack) { innerPadding ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = { refresh() },
            refreshTexts = refreshTexts(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    loading && list.isEmpty() -> item { LoadingState() }
                    error != null && list.isEmpty() -> item {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ErrorState(error ?: "", onRetry = { retryKey++ })
                            Spacer(Modifier.height(4.dp))
                            Button(onClick = { openUrlInBrowser("https://www.pixiv.net/bookmark.php") }) {
                                Text(Str.open_in_browser())
                            }
                        }
                    }
                }
                if (list.isNotEmpty()) {
                    items(list, key = { it.id }) { work ->
                        PixivWorkRow(work, onClick = { onOpenDetail(work.id, "illust") }, onOpenUser = onOpenUser)
                    }
                    item { EndItem(loadingMore, hasMore = true, onRetry = {}) }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    initialQuery: String,
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
) {
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var submitted by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var kind by rememberSaveable { mutableStateOf("illust") }
    // 有结果（返回/保留查询）时收起输入框，避免自动弹出输入法
    var expanded by rememberSaveable(initialQuery) { mutableStateOf(initialQuery.isBlank()) }
    val focusManager = LocalFocusManager.current
    val cacheKey = "search:$submitted:$kind"
    // 从缓存恢复，返回时保留原列表与滚动位置
    var items by remember(cacheKey) {
        mutableStateOf<List<Any>?>(
            if (kind == "novel") FeedCache.novels[cacheKey] else FeedCache.illust[cacheKey],
        )
    }
    val listState = key(cacheKey) { rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(submitted.isNotBlank() && items == null) }
    var loadingMore by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val current = items
    LaunchedEffect(cacheKey, retryKey) {
        if (submitted.isBlank()) {
            items = null
            return@LaunchedEffect
        }
        if (current != null) return@LaunchedEffect
        loading = true
        error = null
        try {
            val list: List<Any> = if (kind == "novel") PixivApi.searchNovels(submitted, 1)
            else PixivApi.searchArtworks(submitted, 1)
            FeedCache.illust[cacheKey] = list.filterIsInstance<PixivWork>()
            FeedCache.novels[cacheKey] = list.filterIsInstance<PixivNovel>()
            items = list
            page = 1
            hasMore = list.size >= 60
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load failed", e)
        }
        loading = false
    }

    fun refresh() {
        scope.launch {
            if (submitted.isBlank()) return@launch
            refreshing = true
            try {
                val list: List<Any> = if (kind == "novel") PixivApi.searchNovels(submitted, 1)
                else PixivApi.searchArtworks(submitted, 1)
                items = list
                FeedCache.illust[cacheKey] = list.filterIsInstance<PixivWork>()
                FeedCache.novels[cacheKey] = list.filterIsInstance<PixivNovel>()
                page = 1
                error = null
            } catch (e: Exception) {
                error = e.friendlyMessage()
                logError("Pixo", "load failed", e)
            }
            refreshing = false
        }
    }

    fun loadMore() {
        if (loadingMore || !hasMore || submitted.isBlank()) return
        scope.launch {
            loadingMore = true
            val next = page + 1
            try {
                val more: List<Any> = if (kind == "novel") PixivApi.searchNovels(submitted, next)
                else PixivApi.searchArtworks(submitted, next)
                val cur = (items ?: emptyList()).toMutableList()
                val seen = HashSet(cur.map { if (it is PixivWork) it.id else (it as PixivNovel).id })
                cur.addAll(more.filter {
                    val id = if (it is PixivWork) it.id else (it as PixivNovel).id
                    id !in seen
                })
                items = cur
                FeedCache.illust[cacheKey] = cur.filterIsInstance<PixivWork>()
                FeedCache.novels[cacheKey] = cur.filterIsInstance<PixivNovel>()
                page = next
                hasMore = more.size >= 60
            } catch (_: Exception) {
            }
            loadingMore = false
        }
    }

    val list = items.orEmpty()
    LaunchedEffect(listState, list.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && last >= total - 3
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }
    SideEffect { FeedCache.indexes[cacheKey] = listState.firstVisibleItemIndex }

    fun openItem(id: Long, kindOf: String) {
        focusManager.clearFocus()
        onOpenDetail(id, kindOf)
    }

    PixoSubScreen(title = Str.search(), onBack = onBack) { innerPadding ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = { refresh() },
            refreshTexts = refreshTexts(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {
                            submitted = it
                            expanded = false
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        label = Str.search_pixiv(),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeChip("illust", Str.illust(), kind) { kind = "illust" }
                        ModeChip("novel", Str.novel(), kind) { kind = "novel" }
                    }
                }
                when {
                    submitted.isBlank() -> item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text(Str.search_hint(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }

                    loading && list.isEmpty() -> item { LoadingState() }
                    error != null && list.isEmpty() -> item { ErrorState(error ?: "", onRetry = { retryKey++ }) }
                }
                if (list.isNotEmpty()) {
                    items(list.size, key = { i ->
                        val it = list[i]
                        if (it is PixivWork) it.id else (it as PixivNovel).id
                    }) { i ->
                        val it = list[i]
                        if (it is PixivWork) PixivWorkRow(it, onClick = { openItem(it.id, "illust") }, onOpenUser = onOpenUser)
                        else NovelRow(it as PixivNovel, onClick = { openItem(it.id, "novel") }, onOpenUser = onOpenUser)
                    }
                    item { EndItem(loadingMore, hasMore, onRetry = {}) }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(
    workId: Long,
    kind: String,
    onBack: () -> Unit,
    onSearchTag: (String) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenComments: (Long, String) -> Unit,
) {
    PixoSubScreen(title = "", onBack = onBack) { innerPadding ->
        if (kind == "novel") {
            NovelDetail(workId, innerPadding, onSearchTag, onOpenUser, onOpenComments)
        } else {
            IllustDetail(workId, innerPadding, onSearchTag, onOpenUser, onOpenComments)
        }
    }
}

@Composable
private fun IllustDetail(
    workId: Long,
    innerPadding: PaddingValues,
    onSearchTag: (String) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenComments: (Long, String) -> Unit,
) {
    val cachedDetail = remember(workId) { FeedCache.illustDetail[workId] }
    val cachedPages = remember(workId) { FeedCache.illustPages[workId] }
    var detail by remember { mutableStateOf(cachedDetail) }
    var pages by remember { mutableStateOf(cachedPages.orEmpty()) }
    var loading by remember { mutableStateOf(cachedDetail == null || cachedPages == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var copiedTag by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var bookmarkBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 预取文案（协程内不能用 @Composable 资源访问）
    val sBookmarkedPublic = Str.bookmarked_public()
    val sBookmarkedPrivate = Str.bookmarked_private()
    val sBookmarkRemoved = Str.bookmark_removed()

    LaunchedEffect(workId, retryKey) {
        // 已缓存则直接使用，避免重复加载
        if (FeedCache.illustDetail[workId] != null && FeedCache.illustPages[workId] != null && retryKey == 0) {
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            val d = PixivApi.fetchIllustDetail(workId)
            val p = PixivApi.fetchIllustPages(workId)
            FeedCache.illustDetail[workId] = d
            FeedCache.illustPages[workId] = p
            detail = d
            pages = p
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load failed", e)
        }
        loading = false
    }
    LaunchedEffect(copiedTag) {
        if (copiedTag != null) {
            delay(1200)
            copiedTag = null
        }
    }

    fun toggleBookmark(restrict: Int) {
        if (bookmarkBusy) return
        scope.launch {
            bookmarkBusy = true
            try {
                val cur = detail
                if (restrict < 0) {
                    // 取消收藏
                    PixivApi.removeBookmark("illust", workId, cur?.bookmarkId ?: 0)
                    detail = cur?.copy(isBookmarked = false, bookmarkPrivate = false)
                    feedback = sBookmarkRemoved
                } else {
                    // 切换公开/私密：先取消再重新收藏，兼容网页与 App API
                    if (cur?.isBookmarked == true && cur.bookmarkPrivate != (restrict == 1)) {
                        PixivApi.removeBookmark("illust", workId, cur.bookmarkId)
                    }
                    PixivApi.addBookmark(workId, restrict)
                    detail = cur?.copy(isBookmarked = true, bookmarkPrivate = restrict == 1)
                    feedback = if (restrict == 0) sBookmarkedPublic else sBookmarkedPrivate
                }
            } catch (e: Exception) {
                feedback = "收藏失败：${e.message}"
            }
            detail?.let { FeedCache.illustDetail[workId] = it }
            bookmarkBusy = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            loading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { LoadingState() }
            error != null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                ErrorState(error ?: "", onRetry = { retryKey++ })
            }

            else -> {
                val d = detail
                LazyColumn(contentPadding = innerPadding) {
                    items(pages) { page ->
                        val ctx = LocalPlatformContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(ctx).data(page.url).crossfade(true).build(),
                            contentDescription = d?.title ?: "",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        IllustInfo(
                            d = d,
                            onSearchTag = onSearchTag,
                            onOpenUser = onOpenUser,
                            feedback = feedback,
                            onLongPressTag = { tag -> copiedTag = tag },
                        )
                    }
                }
                copiedTag?.let { tag ->
                    Box(Modifier.fillMaxSize().padding(bottom = 40.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(Str.copied(tag), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceContainer)
                        }
                    }
                }
            }
        }

        var showBookmarkMenu by remember { mutableStateOf(false) }
        FloatingButton(
            onClick = { showBookmarkMenu = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 64.dp),
        ) {
            Icon(imageVector = MiuixIcons.Favorites, contentDescription = Str.bookmark_public_label())
        }
        FloatingButton(
            onClick = { onOpenComments(workId, "illust") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(imageVector = MiuixIcons.Messages, contentDescription = Str.comments())
        }

        OverlayDialog(show = showBookmarkMenu, onDismissRequest = { showBookmarkMenu = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Str.bookmark_public_label(), style = MiuixTheme.textStyles.title3)
                val isBm = detail?.isBookmarked == true
                if (!isBm) {
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(0)
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.bookmark_public()) }
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(1)
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.bookmark_private()) }
                } else {
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(if (detail?.bookmarkPrivate == true) 0 else 1)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (detail?.bookmarkPrivate == true) Str.bookmark_public() else Str.bookmark_private())
                    }
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(-1)
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.unbookmark()) }
                }
                Button(onClick = { showBookmarkMenu = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
            }
        }
    }
}

@Composable
private fun FloatingButton(onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun IllustInfo(
    d: PixivIllustDetail?,
    onSearchTag: (String) -> Unit,
    onOpenUser: (Long) -> Unit,
    feedback: String?,
    onLongPressTag: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(d?.title ?: "", style = MiuixTheme.textStyles.title3)
        Spacer(Modifier.height(10.dp))
        AuthorHeader(name = d?.author.orEmpty(), userId = d?.authorId ?: 0, avatar = d?.avatar.orEmpty(), onOpenUser = onOpenUser)
        Spacer(Modifier.height(10.dp))
        feedback?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.primary)
        }
        if (d?.tags?.isNotEmpty() == true) {
            Spacer(Modifier.height(10.dp))
            val clipboard = LocalClipboardManager.current
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                d.tags.forEach { tag ->
                    TextTagChip(tag = tag, onClick = { onSearchTag(tag) }, onLongClick = {
                        clipboard.setText(AnnotatedString(tag))
                        onLongPressTag(tag)
                    })
                }
            }
        }
    }
}

/** 作者信息行：头像 + 名字，点击进入作者主页。 */
@Composable
private fun AuthorHeader(name: String, userId: Long, avatar: String, onOpenUser: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpenUser(userId) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AuthorAvatar(avatar, 40.dp, userId, onOpenUser)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MiuixTheme.textStyles.body1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Str.author_page(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        Text("›", style = MiuixTheme.textStyles.title3, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

/** 收藏操作：未收藏时提供公开/私人；已收藏时提供切换与取消。 */
@Composable
private fun BookmarkButtons(
    isBookmarked: Boolean,
    bookmarkPrivate: Boolean,
    busy: Boolean,
    onBookmark: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isBookmarked) {
            Button(onClick = { onBookmark(0) }, enabled = !busy) { Text(Str.bookmark_public()) }
            Button(onClick = { onBookmark(1) }, enabled = !busy) { Text(Str.bookmark_private()) }
        } else {
            if (bookmarkPrivate) {
                Button(onClick = { onBookmark(0) }, enabled = !busy) { Text(Str.bookmark_public()) }
            } else {
                Button(onClick = { onBookmark(1) }, enabled = !busy) { Text(Str.bookmark_private()) }
            }
            Button(onClick = { onBookmark(-1) }, enabled = !busy) { Text(Str.unbookmark()) }
        }
    }
}

@Composable
private fun TextTagChip(tag: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text = "#$tag", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceContainer)
    }
}

/**
 * 评论区（独立页面）。
 *
 * MIUI 卡片风格：头像 + 昵称 + 日期 + 内容的评论卡片列表，底部用 MIUI 控件发布评论。
 */
@Composable
fun CommentsScreen(
    workId: Long,
    kind: String,
    onBack: () -> Unit,
    onOpenUser: (Long) -> Unit,
) {
    val cacheKey = "$kind:$workId"
    var comments by remember(cacheKey) { mutableStateOf(FeedCache.comments[cacheKey]) }
    var loading by remember { mutableStateOf(comments == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var posting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackError by remember { mutableStateOf(false) }
    var copyMenu by remember { mutableStateOf<String?>(null) }
    var copySelect by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val sPosted = Str.comment_posted()
    val sFailPrefix = Str.comment_post_failed("")
    val sHint = Str.comment_hint()
    val sSend = Str.comment_send()

    LaunchedEffect(cacheKey, retryKey) {
        // 已缓存则直接使用，避免重复加载
        if (comments != null && retryKey == 0) return@LaunchedEffect
        loading = true
        error = null
        try {
            val list = PixivApi.fetchComments(kind, workId)
            FeedCache.comments[cacheKey] = list
            comments = list
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load comments failed", e)
        }
        loading = false
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || posting) return
        scope.launch {
            posting = true
            try {
                PixivApi.addComment(kind, workId, text)
                val list = PixivApi.fetchComments(kind, workId)
                FeedCache.comments[cacheKey] = list
                comments = list
                draft = ""
                feedback = sPosted
                feedbackError = false
            } catch (e: Exception) {
                feedback = sFailPrefix + (e.message ?: "")
                feedbackError = true
            }
            posting = false
        }
    }

    PixoSubScreen(title = Str.comments(), onBack = onBack) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading && comments == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingState()
                    }
                    error != null && comments == null -> ErrorState(error ?: "", onRetry = { retryKey++ })
                    comments.orEmpty().isEmpty() -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(Str.no_comments(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(comments.orEmpty()) { _, c -> CommentCard(c, onOpenUser) { t -> copyMenu = t } }
                    }
                }
            }
            feedback?.let {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        it,
                        style = MiuixTheme.textStyles.footnote2,
                        color = if (feedbackError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = sHint,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { send() }, enabled = draft.isNotBlank() && !posting) {
                    Text(sSend)
                }
            }
            OverlayDialog(show = copyMenu != null, onDismissRequest = { copyMenu = null }) {
                val menuText = copyMenu.orEmpty()
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Str.copy_title(), style = MiuixTheme.textStyles.title3)
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(menuText))
                        copyMenu = null
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.copy_all()) }
                    Button(onClick = {
                        copySelect = menuText
                        copyMenu = null
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.copy_select()) }
                }
            }
            OverlayDialog(show = copySelect != null, onDismissRequest = { copySelect = null }) {
                val text = copySelect.orEmpty()
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Str.copy_title(), style = MiuixTheme.textStyles.title3)
                    SelectionContainer {
                        Text(text, style = MiuixTheme.textStyles.body2)
                    }
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(text))
                        copySelect = null
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.copy_all()) }
                }
            }
        }
    }
}

/** 单条评论卡片：头像（点击进作者主页）+ 昵称 + 日期 + 内容。 */
@Composable
private fun CommentCard(c: PixivComment, onOpenUser: (Long) -> Unit, onCopy: (String) -> Unit = {}) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            AuthorAvatar(c.avatar, 36.dp, c.userId, onOpenUser)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = c.user.ifBlank { Str.anonymous() },
                        style = MiuixTheme.textStyles.body1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    c.date.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { onCopy(c.content) })) {
                    SelectionContainer {
                        Text(c.content, style = MiuixTheme.textStyles.body2)
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelDetail(
    workId: Long,
    innerPadding: PaddingValues,
    onSearchTag: (String) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenComments: (Long, String) -> Unit,
) {
    val cachedDetail = remember(workId) { FeedCache.novelDetail[workId] }
    var detail by remember { mutableStateOf(cachedDetail) }
    var loading by remember { mutableStateOf(cachedDetail == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var copiedTag by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var bookmarkBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val sBookmarkedPublic = Str.bookmarked_public()
    val sBookmarkedPrivate = Str.bookmarked_private()
    val sBookmarkRemoved = Str.bookmark_removed()

    LaunchedEffect(workId, retryKey) {
        // 已缓存则直接使用，避免重复加载
        if (FeedCache.novelDetail[workId] != null && retryKey == 0) {
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            val d = PixivApi.fetchNovelDetail(workId)
            FeedCache.novelDetail[workId] = d
            detail = d
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load failed", e)
        }
        loading = false
    }
    LaunchedEffect(copiedTag) {
        if (copiedTag != null) {
            delay(1200)
            copiedTag = null
        }
    }

    fun toggleBookmark(restrict: Int) {
        if (bookmarkBusy) return
        scope.launch {
            bookmarkBusy = true
            try {
                val cur = detail
                if (restrict < 0) {
                    PixivApi.removeBookmark("novel", workId, cur?.bookmarkId ?: 0)
                    detail = cur?.copy(isBookmarked = false, bookmarkPrivate = false)
                    feedback = sBookmarkRemoved
                } else {
                    if (cur?.isBookmarked == true && cur.bookmarkPrivate != (restrict == 1)) {
                        PixivApi.removeBookmark("novel", workId, cur.bookmarkId)
                    }
                    PixivApi.addNovelBookmark(workId, restrict)
                    detail = cur?.copy(isBookmarked = true, bookmarkPrivate = restrict == 1)
                    feedback = if (restrict == 0) sBookmarkedPublic else sBookmarkedPrivate
                }
            } catch (e: Exception) {
                feedback = "收藏失败：${e.message}"
            }
            detail?.let { FeedCache.novelDetail[workId] = it }
            bookmarkBusy = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            loading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { LoadingState() }
            error != null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                ErrorState(error ?: "", onRetry = { retryKey++ })
            }

            else -> {
                val d = detail ?: return@Box
                val clipboard = LocalClipboardManager.current
                LazyColumn(contentPadding = innerPadding) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(d.title, style = MiuixTheme.textStyles.title3)
                            Spacer(Modifier.height(10.dp))
                            AuthorHeader(name = d.author, userId = d.authorId, avatar = d.avatar, onOpenUser = onOpenUser)
                            Spacer(Modifier.height(10.dp))
                            feedback?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.primary)
                            }
                            if (d.tags.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    d.tags.forEach { tag ->
                                        TextTagChip(tag = tag, onClick = { onSearchTag(tag) }, onLongClick = {
                                            clipboard.setText(AnnotatedString(tag))
                                            copiedTag = tag
                                        })
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            if (d.content.isBlank()) {
                                Text(Str.novel_detail() + "：正文不可用", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            } else {
                                Text(d.content, style = MiuixTheme.textStyles.body1)
                            }
                        }
                    }
                }
                copiedTag?.let { tag ->
                    Box(Modifier.fillMaxSize().padding(bottom = 40.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(Str.copied(tag), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceContainer)
                        }
                    }
                }
            }
        }

        var showBookmarkMenu by remember { mutableStateOf(false) }
        FloatingButton(
            onClick = { showBookmarkMenu = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 64.dp),
        ) {
            Icon(imageVector = MiuixIcons.Favorites, contentDescription = Str.bookmark_public_label())
        }
        FloatingButton(
            onClick = { onOpenComments(workId, "novel") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(imageVector = MiuixIcons.Messages, contentDescription = Str.comments())
        }

        OverlayDialog(show = showBookmarkMenu, onDismissRequest = { showBookmarkMenu = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Str.bookmark_public_label(), style = MiuixTheme.textStyles.title3)
                val isBm = detail?.isBookmarked == true
                if (!isBm) {
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(0)
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.bookmark_public()) }
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(1)
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.bookmark_private()) }
                } else {
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(if (detail?.bookmarkPrivate == true) 0 else 1)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (detail?.bookmarkPrivate == true) Str.bookmark_public() else Str.bookmark_private())
                    }
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(-1)
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.unbookmark()) }
                }
                Button(onClick = { showBookmarkMenu = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
            }
        }
    }
}

@Composable
fun ModeChip(value: String, label: String, current: String, onClick: () -> Unit) {
    val selected = value == current
    val color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh
    val textColor = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceContainer
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MiuixTheme.textStyles.body2, color = textColor)
    }
}

@Composable
fun PixivWorkRow(work: PixivWork, onClick: () -> Unit, onOpenUser: (Long) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val ctx = LocalPlatformContext.current
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(work.thumb).crossfade(true).build(),
                contentDescription = work.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(work.title, style = MiuixTheme.textStyles.body1, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuthorAvatar(work.avatar, 16.dp, work.authorId, onOpenUser)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Str.author_prefix() + work.author,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun NovelRow(novel: PixivNovel, onClick: () -> Unit, onOpenUser: (Long) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val ctx = LocalPlatformContext.current
            if (novel.cover.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(novel.cover).crossfade(true).build(),
                    contentDescription = novel.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(MiuixTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = MiuixIcons.Tasks, contentDescription = Str.novel(), tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(novel.title, style = MiuixTheme.textStyles.body1, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuthorAvatar(novel.avatar, 16.dp, novel.authorId, onOpenUser)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Str.author_prefix() + novel.author,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** 作者头像（列表行内小尺寸，可点击进入主页）。 */
@Composable
fun AuthorAvatar(url: String, size: Dp, userId: Long, onOpenUser: (Long) -> Unit = {}) {
    val ctx = LocalPlatformContext.current
    if (url.isNotBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(url).crossfade(true).build(),
            contentDescription = Str.avatar_desc(),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .clickable { onOpenUser(userId) },
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                .clickable { onOpenUser(userId) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Tasks,
                contentDescription = Str.avatar_desc(),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

@Composable
fun LogViewerScreen(onBack: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    PixoSubScreen(title = Str.view_logs(), onBack = onBack) { innerPadding ->
        val logs = remember(tick) { LogBuffer.snapshot() }
        val crashText = remember(tick) { readCrashLog().trim() }
        LazyColumn(contentPadding = innerPadding) {
            if (crashText.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            Str.crash_log(),
                            style = MiuixTheme.textStyles.title3,
                            color = MiuixTheme.colorScheme.error,
                        )
                        Button(onClick = {
                            clearCrashLog()
                            tick++
                        }) { Text(Str.clear_crash_log()) }
                    }
                }
                item {
                    Text(
                        text = crashText,
                        style = MiuixTheme.textStyles.footnote2,
                        fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = { LogBuffer.clear() }) { Text(Str.clear_logs()) }
                }
            }
            if (logs.isEmpty() && crashText.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text(Str.no_comments(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            } else {
                items(logs) { line ->
                    Text(
                        text = line,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** 崩溃日志独立页：展示持久化的崩溃堆栈，可清空。 */
@Composable
fun CrashLogPage(onBack: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    val text = remember(tick) { readCrashLog().trim() }
    PixoSubScreen(title = Str.crash_log(), onBack = onBack) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = {
                    clearCrashLog()
                    tick++
                }, enabled = text.isNotBlank()) { Text(Str.clear_crash_log()) }
            }
            if (text.isBlank()) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(Str.crash_none(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            } else {
                LazyColumn(contentPadding = innerPadding) {
                    item {
                        Text(
                            text = text,
                            style = MiuixTheme.textStyles.footnote2,
                            fontFamily = FontFamily.Monospace,
                            color = MiuixTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 发现板块：发现作品流（无限滚动 + 下拉刷新，返回恢复原位）。 */
@Composable
fun DiscoverScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
) {
    val cacheKey = "discover:feed"
    var items by remember(cacheKey) { mutableStateOf<List<PixivWork>?>(FeedCache.illust[cacheKey]) }
    val listState = key(cacheKey) { rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0) }
    var loading by remember { mutableStateOf(items == null) }
    var loadingMore by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(cacheKey, retryKey) {
        if (items != null && retryKey == 0) return@LaunchedEffect
        loading = true
        error = null
        try {
            val list = PixivApi.fetchTopFeed(0, 30)
            FeedCache.illust[cacheKey] = list
            items = list
            hasMore = list.size >= 30
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "discover load failed", e)
        }
        loading = false
    }

    fun refresh() {
        scope.launch {
            refreshing = true
            try {
                val list = PixivApi.fetchTopFeed(0, 30)
                FeedCache.illust[cacheKey] = list
                items = list
                hasMore = list.size >= 30
                error = null
            } catch (e: Exception) {
                error = e.friendlyMessage()
                logError("Pixo", "discover refresh failed", e)
            }
            refreshing = false
        }
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            val n = items?.size ?: 0
            try {
                val more = PixivApi.fetchTopFeed(n, 30)
                val cur = (items ?: emptyList()).toMutableList()
                val seen = HashSet(cur.map { it.id })
                cur.addAll(more.filter { it.id !in seen })
                FeedCache.illust[cacheKey] = cur
                items = cur
                hasMore = more.size >= 30
            } catch (_: Exception) {
            }
            loadingMore = false
        }
    }

    val list = items.orEmpty()
    LaunchedEffect(listState, list.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && last >= total - 3
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }
    SideEffect { FeedCache.indexes[cacheKey] = listState.firstVisibleItemIndex }

    PixoSubScreen(title = Str.discover(), onBack = onBack) { innerPadding ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = { refresh() },
            refreshTexts = refreshTexts(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            when {
                loading && list.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    LoadingState()
                }
                error != null && list.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    ErrorState(error ?: "", onRetry = { retryKey++ })
                }
                list.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    EmptyHint()
                }
                else -> LazyColumn(
                    state = listState,
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(list, key = { it.id }) { w ->
                        PixivWorkRow(w, onClick = { onOpenDetail(w.id, "illust") }, onOpenUser = onOpenUser)
                    }
                    item { EndItem(loadingMore, hasMore, onRetry = {}) }
                }
            }
        }
    }
}

/** 推荐板块：Pixiv 推荐作品（下拉刷新，恢复滚动）。 */
@Composable
fun RecommendedScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
) {
    val cacheKey = "recommend:feed"
    var items by remember(cacheKey) { mutableStateOf<List<PixivWork>?>(FeedCache.illust[cacheKey]) }
    val listState = key(cacheKey) { rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0) }
    var loading by remember { mutableStateOf(items == null) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        val list = PixivApi.fetchTopArtworks()
        FeedCache.illust[cacheKey] = list
        items = list
    }

    LaunchedEffect(cacheKey, retryKey) {
        if (items != null && retryKey == 0) return@LaunchedEffect
        loading = true
        error = null
        try {
            load()
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "recommend load failed", e)
        }
        loading = false
    }

    fun refresh() {
        scope.launch {
            refreshing = true
            try {
                load()
                error = null
            } catch (e: Exception) {
                error = e.friendlyMessage()
                logError("Pixo", "recommend refresh failed", e)
            }
            refreshing = false
        }
    }

    SideEffect { FeedCache.indexes[cacheKey] = listState.firstVisibleItemIndex }

    val list = items.orEmpty()
    PixoSubScreen(title = Str.recommended(), onBack = onBack) { innerPadding ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = { refresh() },
            refreshTexts = refreshTexts(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            when {
                loading && list.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    LoadingState()
                }
                error != null && list.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    ErrorState(error ?: "", onRetry = { retryKey++ })
                }
                list.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    EmptyHint()
                }
                else -> LazyColumn(
                    state = listState,
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(list, key = { it.id }) { w ->
                        PixivWorkRow(w, onClick = { onOpenDetail(w.id, "illust") }, onOpenUser = onOpenUser)
                    }
                }
            }
        }
    }
}

/** 作者主页：资料头 + 插画/小说作品列表。 */
@Composable
fun UserScreen(
    userId: Long,
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit,
) {
    val illustKey = "userIllusts:$userId"
    val novelKey = "userNovels:$userId"
    val cachedUser = remember(userId) { FeedCache.userProfiles[userId] }
    var user by remember { mutableStateOf(cachedUser) }
    var kind by rememberSaveable { mutableStateOf("illust") }
    var illustItems by remember(userId) { mutableStateOf<List<PixivWork>?>(FeedCache.illust[illustKey] as? List<PixivWork>) }
    var novelItems by remember(userId) { mutableStateOf<List<PixivNovel>?>(FeedCache.novels[novelKey] as? List<PixivNovel>) }
    var loading by remember { mutableStateOf(illustItems == null || novelItems == null) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var bioDialog by remember { mutableStateOf<String?>(null) }
    var copyMenu by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val copyAllLabel = Str.copy_all()
    val cacheKey = if (kind == "novel") novelKey else illustKey
    val listState = key(cacheKey) { rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0) }

    suspend fun fetchMore(kind: String, offset: Int): List<Any> =
        if (kind == "novel") PixivApi.fetchUserNovels(userId, offset, 30)
        else PixivApi.fetchUserIllusts(userId, offset, 30)

    LaunchedEffect(userId, retryKey) {
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
            }
            hasMore = tokenMode
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "user works load failed", e)
        }
        loading = false
    }

    val currentList: List<Any> = if (kind == "novel") novelItems.orEmpty() else illustItems.orEmpty()

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

    PixoSubScreen(title = Str.author_page(), onBack = onBack) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            when {
                loading && user == null && currentList.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    LoadingState()
                }
                error != null && user == null && currentList.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    ErrorState(error ?: "", onRetry = { retryKey++ })
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = innerPadding,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item { UserHeader(user) { bio -> copyMenu = bio } }
                        item {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ModeChip("illust", Str.illust(), kind) { kind = "illust" }
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
                        } else {
                            items(illustItems.orEmpty(), key = { it.id }) { w ->
                                PixivWorkRow(w, onClick = { onOpenDetail(w.id, "illust") }, onOpenUser = onOpenUser)
                            }
                            item { EndItem(loadingMore, hasMore, onRetry = {}) }
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
                        Button(onClick = {
                            clipboard.setText(AnnotatedString(text))
                            bioDialog = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(copyAllLabel)
                        }
                    }
                }
        }
    }
}

@Composable
private fun UserHeader(user: PixivUser?, onCopyBio: (String) -> Unit = {}) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 基本信息卡
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                AuthorAvatar(user?.avatar.orEmpty(), 68.dp, user?.id ?: 0)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user?.name.orEmpty(), style = MiuixTheme.textStyles.title3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    user?.account?.takeIf { it.isNotBlank() }?.let {
                        Text("@" + it, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Str.user_stats(user?.totalIllusts ?: 0, user?.totalNovels ?: 0),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
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

@Composable
private fun EmptyHint() {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Text(Str.no_works(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}