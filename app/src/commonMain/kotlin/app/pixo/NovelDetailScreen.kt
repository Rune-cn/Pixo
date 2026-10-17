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
internal fun NovelDetail(
    workId: Long,
    innerPadding: PaddingValues,
    onSearchTag: (String) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenComments: (Long, String) -> Unit,
) {
    val cachedDetail = remember(workId) { FeedCache.novelDetail[workId] }
    var detail by remember { mutableStateOf(cachedDetail) }
    var loading by remember { mutableStateOf(cachedDetail == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var tagAction by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var bookmarkBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sTagBlockedFormat = Str.tag_blocked("%s")

    val sBookmarkedPublic = Str.bookmarked_public()
    val sBookmarkedPrivate = Str.bookmarked_private()
    val sBookmarkRemoved = Str.bookmark_removed()

    LaunchedEffect(workId, retryKey) {
        // 已缓存则直接使用，避免重复加载
        if (FeedCache.novelDetail[workId] != null && retryKey == 0) {
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            val d = PixivApi.fetchNovelDetail(workId)
            FeedCache.novelDetail[workId] = d
            detail = d
            // 记录本地浏览历史（小说）
            HistoryStore.add(HistoryItem(workId, "novel", d.title, "", System.currentTimeMillis()))
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load failed", e)
        }
        loading = false
    }

    fun toggleBookmark(restrict: Int) {
        if (bookmarkBusy) return
        scope.launch {
            bookmarkBusy = true
            try {
                val cur = detail
                if (restrict < 0) {
                    PixivApi.removeBookmark("novel", workId, cur?.bookmarkId ?: 0)
                    detail = cur?.copy(isBookmarked = false, bookmarkPrivate = false)
                    feedback = sBookmarkRemoved
                } else {
                    if (cur?.isBookmarked == true && cur.bookmarkPrivate != (restrict == 1)) {
                        PixivApi.removeBookmark("novel", workId, cur.bookmarkId)
                    }
                    PixivApi.addNovelBookmark(workId, restrict)
                    detail = cur?.copy(isBookmarked = true, bookmarkPrivate = restrict == 1)
                    feedback = if (restrict == 0) sBookmarkedPublic else sBookmarkedPrivate
                }
            } catch (e: Exception) {
                feedback = "收藏失败：${e.message}"
            }
            detail?.let { FeedCache.novelDetail[workId] = it }
            bookmarkBusy = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            loading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { LoadingState() }
            error != null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                ErrorState(error ?: "", onRetry = { retryKey++ })
            }

            else -> {
                val d = detail ?: return@Box
                val clipboard = LocalClipboardManager.current
                LazyColumn(contentPadding = innerPadding, modifier = Modifier.scrollEndHaptic().overScrollVertical()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(d.title, style = MiuixTheme.textStyles.title3)
                            Spacer(Modifier.height(10.dp))
                            AuthorHeader(name = d.author, userId = d.authorId, avatar = d.avatar, onOpenUser = onOpenUser)
                            Spacer(Modifier.height(10.dp))
                            if (d.tags.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    d.tags.forEach { tag ->
                                        TextTagChip(tag = tag, onClick = { onSearchTag(tag) }, onLongClick = {
                                            tagAction = tag
                                        })
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            if (d.content.isBlank()) {
                                Text(Str.novel_detail() + "：正文不可用", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            } else {
                                Text(d.content, style = MiuixTheme.textStyles.body1)
                            }
                        }
                    }
                }
                BottomToast(feedback)
                // 长按标签：选择复制或屏蔽（屏蔽的标签进入黑名单正则过滤）
                OverlayDialog(show = tagAction != null, onDismissRequest = { tagAction = null }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val clipboard = LocalClipboardManager.current
                        val tagText = tagAction.orEmpty()
                        Text("#$tagText", style = MiuixTheme.textStyles.title3)
                        Button(onClick = {
                            clipboard.setText(AnnotatedString(tagText))
                            tagAction = null
                        }, modifier = Modifier.fillMaxWidth()) { Text(Str.copy_tag()) }
                        Button(onClick = {
                            val add = tagText
                            val cur = AppPrefs.tagBlockKeywords.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
                            if (cur.none { it.equals(add, ignoreCase = true) }) {
                                AppPrefs.tagBlockKeywords = (cur + add).joinToString("\n")
                                invalidateFeedCaches()
                            }
                            feedback = sTagBlockedFormat.replace("%s", tagText)
                            tagAction = null
                        }, modifier = Modifier.fillMaxWidth()) { Text(Str.block_tag()) }
                        Button(onClick = { tagAction = null }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
                    }
                }
            }
        }

        var showBookmarkMenu by remember { mutableStateOf(false) }
        FloatingButton(
            onClick = { showBookmarkMenu = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 64.dp),
        ) {
            Icon(imageVector = MiuixIcons.Favorites, contentDescription = Str.bookmark_public_label())
        }
        FloatingButton(
            onClick = { onOpenComments(workId, "novel") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(imageVector = MiuixIcons.Messages, contentDescription = Str.comments())
        }

        OverlayDialog(show = showBookmarkMenu, onDismissRequest = { showBookmarkMenu = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Str.bookmark_public_label(), style = MiuixTheme.textStyles.title3)
                val isBm = detail?.isBookmarked == true
                if (!isBm) {
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(0)
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.bookmark_public()) }
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(1)
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.bookmark_private()) }
                } else {
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(if (detail?.bookmarkPrivate == true) 0 else 1)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (detail?.bookmarkPrivate == true) Str.bookmark_public() else Str.bookmark_private())
                    }
                    Button(onClick = {
                        showBookmarkMenu = false
                        toggleBookmark(-1)
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.unbookmark()) }
                }
                Button(onClick = { showBookmarkMenu = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
            }
        }
    }
}
