package app.pixo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Search
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
    val loggedIn = AppPrefs.cookie.isNotBlank() || PixivAuth.isTokenMode
    val feed = rememberPagedFeed(
        cacheKey = "home:feed",
        cache = FeedCache.illust,
        pageSize = 30,
        reloadKey = loggedIn,
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
    // 上滑收起底栏、下滑弹出（瀑布流走 StaggeredWorkGrid 内部的跟踪）
    TrackBottomBarScroll { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
    val list = items.orEmpty()

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
                                        ErrorState(error ?: "", onRetry = { feed.retry() })
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
                            ErrorState(error ?: "", onRetry = { feed.retry() })
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
