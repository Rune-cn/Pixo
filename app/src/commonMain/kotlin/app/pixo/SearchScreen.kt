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
    // 从缓存恢复，返回时保留原列表与滚动位置
    var items by remember(cacheKey, SettingsRevision.current) {
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
                    onRetry = { retryKey++ },
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
}

/** 搜索结果/列表里任意条目的稳定 id（插画、小说、用户混排时去重用）。 */
private fun itemIdOf(it: Any): Long = when (it) {
    is PixivWork -> it.id
    is PixivNovel -> it.id
    is PixivUser -> it.id
    else -> 0L
}
