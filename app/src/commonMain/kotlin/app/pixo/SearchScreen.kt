package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SearchScreen(
    initialQuery: String,
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit = {},
    waterfall: Boolean = false,
) {
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var submitted by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var kind by rememberSaveable { mutableStateOf("illust") }
    // 有结果（返回/保留查询）时收起输入框，避免自动弹出输入法
    var expanded by rememberSaveable(initialQuery) { mutableStateOf(initialQuery.isBlank()) }
    val focusManager = LocalFocusManager.current
    val cacheKey = "search:$submitted:$kind"
    // 用户搜索每页 10 条，插画/小说每页 60 条
    val pageSize = if (kind == "user") 10 else 60
    val feed = rememberPagedFeedByKey(
        cacheKey = cacheKey,
        idOf = { itemIdOf(it) },
        pageSize = pageSize,
        enabled = submitted.isNotBlank(),
        restore = { key: String ->
            when (kind) {
                "novel" -> FeedCache.novels[key] as List<Any>?
                "user" -> FeedCache.users[key] as List<Any>?
                else -> FeedCache.illust[key] as List<Any>?
            }
        },
        persist = { key, list ->
            // 三类结果共用同一个缓存键，按类型分别存，返回时可原样恢复
            FeedCache.illust[key] = list.filterIsInstance<PixivWork>()
            FeedCache.novels[key] = list.filterIsInstance<PixivNovel>()
            FeedCache.users[key] = list.filterIsInstance<PixivUser>()
        },
        load = { page ->
            val batch: List<Any> = when (kind) {
                "novel" -> PixivApi.searchNovels(submitted, page)
                "user" -> PixivApi.searchUsers(submitted, page)
                else -> PixivApi.searchArtworks(submitted, page)
            }
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
            if (waterfall && kind == "illust") {
                StaggeredWorkGrid(
                    works = list.filterIsInstance<PixivWork>(),
                    onOpenDetail = { onOpenDetail(it, "illust") },
                    onOpenUser = onOpenUser,
                    modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = innerPadding,
                    loadingMore = loadingMore,
                    hasMore = hasMore,
                    onLoadMore = { loadMore() },
                    header = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ModeChip("illust", Str.illust(), kind) { kind = "illust" }
                                ModeChip("novel", Str.novel(), kind) { kind = "novel" }
                                ModeChip("user", Str.user(), kind) { kind = "user" }
                            }
                        }
                    },
                    error = error,
                    onRetry = { feed.retry() },
                    loading = loading && list.isEmpty(),
                    emptyHint = {
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text(Str.search_hint(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    },
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
                    error != null && list.isEmpty() -> item { ErrorState(error ?: "", onRetry = { feed.retry() }) }
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
}

/** 搜索结果/列表里任意条目的稳定 id（插画、小说、用户混排时去重用）。 */
private fun itemIdOf(it: Any): Long = when (it) {
    is PixivWork -> it.id
    is PixivNovel -> it.id
    is PixivUser -> it.id
    else -> 0L
}
