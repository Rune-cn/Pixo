package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
fun RankingScreen(
    mode: String,
    content: String = "illust",
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
    waterfall: Boolean = false,
) {
    var currentMode by rememberSaveable { mutableStateOf(mode) }
    val cacheKey = "rank:$content:$currentMode"
    val feed = rememberPagedFeed(
        cacheKey = cacheKey,
        cache = FeedCache.illust,
        pageSize = 50,
        idOf = { it.id },
        load = { page ->
            val batch = if (content == "manga") PixivApi.fetchMangaRanking(currentMode, page)
            else PixivApi.fetchRanking(currentMode, page)
            Page(batch)
        },
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
            if (waterfall) {
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
                    header = {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModeChip("daily", Str.daily(), currentMode) { currentMode = "daily" }
                            ModeChip("weekly", Str.weekly(), currentMode) { currentMode = "weekly" }
                            ModeChip("monthly", Str.monthly(), currentMode) { currentMode = "monthly" }
                        }
                    },
                    error = error,
                    onRetry = { feed.retry() },
                    loading = loading && list.isEmpty(),
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
                        ModeChip("daily", Str.daily(), currentMode) { currentMode = "daily" }
                        ModeChip("weekly", Str.weekly(), currentMode) { currentMode = "weekly" }
                        ModeChip("monthly", Str.monthly(), currentMode) { currentMode = "monthly" }
                    }
                }
                when {
                    loading && list.isEmpty() -> item { LoadingState() }
                    error != null && list.isEmpty() -> item { ErrorState(error ?: "", onRetry = { feed.retry() }) }
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
}


@Composable
fun NovelRankingScreen(
    mode: String,
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
    waterfall: Boolean = false,
) {
    var currentMode by rememberSaveable { mutableStateOf(mode) }
    val cacheKey = "novelRank:$currentMode"
    val feed = rememberPagedFeed(
        cacheKey = cacheKey,
        cache = FeedCache.novels,
        pageSize = 50,
        idOf = { it.id },
        load = { page -> Page(PixivApi.fetchNovelRanking(currentMode, page)) },
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
                    error != null && list.isEmpty() -> item { ErrorState(error ?: "", onRetry = { feed.retry() }) }
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
