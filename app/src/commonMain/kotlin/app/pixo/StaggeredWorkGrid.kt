package app.pixo

// ===== 两列瀑布流作品网格 =====
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 两列瀑布流作品网格：缩略图高度随图片比例自适应形成错落；
 * 滚到接近末尾时触发加载更多（懒加载）；底部显示加载/无更多提示。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StaggeredWorkGrid(
    works: List<PixivWork>,
    onOpenDetail: (Long) -> Unit,
    onOpenUser: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    loadingMore: Boolean = false,
    hasMore: Boolean = true,
    onLoadMore: () -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    error: String? = null,
    onRetry: () -> Unit = {},
    loading: Boolean = false,
    emptyHint: (@Composable () -> Unit)? = null,
    userScrollEnabled: Boolean = true,
) {
    val gridState = rememberLazyStaggeredGridState()

    // 懒加载：最后一个可见项接近末尾（剩余 <= 4）时触发加载更多
    LaunchedEffect(gridState, works.size) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && last >= total - 4
        }.collect { nearEnd -> if (nearEnd) onLoadMore() }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        userScrollEnabled = userScrollEnabled,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
    ) {
        if (header != null) {
            item(span = StaggeredGridItemSpan.FullLine) { header() }
        }
        when {
            error != null && works.isEmpty() -> item(span = StaggeredGridItemSpan.FullLine) {
                ErrorState(error, onRetry)
            }
            loading && works.isEmpty() -> item(span = StaggeredGridItemSpan.FullLine) {
                LoadingState()
            }
            works.isEmpty() && emptyHint != null -> item(span = StaggeredGridItemSpan.FullLine) {
                emptyHint()
            }
        }
        items(works, key = { it.id }) { work ->
            StaggeredWorkCard(
                work = work,
                onClick = { onOpenDetail(work.id) },
                onOpenUser = { onOpenUser(work.authorId) },
            )
        }
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    loadingMore -> Text(
                        Str.load_more(),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    !hasMore -> Text(
                        Str.no_more(),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

/** 单个瀑布流卡片：缩略图（等比高度）+ 标题 + 作者 + R-18/多图角标。 */
@Composable
private fun StaggeredWorkCard(
    work: PixivWork,
    onClick: () -> Unit,
    onOpenUser: () -> Unit,
) {
    var showAuthorMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column {
            // 缩略图：图片解码后按实际比例撑高（瀑布流错落）；拿不到比例（加载中/无固有尺寸）用 0.75 兜底
            var thumbPainter by remember(work.thumb) { mutableStateOf<Painter?>(null) }
            val ratio = thumbPainter?.let { painter ->
                val size = painter.intrinsicSize
                if (size.width.isFinite() && size.height.isFinite() && size.width > 0f) {
                    size.height / size.width
                } else {
                    0.75f
                }
            } ?: 0.75f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
            ) {
                val ctx = LocalPlatformContext.current
                AsyncImage(
                    model = remember(work.thumb) {
                        ImageRequest.Builder(ctx).data(work.thumb).crossfade(true).build()
                    },
                    contentDescription = work.title,
                    contentScale = ContentScale.FillWidth,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success) thumbPainter = state.painter
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (work.isR18) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE53935))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(Str.r18(), style = MiuixTheme.textStyles.footnote1, color = Color.White)
                    }
                }
                if (work.pageCount > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text("×${work.pageCount}", style = MiuixTheme.textStyles.footnote1, color = Color.White)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        work.title,
                        style = MiuixTheme.textStyles.body2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showAuthorMenu = true }) {
                        Icon(imageVector = MiuixIcons.More, contentDescription = Str.more_actions())
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuthorAvatar(work.avatar, 14.dp, work.authorId, onOpenUser = { onOpenUser() })
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Str.author_prefix() + work.author,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    if (showAuthorMenu) {
        OverlayDialog(show = true, onDismissRequest = { showAuthorMenu = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("#" + work.author, style = MiuixTheme.textStyles.title3)
                Button(onClick = {
                    val add = work.author
                    val cur = AppPrefs.blockedAuthors.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
                    if (cur.none { it.equals(add, ignoreCase = true) }) {
                        AppPrefs.blockedAuthors = (cur + add).joinToString("\n")
                        invalidateFeedCaches()
                    }
                    showAuthorMenu = false
                }, modifier = Modifier.fillMaxWidth()) { Text(Str.block_this_author()) }
                Button(onClick = { showAuthorMenu = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
            }
        }
    }

}
