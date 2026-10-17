package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 发现板块：发现作品流（无限滚动 + 下拉刷新，返回恢复原位）。 */
@Composable
fun DiscoverScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
    waterfall: Boolean = false,
) {
    val cacheKey = "discover:feed"
    var items by remember(cacheKey, SettingsRevision.current) {
        mutableStateOf<List<PixivWork>?>(FeedCache.illust[cacheKey])
    }
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
                else -> if (waterfall) {
                    StaggeredWorkGrid(
                        works = list,
                        onOpenDetail = { onOpenDetail(it, "illust") },
                        onOpenUser = onOpenUser,
                        modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = innerPadding,
                        loadingMore = loadingMore,
                        hasMore = hasMore,
                        onLoadMore = { loadMore() },
                    )
                } else {
                    LazyColumn(
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

}


/** 推荐板块：Pixiv 推荐作品（下拉刷新，恢复滚动）。 */
@Composable
fun RecommendedScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
    waterfall: Boolean = false,
) {
    val cacheKey = "recommend:feed"
    var items by remember(cacheKey, SettingsRevision.current) {
        mutableStateOf<List<PixivWork>?>(FeedCache.illust[cacheKey])
    }
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
                else -> if (waterfall) {
                    StaggeredWorkGrid(
                        works = list,
                        onOpenDetail = { onOpenDetail(it, "illust") },
                        onOpenUser = onOpenUser,
                        modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = innerPadding,
                    )
                } else {
                    LazyColumn(
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
}
