package app.pixo

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Weeks
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 内存级分页/滚动缓存：从详情返回时恢复到原滚动位置，详情等避免重复加载。 */
object FeedCache {
    val illust = HashMap<String, List<PixivWork>>()
    val novels = HashMap<String, List<PixivNovel>>()
    val users = HashMap<String, List<PixivUser>>()
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

    fun clear() {
        illust.clear()
        novels.clear()
        users.clear()
        indexes.clear()
        illustDetail.clear()
        novelDetail.clear()
        illustPages.clear()
        comments.clear()
        userProfiles.clear()
        userWorksCache.clear()
    }
}

/** 搜索结果/列表里任意条目的稳定 id（插画、小说、用户混排时去重用）。 */
private fun itemIdOf(it: Any): Long = when (it) {
    is PixivWork -> it.id
    is PixivNovel -> it.id
    is PixivUser -> it.id
    else -> 0L
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
    onOpenRanking: (String, String) -> Unit,
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

    val scrollBehavior = MiuixScrollBehavior()
    PixoTabFrame(title = Str.nav_home(), scrollBehavior = scrollBehavior) { frame ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = { refresh() },
            refreshTexts = refreshTexts(),
            contentPadding = PaddingValues(top = frame.calculateTopPadding()),
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                overscrollEffect = null,
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
    onOpenRanking: (String, String) -> Unit,
    onOpenNovelRanking: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenRecommended: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryCard(MiuixIcons.All, Str.illust_ranking()) { onOpenRanking("daily", "illust") }
            EntryCard(MiuixIcons.Notes, Str.novel_ranking()) { onOpenNovelRanking("daily") }
            EntryCard(MiuixIcons.Image, Str.discover()) { onOpenDiscover() }
            EntryCard(MiuixIcons.Promotions, Str.recommended()) { onOpenRecommended() }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryCard(MiuixIcons.Search, Str.search()) { onOpenSearch() }
            EntryCard(MiuixIcons.All, Str.manga_ranking()) { onOpenRanking("daily", "manga") }
            EntryCard(MiuixIcons.Favorites, Str.bookmarks()) { onOpenBookmarks("show") }
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
    content: String = "illust",
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
) {
    var currentMode by rememberSaveable { mutableStateOf(mode) }
    val cacheKey = "rank:$content:$currentMode"
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
            val list = if (content == "manga") PixivApi.fetchMangaRanking(currentMode, 1)
            else PixivApi.fetchRanking(currentMode, 1)
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
                val list = if (content == "manga") PixivApi.fetchMangaRanking(currentMode, 1)
                else PixivApi.fetchRanking(currentMode, 1)
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
                val more = if (content == "manga") PixivApi.fetchMangaRanking(currentMode, page)
                else PixivApi.fetchRanking(currentMode, page)
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

    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = if (content == "manga") Str.manga_ranking() else Str.ranking(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
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
                modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                overscrollEffect = null,
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

    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.novel_ranking(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
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
                modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                overscrollEffect = null,
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
    var currentRestrict by rememberSaveable { mutableStateOf(restrict) }
    val cacheKey = "bookmarks:$currentRestrict"
    var items by remember(cacheKey) { mutableStateOf<List<PixivWork>?>(FeedCache.illust[cacheKey]) }
    val listState = key(cacheKey) { rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0) }
    var offset by remember(currentRestrict) { mutableIntStateOf(items?.size ?: 0) }
    var maxBookmarkId by remember(currentRestrict) { mutableLongStateOf(0L) }
    var hasMore by remember(currentRestrict) { mutableStateOf(true) }
    var loading by remember(currentRestrict) { mutableStateOf(items == null) }
    var loadingMore by remember(currentRestrict) { mutableStateOf(false) }
    var refreshing by remember(currentRestrict) { mutableStateOf(false) }
    var error by remember(currentRestrict) { mutableStateOf<String?>(null) }
    var retryKey by remember(currentRestrict) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val initial = items
    LaunchedEffect(cacheKey, retryKey) {
        if (initial != null && initial.isNotEmpty()) return@LaunchedEffect
        loading = true
        error = null
        try {
            val page = PixivApi.fetchIllustBookmarks(currentRestrict, 0, null, 24)
            FeedCache.illust[cacheKey] = page.works
            items = page.works
            offset = page.works.size
            maxBookmarkId = page.nextMaxBookmarkId ?: 0L
            hasMore = page.hasMore
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
                val page = PixivApi.fetchIllustBookmarks(currentRestrict, 0, null, 24)
                FeedCache.illust[cacheKey] = page.works
                items = page.works
                offset = page.works.size
                maxBookmarkId = page.nextMaxBookmarkId ?: 0L
                hasMore = page.hasMore
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
            try {
                val page = PixivApi.fetchIllustBookmarks(currentRestrict, offset, maxBookmarkId.takeIf { it > 0L }, 24)
                val cur = (items ?: emptyList()).toMutableList()
                val seen = HashSet(cur.map { it.id })
                cur.addAll(page.works.filter { it.id !in seen })
                items = cur
                FeedCache.illust[cacheKey] = cur
                offset = cur.size
                maxBookmarkId = page.nextMaxBookmarkId ?: 0L
                hasMore = page.hasMore
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

    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.bookmarks(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
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
                modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                overscrollEffect = null,
            ) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeChip("show", Str.bookmark_public_label(), currentRestrict) { currentRestrict = "show" }
                        ModeChip("hide", Str.bookmark_private_label(), currentRestrict) { currentRestrict = "hide" }
                    }
                }
                when {
                    loading && list.isEmpty() -> item { LoadingState() }
                    error != null && list.isEmpty() -> item {
                        ErrorState(error ?: "", onRetry = { retryKey++ })
                    }
                }
                if (list.isNotEmpty()) {
                    items(list, key = { it.id }) { work ->
                        PixivWorkRow(work, onClick = { onOpenDetail(work.id, "illust") }, onOpenUser = onOpenUser)
                    }
                    item { EndItem(loadingMore, hasMore, onRetry = {}) }
                } else if (!loading && error == null) {
                    item { EmptyHint() }
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
            when (kind) {
                "novel" -> FeedCache.novels[cacheKey]
                "user" -> FeedCache.users[cacheKey]
                else -> FeedCache.illust[cacheKey]
            },
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
    // 用户搜索每页固定 10 条，插画/小说每页 60 条
    val pageSize = if (kind == "user") 10 else 60

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
            val list: List<Any> = when (kind) {
                "novel" -> PixivApi.searchNovels(submitted, 1)
                "user" -> PixivApi.searchUsers(submitted, 1)
                else -> PixivApi.searchArtworks(submitted, 1)
            }
            FeedCache.illust[cacheKey] = list.filterIsInstance<PixivWork>()
            FeedCache.novels[cacheKey] = list.filterIsInstance<PixivNovel>()
            FeedCache.users[cacheKey] = list.filterIsInstance<PixivUser>()
            items = list
            page = 1
            hasMore = list.size >= pageSize
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
                val list: List<Any> = when (kind) {
                    "novel" -> PixivApi.searchNovels(submitted, 1)
                    "user" -> PixivApi.searchUsers(submitted, 1)
                    else -> PixivApi.searchArtworks(submitted, 1)
                }
                items = list
                FeedCache.illust[cacheKey] = list.filterIsInstance<PixivWork>()
                FeedCache.novels[cacheKey] = list.filterIsInstance<PixivNovel>()
                FeedCache.users[cacheKey] = list.filterIsInstance<PixivUser>()
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
                val more: List<Any> = when (kind) {
                    "novel" -> PixivApi.searchNovels(submitted, next)
                    "user" -> PixivApi.searchUsers(submitted, next)
                    else -> PixivApi.searchArtworks(submitted, next)
                }
                val cur = (items ?: emptyList()).toMutableList()
                val seen = HashSet(cur.map { itemIdOf(it) })
                cur.addAll(more.filter {
                    val id = itemIdOf(it)
                    id !in seen
                })
                items = cur
                FeedCache.illust[cacheKey] = cur.filterIsInstance<PixivWork>()
                FeedCache.novels[cacheKey] = cur.filterIsInstance<PixivNovel>()
                FeedCache.users[cacheKey] = cur.filterIsInstance<PixivUser>()
                page = next
                hasMore = more.size >= pageSize
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

    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.search(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
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
                modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                overscrollEffect = null,
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
                        ModeChip("user", Str.user(), kind) { kind = "user" }
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
                        itemIdOf(it)
                    }) { i ->
                        val it = list[i]
                        when (it) {
                            is PixivWork -> PixivWorkRow(it, onClick = { openItem(it.id, "illust") }, onOpenUser = onOpenUser)
                            is PixivNovel -> NovelRow(it, onClick = { openItem(it.id, "novel") }, onOpenUser = onOpenUser)
                            is PixivUser -> PixivUserRow(it, onClick = { onOpenUser(it.id) })
                            else -> {}
                        }
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
    onOpenDetail: (Long, String) -> Unit,
) {
    // 顶栏「下载原图」触发信号：传给 IllustDetail，由它拿原图数据发起下载
    var downloadSignal by remember { mutableIntStateOf(0) }
    PixoSubScreen(title = "", onBack = onBack, actions = {
        var showMenu by remember { mutableStateOf(false) }
        IconButton(onClick = { showMenu = true }) {
            Icon(imageVector = MiuixIcons.More, contentDescription = Str.more_actions())
        }
        if (showMenu) {
            OverlayDialog(show = true, onDismissRequest = { showMenu = false }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Str.more_actions(), style = MiuixTheme.textStyles.title3)
                    Button(onClick = {
                        showMenu = false
                        openUrlInBrowser(
                            if (kind == "novel") "https://www.pixiv.net/novel/show.php?id=$workId"
                            else "https://www.pixiv.net/artworks/$workId",
                        )
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.open_in_browser()) }
                    Button(onClick = {
                        showMenu = false
                        shareText(
                            if (kind == "novel") "https://www.pixiv.net/novel/show.php?id=$workId"
                            else "https://www.pixiv.net/artworks/$workId",
                        )
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.share()) }
                    if (kind != "novel") {
                        Button(onClick = {
                            showMenu = false
                            downloadSignal++
                        }, modifier = Modifier.fillMaxWidth()) { Text(Str.download_original()) }
                    }
                    Button(onClick = { showMenu = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
                }
            }
        }
    }) { innerPadding ->
        if (kind == "novel") {
            NovelDetail(workId, innerPadding, onSearchTag, onOpenUser, onOpenComments)
        } else {
            IllustDetail(workId, innerPadding, onSearchTag, onOpenUser, onOpenComments, downloadSignal, onOpenDetail)
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
    downloadSignal: Int = 0,
    onOpenDetail: (Long, String) -> Unit = { _, _ -> },
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
    var lightboxIndex by remember { mutableIntStateOf(-1) }
    var ugoira by remember(workId) { mutableStateOf<PixivUgoira?>(null) }
    var ugoiraPlaying by remember(workId) { mutableStateOf(!AppPrefs.ugoiraConfirm) }
    var downloadBusy by remember { mutableStateOf(false) }
    var downloadDialog by remember { mutableStateOf(false) }
    var downloadToast by remember { mutableStateOf<String?>(null) }
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val scope = rememberCoroutineScope()

    // 预取文案（协程内不能用 @Composable 资源访问）
    val sBookmarkedPublic = Str.bookmarked_public()
    val sBookmarkedPrivate = Str.bookmarked_private()
    val sBookmarkRemoved = Str.bookmark_removed()
    val sDownloadFailed = Str.download_failed()
    // 预取带占位数字的完成文案（协程内不能用 @Composable 资源访问）
    val sDownloadDoneFormat = Str.download_done(123456).replace("123456", "%s")

    LaunchedEffect(workId, retryKey) {
        // 已缓存则直接使用，避免重复加载
        val cachedDetail = FeedCache.illustDetail[workId]
        if (cachedDetail != null && FeedCache.illustPages[workId] != null && retryKey == 0) {
            if (cachedDetail.illustType == 2 && ugoira == null) {
                try {
                    ugoira = PixivApi.fetchUgoiraMeta(workId)
                } catch (e: Exception) {
                    logError("Pixo", "ugoira meta fail", e)
                }
            }
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
            // ugoira 动图：详情命中 illustType=2 时取帧元数据，失败不影响正文
            if (d.illustType == 2) {
                try {
                    ugoira = PixivApi.fetchUgoiraMeta(workId)
                } catch (e: Exception) {
                    logError("Pixo", "ugoira meta fail", e)
                }
            }
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

    fun fileNameFor(page: PixivPage, index: Int): String {
        val url = page.original.ifBlank { page.url }
        val ext = url.substringAfterLast('.', "jpg").substringBefore('?')
            .takeIf { it.isNotBlank() && it.length <= 5 } ?: "jpg"
        return "${workId}_p$index.$ext"
    }

    fun downloadOne(page: PixivPage, index: Int) {
        if (downloadBusy) return
        scope.launch {
            downloadBusy = true
            try {
                val saved = PixivApi.downloadImage(page.original.ifBlank { page.url }, fileNameFor(page, index))
                downloadToast = saved ?: sDownloadFailed
            } catch (e: Exception) {
                logError("Pixo", "download failed", e)
                downloadToast = sDownloadFailed + "：" + (e.message ?: "")
            }
            downloadBusy = false
        }
    }

    fun downloadSelected() {
        if (downloadBusy) return
        scope.launch {
            downloadBusy = true
            var ok = 0
            var fail = 0
            for (index in selectedPages.toSortedSet()) {
                val page = pages.getOrNull(index) ?: continue
                try {
                    // writeDownloadFile 写失败返回 null（不抛异常），必须以返回值统计，
                    // 否则 SAF 目录失效时会“假成功”
                    val saved = PixivApi.downloadImage(page.original.ifBlank { page.url }, fileNameFor(page, index))
                    if (saved != null) ok++ else fail++
                } catch (e: Exception) {
                    logError("Pixo", "download page $index failed", e)
                    fail++
                }
            }
            downloadToast = if (fail == 0) sDownloadDoneFormat.format(ok) else "$sDownloadFailed（$fail）"
            downloadBusy = false
            downloadDialog = false
        }
    }

    // 顶栏「下载原图」：单图直接下载，多图弹选择对话框（默认全选）
    LaunchedEffect(downloadSignal) {
        if (downloadSignal <= 0) return@LaunchedEffect
        val p = pages
        if (p.isEmpty()) return@LaunchedEffect
        if (p.size == 1) {
            downloadOne(p[0], 0)
        } else {
            selectedPages = p.indices.toSet()
            downloadDialog = true
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
                LazyColumn(contentPadding = innerPadding, modifier = Modifier.scrollEndHaptic().overScrollVertical()) {
                    itemsIndexed(pages) { index, page ->
                        val isUgoira = ugoira != null && index == 0
                        Box(Modifier.fillMaxWidth().clickable { lightboxIndex = index }) {
                            if (isUgoira) {
                                if (ugoiraPlaying) {
                                    UgoiraAsyncImage(
                                        workId = workId,
                                        fallbackUrl = page.url,
                                        contentDescription = d?.title ?: "",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth(),
                                        playing = true,
                                    )
                                } else {
                                    val ctx = LocalPlatformContext.current
                                    AsyncImage(
                                        model = remember(page.url) { ImageRequest.Builder(ctx).data(page.url).crossfade(true).build() },
                                        contentDescription = d?.title ?: "",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    // 手动确认播放：点击覆盖层开始动图
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { ugoiraPlaying = true },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                        ) {
                                            Text(Str.play_animation(), style = MiuixTheme.textStyles.body2, color = Color.White)
                                        }
                                    }
                                }
                            } else {
                                val ctx = LocalPlatformContext.current
                                AsyncImage(
                                    model = remember(page.url) { ImageRequest.Builder(ctx).data(page.url).crossfade(true).build() },
                                    contentDescription = d?.title ?: "",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (isUgoira) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(Str.animated_badge(), style = MiuixTheme.textStyles.footnote2, color = Color.White)
                                }
                            }
                        }
                    }
                    item {
                        IllustInfo(
                            d = d,
                            onSearchTag = onSearchTag,
                            onOpenUser = onOpenUser,
                            onLongPressTag = { tag -> copiedTag = tag },
                        )
                    }
                    item {
                        RelatedWorksRow(
                            workId = workId,
                            onOpenDetail = onOpenDetail,
                        )
                    }
                }
                BottomToast(copiedTag?.let { Str.copied(it) })
                BottomToast(feedback)
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

        OverlayDialog(show = downloadDialog, onDismissRequest = { if (!downloadBusy) downloadDialog = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Str.download_selected_hint(), style = MiuixTheme.textStyles.title3)
                Button(
                    onClick = {
                        selectedPages = if (selectedPages.size == pages.size) emptySet() else pages.indices.toSet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (selectedPages.size == pages.size) Str.deselect_all() else Str.select_all()) }
                LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(pages.size) { index ->
                        val checked = index in selectedPages
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedPages = if (checked) selectedPages - index else selectedPages + index }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (checked) MiuixTheme.colorScheme.primary else Color.Transparent)
                                    .border(1.5.dp, if (checked) Color.Transparent else MiuixTheme.colorScheme.onSurfaceVariantSummary, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (checked) {
                                    Text("✓", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onPrimary)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("${index + 1} / ${pages.size}", style = MiuixTheme.textStyles.body2)
                        }
                    }
                }
                Button(
                    onClick = { if (!downloadBusy && selectedPages.isNotEmpty()) downloadSelected() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Str.download()) }
                Button(onClick = { downloadDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
            }
        }

        BottomToast(downloadToast)

        if (lightboxIndex >= 0) {
            ImageLightbox(
                pages = pages,
                ugoira = ugoira,
                workId = workId,
                initialIndex = lightboxIndex,
                onClose = { lightboxIndex = -1 },
            )
        }
    }
}

/** 图片灯箱：点击图片全屏查看，横向滑动翻页。
 *  缩放交给成熟的 telephoto zoomable（双指捏合、缩放后拖动、双击放大/还原）；
 *  未放大时单指滑动交给 Pager 翻页，放大后 Pager 自动锁定。 */
@Composable
private fun ImageLightbox(
    pages: List<PixivPage>,
    ugoira: PixivUgoira?,
    workId: Long,
    initialIndex: Int,
    onClose: () -> Unit,
) {
    val ctx = LocalPlatformContext.current
    val safeIndex = initialIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeIndex) { pages.size.coerceAtLeast(1) }
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 6f))
    val isZoomed = (zoomableState.zoomFraction ?: 0f) > 0.001f
    var ugoiraPlaying by remember(workId) { mutableStateOf(!AppPrefs.ugoiraConfirm) }

    // 切页时回到未缩放状态
    LaunchedEffect(pagerState.currentPage) { zoomableState.resetZoom() }
    SystemBackHandler(enabled = true) { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isZoomed,
            modifier = Modifier.fillMaxSize(),
            overscrollEffect = null,
        ) { index ->
            val page = pages.getOrNull(index) ?: return@HorizontalPager
            val zoomModifier = Modifier
                .fillMaxSize()
                .zoomable(
                    state = zoomableState,
                    onClick = {
                        if ((zoomableState.zoomFraction ?: 0f) <= 0.001f) onClose()
                    },
                )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (ugoira != null && index == 0 && ugoiraPlaying) {
                    UgoiraAsyncImage(
                        workId = workId,
                        fallbackUrl = page.original.ifBlank { page.url },
                        contentDescription = "illust",
                        contentScale = ContentScale.Fit,
                        modifier = zoomModifier,
                        playing = true,
                    )
                } else {
                    AsyncImage(
                        model = remember(page.original.ifBlank { page.url }) { ImageRequest.Builder(ctx).data(page.original.ifBlank { page.url }).crossfade(true).build() },
                        contentDescription = "illust",
                        contentScale = ContentScale.Fit,
                        modifier = zoomModifier,
                    )
                    if (ugoira != null && index == 0 && !ugoiraPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { ugoiraPlaying = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text(Str.play_animation(), style = MiuixTheme.textStyles.body2, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        // 页码指示
        Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                text = "${pagerState.currentPage + 1} / ${pages.size}",
                style = MiuixTheme.textStyles.body2,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        // 关闭按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = MiuixIcons.Back, contentDescription = "关闭", tint = Color.White)
        }
    }
}

/** 头像全屏放大预览：黑底 + 双指缩放，未放大时点击或返回键关闭。 */
@Composable
private fun AvatarPreview(url: String, onClose: () -> Unit) {
    val ctx = LocalPlatformContext.current
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 6f))
    val isZoomed = (zoomableState.zoomFraction ?: 0f) > 0.001f
    SystemBackHandler(enabled = true) { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = remember(url) { ImageRequest.Builder(ctx).data(url).crossfade(true).build() },
                contentDescription = Str.avatar_desc(),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(
                        state = zoomableState,
                        onClick = {
                            if ((zoomableState.zoomFraction ?: 0f) <= 0.001f) onClose()
                        },
                    ),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = MiuixIcons.Back, contentDescription = "关闭", tint = Color.White)
        }
    }
}

private class UgoiraFrameData(val frames: List<ByteArray>, val delays: List<Int>)

/** 已下载的 ugoira 帧缓存（进程内共享），避免详情页/灯箱反复下载 zip 造成闪烁。 */
private val ugoiraFrameCache = mutableMapOf<Long, UgoiraFrameData>()

/** ugoira 动图播放：下载 zip → 解压帧 → 按帧延时轮播；失败时回退到静态图。 */
@Composable
private fun UgoiraAsyncImage(
    workId: Long,
    fallbackUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    playing: Boolean = true,
) {
    val ctx = LocalPlatformContext.current
    var frameBytes by remember(workId) { mutableStateOf(ugoiraFrameCache[workId]?.frames.orEmpty()) }
    var delays by remember(workId) { mutableStateOf(ugoiraFrameCache[workId]?.delays.orEmpty()) }
    var index by remember(workId) { mutableIntStateOf(0) }

    LaunchedEffect(workId) {
        if (ugoiraFrameCache.containsKey(workId)) return@LaunchedEffect
        try {
            val meta = PixivApi.fetchUgoiraMeta(workId)
            val bytes = loadUgoiraFrames(meta.zipUrl, meta.frames.map { it.file })
            if (bytes != null && bytes.isNotEmpty()) {
                val data = UgoiraFrameData(bytes, meta.frames.map { it.delayMs })
                ugoiraFrameCache[workId] = data
                frameBytes = data.frames
                delays = data.delays
            }
        } catch (e: Exception) {
            logError("Pixo", "ugoira load fail", e)
        }
    }
    LaunchedEffect(frameBytes, playing) {
        if (frameBytes.isEmpty() || !playing) return@LaunchedEffect
        while (true) {
            delay(delays.getOrElse(index % frameBytes.size) { 125 }.toLong())
            index = (index + 1) % frameBytes.size
        }
    }
    // 帧字节直接解码为 ImageBitmap 再用 Image 绘制，避免每帧走 Coil 请求造成闪烁
    // 暂停时固定显示首帧（zip 首帧即静态图），不切回 Coil，避免“播放/暂停”反复闪烁
    val frame = if (frameBytes.isEmpty()) null
    else frameBytes.getOrNull((if (playing) index else 0) % frameBytes.size)
    val frameBitmap = remember(frame) {
        frame?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
    }
    if (frameBitmap != null) {
        Image(
            bitmap = frameBitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        AsyncImage(
            model = remember(fallbackUrl) { ImageRequest.Builder(ctx).data(fallbackUrl).crossfade(true).build() },
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
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

/** 底部居中的提示条（下载失败/收藏结果等反馈统一样式）。 */
@Composable
internal fun BottomToast(text: String?) {
    if (text == null) return
    Box(Modifier.fillMaxSize().padding(bottom = 40.dp), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(MiuixTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(text, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceContainer)
        }
    }
}

@Composable
private fun IllustInfo(
    d: PixivIllustDetail?,
    onSearchTag: (String) -> Unit,
    onOpenUser: (Long) -> Unit,
    onLongPressTag: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(d?.title ?: "", style = MiuixTheme.textStyles.title3)
        Spacer(Modifier.height(10.dp))
        AuthorHeader(name = d?.author.orEmpty(), userId = d?.authorId ?: 0, avatar = d?.avatar.orEmpty(), onOpenUser = onOpenUser)
        Spacer(Modifier.height(10.dp))
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
    Card(Modifier.fillMaxWidth().clickable { onOpenUser(userId) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
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

/** 详情页「相关作品」横滑区：作品缩略卡 + 标题，点按打开新详情。 */
@Composable
private fun RelatedWorksRow(
    workId: Long,
    onOpenDetail: (Long, String) -> Unit,
) {
    var works by remember(workId) { mutableStateOf<List<PixivWork>?>(null) }
    LaunchedEffect(workId) {
        works = try {
            PixivApi.fetchRelatedWorks(workId)
        } catch (_: Exception) {
            emptyList()
        }
    }
    val list = works
    if (list == null || list.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = Str.related_works(),
            style = MiuixTheme.textStyles.title3,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            items(list, key = { it.id }) { work ->
                RelatedWorkCard(work, onClick = { onOpenDetail(work.id, "illust") })
            }
        }
    }
}

@Composable
private fun RelatedWorkCard(work: PixivWork, onClick: () -> Unit) {
    val ctx = LocalPlatformContext.current
    Column(
        Modifier.width(116.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(116.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = remember(work.thumb) { ImageRequest.Builder(ctx).data(work.thumb).crossfade(true).build() },
                contentDescription = work.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (work.isR18) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("R-18", style = MiuixTheme.textStyles.footnote2, color = Color.White)
                }
            }
        }
        Text(
            text = work.title,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
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
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var posting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackError by remember { mutableStateOf(false) }
    var copyMenu by remember { mutableStateOf<String?>(null) }
    var copySelect by remember { mutableStateOf<String?>(null) }
    /** 正在回复的目标：(评论 id, 用户名)；为空表示发表一级评论。 */
    var replyTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }
    /** 评论/追评刷新标记：发评论后 +1，触发已展开的追评区重拉数据。 */
    var refreshTick by remember { mutableIntStateOf(0) }
    val replyStates = remember { mutableStateOf(emptyMap<Long, ReplyUiState>()) }
    var replyStatesMap by replyStates

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
                PixivApi.addComment(kind, workId, text, replyTarget?.first ?: 0)
                val list = PixivApi.fetchComments(kind, workId)
                FeedCache.comments[cacheKey] = list
                comments = list
                draft = ""
                replyTarget = null
                refreshTick++
                feedback = sPosted
                feedbackError = false
            } catch (e: Exception) {
                feedback = sFailPrefix + (e.message ?: "")
                feedbackError = true
            }
            posting = false
        }
    }

    fun refresh() {
        if (refreshing) return
        scope.launch {
            refreshing = true
            try {
                val list = PixivApi.fetchComments(kind, workId)
                FeedCache.comments[cacheKey] = list
                comments = list
                error = null
            } catch (e: Exception) {
                error = e.friendlyMessage()
                logError("Pixo", "refresh comments failed", e)
            }
            refreshing = false
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.comments(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        loading && comments == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingState()
                        }
                        error != null && comments == null -> ErrorState(error ?: "", onRetry = { retryKey++ })
                        else -> PullToRefresh(
                            isRefreshing = refreshing,
                            onRefresh = { refresh() },
                            refreshTexts = refreshTexts(),
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                                overscrollEffect = null,
                            ) {
                                if (comments.orEmpty().isEmpty()) {
                                    item {
                                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(Str.no_comments(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                        }
                                    }
                                }
                                itemsIndexed(comments.orEmpty(), key = { _, c -> c.id }) { _, c ->
                                    RootCommentCard(
                                        c = c,
                                        kind = kind,
                                        workId = workId,
                                        onOpenUser = onOpenUser,
                                        onCopy = { t -> copyMenu = t },
                                        onReply = { t -> replyTarget = t.id to t.user },
                                        refreshTick = refreshTick,
                                        state = replyStatesMap[c.id] ?: ReplyUiState(),
                                        onStateChange = { s -> replyStatesMap = replyStatesMap + (c.id to s) },
                                    )
                                }
                            }
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
                // 回复目标提示：点评论卡片下的「回复」后出现
                replyTarget?.let { target ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Str.reply_action() + " @" + target.second.ifBlank { Str.anonymous() },
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = "×",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { replyTarget = null }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
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
                    }
                }
            }
        }
    }
}

/** 一级评论卡片：整卡长按复制文字；深色追评区内嵌卡片里，点「查看追评」展开、可对一级评论回复。 */
@Composable

/** 追评 UI 状态：提升到评论区持有（LazyColumn 滚出视口会销毁 item，状态在外层才不丢失、不收起）。 */
private data class ReplyUiState(
    val expanded: Boolean = false,
    val replies: List<PixivComment> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val appNextUrl: String? = null,
    val webPage: Int = 0,
    val error: String? = null,
)

private fun RootCommentCard(
    c: PixivComment,
    kind: String,
    workId: Long,
    onOpenUser: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onReply: (PixivComment) -> Unit,
    refreshTick: Int,
    state: ReplyUiState,
    onStateChange: (ReplyUiState) -> Unit,
) {
    val scope = rememberCoroutineScope()

    suspend fun loadPage() {
        val next = state.copy(
            loading = state.replies.isEmpty(),
            loadingMore = state.replies.isNotEmpty(),
            error = null,
        )
        onStateChange(next)
        try {
            val res = PixivApi.fetchCommentReplies(kind, workId, c.id, next.appNextUrl, next.webPage + 1)
            val seen = next.replies.mapTo(HashSet()) { it.id }
            onStateChange(
                next.copy(
                    replies = next.replies + res.replies.filter { it.id !in seen },
                    hasMore = res.hasMore,
                    appNextUrl = res.nextUrl,
                    webPage = next.webPage + 1,
                    loading = false,
                    loadingMore = false,
                ),
            )
        } catch (e: Exception) {
            onStateChange(next.copy(loading = false, loadingMore = false, error = e.friendlyMessage()))
        }
    }

    // 首次展开或发过新评论（refreshTick 变化）时拉取/刷新追评；
    // 状态已提升到评论区持有，滚动出屏再回来不丢失、不收起。
    LaunchedEffect(c.id, state.expanded, refreshTick) {
        if (state.expanded && (c.hasReplies || c.replies.isNotEmpty())) {
            if (refreshTick > 0 || state.replies.isEmpty()) {
                loadPage()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { onCopy(c.content) }),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
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
                    Text(c.content, style = MiuixTheme.textStyles.body2)
                }
            }
            // 操作行：回复（对一级评论也能回复），追评卡片紧随其下
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = Str.reply_action(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReply(c) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            // 深色追评区：追评卡片嵌在一级评论卡片里面（回复按钮下方），用更深颜色区分
            if (c.hasReplies || c.replies.isNotEmpty() || state.expanded) {
                if (!state.expanded) {
                    // 收起态：深色条，点击展开
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .clickable { onStateChange(state.copy(expanded = true)) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = Str.view_replies(),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        when {
                            state.loading && state.replies.isEmpty() -> Box(
                                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(Str.load_more(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }

                            state.error != null && state.replies.isEmpty() -> Column(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(state.error ?: "", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                Text(
                                    text = Str.retry(),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { scope.launch { loadPage() } }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }

                            state.replies.isEmpty() -> Box(
                                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(Str.no_replies(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }

                            else -> {
                                state.replies.forEach { r ->
                                    ReplyCard(c = r, onOpenUser = onOpenUser, onCopy = { t -> onCopy(t) }, onReply = { onReply(r) })
                                }
                                if (state.loadingMore) {
                                    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(Str.load_more(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                    }
                                }
                            }
                        }
                        // 底部操作：加载更多 / 收起
                        Row(
                            Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (state.hasMore) {
                                Text(
                                    text = Str.load_more_replies(),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = !state.loadingMore) { scope.launch { loadPage() } }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            Text(
                                text = Str.collapse_replies(),
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onStateChange(state.copy(expanded = false)) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 追评卡片（用于一级评论卡片内的深色追评区）：头像 + 昵称 + 日期 + 内容 + 回复入口。 */
@Composable
private fun ReplyCard(
    c: PixivComment,
    onOpenUser: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onReply: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .combinedClickable(onClick = {}, onLongClick = { onCopy(c.content) })
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuthorAvatar(c.avatar, 24.dp, c.userId, onOpenUser)
            Spacer(Modifier.width(8.dp))
            Text(
                text = c.user.ifBlank { Str.anonymous() },
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            c.date.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.width(6.dp))
                Text(it, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        Text(c.content, style = MiuixTheme.textStyles.footnote2)
        // 卡片下点击回复，唤起回复输入框
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = Str.reply_action(),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onReply() }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
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
                LazyColumn(contentPadding = innerPadding, modifier = Modifier.scrollEndHaptic().overScrollVertical()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(d.title, style = MiuixTheme.textStyles.title3)
                            Spacer(Modifier.height(10.dp))
                            AuthorHeader(name = d.author, userId = d.authorId, avatar = d.avatar, onOpenUser = onOpenUser)
                            Spacer(Modifier.height(10.dp))
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
                BottomToast(copiedTag?.let { Str.copied(it) })
                BottomToast(feedback)
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
            Box {
                val ctx = LocalPlatformContext.current
                AsyncImage(
                    model = remember(work.thumb) { ImageRequest.Builder(ctx).data(work.thumb).crossfade(true).build() },
                    contentDescription = work.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                )
                if (work.isR18) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE53935))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(Str.r18(), style = MiuixTheme.textStyles.footnote1, color = Color.White)
                    }
                }
                if (work.illustType == 2) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(Str.animated_badge(), style = MiuixTheme.textStyles.footnote1, color = Color.White)
                    }
                }
                if (work.pageCount > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text("×${work.pageCount}", style = MiuixTheme.textStyles.footnote1, color = Color.White)
                    }
                }
            }
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
            Box {
                if (novel.cover.isNotBlank()) {
                    AsyncImage(
                        model = remember(novel.cover) { ImageRequest.Builder(ctx).data(novel.cover).crossfade(true).build() },
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
                if (novel.isR18) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE53935))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(Str.r18(), style = MiuixTheme.textStyles.footnote1, color = Color.White)
                    }
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

@Composable
private fun PixivUserRow(user: PixivUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AuthorAvatar(user.avatar, 48.dp, user.id)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, style = MiuixTheme.textStyles.title3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                user.account.takeIf { it.isNotBlank() }?.let { account ->
                    Text(
                        "@" + account,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                user.comment.takeIf { it.isNotBlank() }?.let { comment ->
                    Text(
                        comment.replace('\n', ' '),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
            model = remember(url) { ImageRequest.Builder(ctx).data(url).crossfade(true).build() },
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
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.view_logs(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        val logs = remember(tick) { LogBuffer.snapshot() }
        val crashText = remember(tick) { readCrashLog().trim() }
        LazyColumn(contentPadding = innerPadding, modifier = Modifier.scrollEndHaptic().overScrollVertical()) {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                clipboard.setText(AnnotatedString(crashText))
                            }, enabled = crashText.isNotBlank()) { Text(Str.copy_title()) }
                            Button(onClick = {
                                clearCrashLog()
                                tick++
                            }) { Text(Str.clear_crash_log()) }
                        }
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
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(logs.joinToString("\n")))
                    }, enabled = logs.isNotEmpty()) { Text(Str.copy_title()) }
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
    val clipboard = LocalClipboardManager.current
    val text = remember(tick) { readCrashLog().trim() }
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.crash_log(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = {
                    clipboard.setText(AnnotatedString(text))
                }, enabled = text.isNotBlank()) { Text(Str.copy_title()) }
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
                LazyColumn(contentPadding = innerPadding, modifier = Modifier.scrollEndHaptic().overScrollVertical()) {
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
    onOpenSettings: (() -> Unit)? = null,
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

    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.discover(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
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
                    EmptyHint(onOpenSettings = onOpenSettings)
                }
                else -> LazyColumn(
                    state = listState,
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                overscrollEffect = null,
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
    onOpenSettings: (() -> Unit)? = null,
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
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.recommended(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
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
                    EmptyHint(onOpenSettings = onOpenSettings)
                }
                else -> LazyColumn(
                    state = listState,
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                overscrollEffect = null,
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
    onOpenFollows: (Long, String) -> Unit,
) {
    val illustKey = "userIllusts:$userId"
    val mangaKey = "userManga:$userId"
    val novelKey = "userNovels:$userId"
    val cachedUser = remember(userId) { FeedCache.userProfiles[userId] }
    var user by remember { mutableStateOf(cachedUser) }
    var kind by rememberSaveable { mutableStateOf("illust") }
    var illustItems by remember(userId) { mutableStateOf<List<PixivWork>?>(FeedCache.illust[illustKey] as? List<PixivWork>) }
    var mangaItems by remember(userId) { mutableStateOf<List<PixivWork>?>(FeedCache.illust[mangaKey] as? List<PixivWork>) }
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
        if (showMenu) {
            OverlayDialog(show = true, onDismissRequest = { showMenu = false }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Str.more_actions(), style = MiuixTheme.textStyles.title3)
                    Button(onClick = {
                        showMenu = false
                        openUrlInBrowser("https://www.pixiv.net/users/$userId")
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.open_in_browser()) }
                    Button(onClick = {
                        showMenu = false
                        shareText("https://www.pixiv.net/users/$userId")
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.share()) }
                    Button(onClick = { showMenu = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
                }
            }
        }
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
                    Text(user?.name.orEmpty(), style = MiuixTheme.textStyles.title3, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

@Composable
internal fun UserStatCard(
    label: String,
    count: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = count?.toString() ?: "—",
                style = MiuixTheme.textStyles.title3,
            )
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
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

@Composable
private fun EmptyHint(onOpenSettings: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(Str.no_works(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (onOpenSettings != null && AppPrefs.cookie.isBlank() && !PixivAuth.isLoggedIn) {
            Text(
                text = Str.need_cookie_hint(),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Button(onClick = onOpenSettings) { Text(Str.go_settings()) }
        }
    }
}
