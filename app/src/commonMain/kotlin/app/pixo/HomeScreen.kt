package app.pixo

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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




@Composable
fun HomePage(
    onOpenRanking: (String, String) -> Unit,
    onOpenNovelRanking: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenRecommended: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenBookmarks: (String) -> Unit,
    waterfall: Boolean,
    onOpenFollow: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUser: (Long) -> Unit,
    bottomPadding: Dp = 0.dp,
) {
    val cacheKey = "home:feed"
    var items by remember(SettingsRevision.current) { mutableStateOf<List<PixivWork>?>(FeedCache.illust[cacheKey]) }
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
            if (waterfall) {
                StaggeredWorkGrid(
                    works = list,
                    onOpenDetail = { id -> onOpenDetail(id, "illust") },
                    onOpenUser = onOpenUser,
                    modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(top = frame.calculateTopPadding(), bottom = bottomPadding),
                    loadingMore = loadingMore,
                    hasMore = hasMore,
                    onLoadMore = { loadMore() },
                    header = {
                        Column {
                            HomeEntries(
                                onOpenRanking, onOpenNovelRanking, onOpenDiscover, onOpenRecommended,
                                onOpenSearch, onOpenBookmarks, onOpenFollow,
                            )
                            when {
                                error != null && list.isEmpty() -> Column(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
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

                                loading && list.isEmpty() -> LoadingState()
                            }
                            if (list.isEmpty() && error == null && TagBlock.patternCount() > 0) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        Str.blocked_all(),
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                            SmallTitle(text = Str.home_title())
                        }
                    },
                )
            } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                overscrollEffect = null,
                contentPadding = PaddingValues(top = frame.calculateTopPadding(), bottom = bottomPadding),
            ) {
            item {
                HomeEntries(onOpenRanking, onOpenNovelRanking, onOpenDiscover, onOpenRecommended, onOpenSearch, onOpenBookmarks, onOpenFollow)
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
            } else if (error == null && TagBlock.patternCount() > 0) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(Str.blocked_all(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
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
    onOpenFollow: () -> Unit,
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
            EntryCard(MiuixIcons.Promotions, Str.follow_new()) { onOpenFollow() }
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
