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
    val feed = rememberPagedFeed(
        cacheKey = "discover:feed",
        cache = FeedCache.illust,
        pageSize = 30,
        idOf = { it.id },
        load = { page -> Page(PixivApi.fetchTopFeed((page - 1) * 30, 30)) },
    )
    val items = feed.items
    val loading = feed.loading
    val loadingMore = feed.loadingMore
    val refreshing = feed.refreshing
    val hasMore = feed.hasMore
    val error = feed.error
    val listState = feed.listState

    fun refresh() = feed.refreshNow()
    fun loadMore() = feed.loadMoreNow()
    val list = items.orEmpty()

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
                    ErrorState(error ?: "", onRetry = { feed.retry() })
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
    val feed = rememberPagedFeed(
        cacheKey = "recommend:feed",
        cache = FeedCache.illust,
        idOf = { it.id },
        // 推荐接口一次返回整块内容，没有下一页
        load = { Page(PixivApi.fetchTopArtworks(), hasMore = false) },
    )
    val items = feed.items
    val loading = feed.loading
    val refreshing = feed.refreshing
    val error = feed.error
    val listState = feed.listState

    fun refresh() = feed.refreshNow()
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
                    ErrorState(error ?: "", onRetry = { feed.retry() })
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
