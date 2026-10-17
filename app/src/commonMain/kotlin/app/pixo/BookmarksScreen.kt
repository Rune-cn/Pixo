package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
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
