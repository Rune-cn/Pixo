package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 关注新作页：已关注画师的最新作品（网页 feed，分页加载）。 */
@Composable
fun FollowScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long, String) -> Unit,
    onOpenUser: (Long) -> Unit,
    waterfall: Boolean = false,
) {
    val feed = rememberPagedFeedByKey(
        cacheKey = "follow:latest",
        idOf = { it.id },
        pageSize = 60,
        restore = { null },
        persist = { _, _ -> },
        load = { page -> Page(PixivApi.fetchFollowLatest(page)) },
    )
    val items = feed.items
    val loadingMore = feed.loadingMore
    val hasMore = feed.hasMore
    val error = feed.error

    fun loadMore() = feed.loadMoreNow()
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.follow_new(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
            if (waterfall) {
                StaggeredWorkGrid(
                    works = items.orEmpty(),
                    onOpenDetail = { onOpenDetail(it, "illust") },
                    onOpenUser = onOpenUser,
                    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = innerPadding,
                    loadingMore = loadingMore,
                    hasMore = hasMore,
                    onLoadMore = { loadMore() },
                    error = error,
                    onRetry = { feed.retry() },
                    loading = items == null,
                )
            } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            overscrollEffect = null,
        ) {
            when {
                items == null && error != null -> item {
                    ErrorState(error ?: "", onRetry = { feed.retry() })
                }
                items == null -> item { LoadingState() }
                items.orEmpty().isEmpty() -> item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (TagBlock.patternCount() > 0) Str.blocked_all() else Str.follow_empty(),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                else -> {
                    items(items!!, key = { it.id }) { work ->
                        PixivWorkRow(work, onClick = { onOpenDetail(work.id, "illust") }, onOpenUser = onOpenUser)
                    }
                    item {
                        EndItem(loadingMore, hasMore, onRetry = { loadMore() })
                    }
                }
            }
        }
            }
    }
}
