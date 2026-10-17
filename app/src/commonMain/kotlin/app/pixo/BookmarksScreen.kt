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
fun BookmarksScreen(
    restrict: String,
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
    waterfall: Boolean = false,
) {
    var currentRestrict by rememberSaveable { mutableStateOf(restrict) }
    val cacheKey = "bookmarks:$currentRestrict"
    var items by remember(cacheKey, SettingsRevision.current) {
        mutableStateOf<List<PixivWork>?>(FeedCache.illust[cacheKey])
    }
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
            if (waterfall) {
                StaggeredWorkGrid(
                    works = list,
                    onOpenDetail = { onOpenDetail(it, "illust") },
                    onOpenUser = onOpenUser,
                    modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = innerPadding,
                    loadingMore = loadingMore,
                    hasMore = hasMore,
                    onLoadMore = { loadMore() },
                    header = {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModeChip("show", Str.bookmark_public_label(), currentRestrict) { currentRestrict = "show" }
                            ModeChip("hide", Str.bookmark_private_label(), currentRestrict) { currentRestrict = "hide" }
                        }
                    },
                    error = error,
                    onRetry = { retryKey++ },
                    loading = loading && list.isEmpty(),
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
}
