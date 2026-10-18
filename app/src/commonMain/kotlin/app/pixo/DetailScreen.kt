package app.pixo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun DetailScreen(
    workId: Long,
    kind: String,
    onBack: () -> Unit,
    onSearchTag: (String) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenComments: (Long, String) -> Unit,
    onOpenDetail: (Long, String) -> Unit,
) {
    // 顶栏「下载原图」触发信号：传给 IllustDetail，由它拿原图数据发起下载
    var downloadSignal by remember { mutableIntStateOf(0) }
    PixoSubScreen(title = "", onBack = onBack, actions = {
        var showMenu by remember { mutableStateOf(false) }
        IconButton(onClick = { showMenu = true }) {
            Icon(imageVector = MiuixIcons.More, contentDescription = Str.more_actions())
        }
        if (showMenu) {
            OverlayDialog(show = true, onDismissRequest = { showMenu = false }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Str.more_actions(), style = MiuixTheme.textStyles.title3)
                    Button(onClick = {
                        showMenu = false
                        openUrlInBrowser(
                            if (kind == "novel") "https://www.pixiv.net/novel/show.php?id=$workId"
                            else "https://www.pixiv.net/artworks/$workId",
                        )
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.open_in_browser()) }
                    Button(onClick = {
                        showMenu = false
                        shareText(
                            if (kind == "novel") "https://www.pixiv.net/novel/show.php?id=$workId"
                            else "https://www.pixiv.net/artworks/$workId",
                        )
                    }, modifier = Modifier.fillMaxWidth()) { Text(Str.share()) }
                    if (kind != "novel") {
                        Button(onClick = {
                            showMenu = false
                            downloadSignal++
                        }, modifier = Modifier.fillMaxWidth()) { Text(Str.download_original()) }
                    }
                    Button(onClick = { showMenu = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
                }
            }
        }
    }) { innerPadding ->
        if (kind == "novel") {
            NovelDetail(workId, innerPadding, onSearchTag, onOpenUser, onOpenComments)
        } else {
            IllustDetail(workId, innerPadding, onSearchTag, onOpenUser, onOpenComments, downloadSignal, onOpenDetail)
        }
    }
}


@Composable
private fun IllustDetail(
    workId: Long,
    innerPadding: PaddingValues,
    onSearchTag: (String) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenComments: (Long, String) -> Unit,
    downloadSignal: Int = 0,
    onOpenDetail: (Long, String) -> Unit = { _, _ -> },
) {
    val cachedDetail = remember(workId) { FeedCache.illustDetail[workId] }
    val cachedPages = remember(workId) { FeedCache.illustPages[workId] }
    var detail by remember { mutableStateOf(cachedDetail) }
    var pages by remember { mutableStateOf(cachedPages.orEmpty()) }
    var loading by remember { mutableStateOf(cachedDetail == null || cachedPages == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var tagAction by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var bookmarkBusy by remember { mutableStateOf(false) }
    var lightboxIndex by remember { mutableIntStateOf(-1) }
    var ugoira by remember(workId) { mutableStateOf<PixivUgoira?>(null) }
    var ugoiraPlaying by remember(workId) { mutableStateOf(!AppPrefs.ugoiraConfirm) }
    var downloadBusy by remember { mutableStateOf(false) }
    var downloadDialog by remember { mutableStateOf(false) }
    var downloadToast by remember { mutableStateOf<String?>(null) }
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val scope = rememberCoroutineScope()

    // 预取文案（协程内不能用 @Composable 资源访问）
    val sBookmarkedPublic = Str.bookmarked_public()
    val sBookmarkedPrivate = Str.bookmarked_private()
    val sBookmarkRemoved = Str.bookmark_removed()
    val sDownloadFailed = Str.download_failed()
    // 预取带占位数字的完成文案（协程内不能用 @Composable 资源访问）
    val sDownloadDoneFormat = Str.download_done(123456).replace("123456", "%s")
    val sTagBlockedFormat = Str.tag_blocked("%s")

    LaunchedEffect(workId, retryKey) {
        // 已缓存则直接使用，避免重复加载
        val cachedDetail = FeedCache.illustDetail[workId]
        if (cachedDetail != null && FeedCache.illustPages[workId] != null && retryKey == 0) {
            if (cachedDetail.illustType == 2 && ugoira == null) {
                try {
                    ugoira = PixivApi.fetchUgoiraMeta(workId)
                } catch (e: Exception) {
                    logError("Pixo", "ugoira meta fail", e)
                }
            }
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            val d = PixivApi.fetchIllustDetail(workId)
            val p = PixivApi.fetchIllustPages(workId)
            FeedCache.illustDetail[workId] = d
            FeedCache.illustPages[workId] = p
            detail = d
            pages = p
            // 记录本地浏览历史（插画/漫画，缩略图取第一页）
            HistoryStore.add(HistoryItem(workId, "illust", d.title, p.firstOrNull()?.url.orEmpty(), System.currentTimeMillis()))
            // ugoira 动图：详情命中 illustType=2 时取帧元数据，失败不影响正文
            if (d.illustType == 2) {
                try {
                    ugoira = PixivApi.fetchUgoiraMeta(workId)
                } catch (e: Exception) {
                    logError("Pixo", "ugoira meta fail", e)
                }
            }
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
                    // 取消收藏
                    PixivApi.removeBookmark("illust", workId, cur?.bookmarkId ?: 0)
                    detail = cur?.copy(isBookmarked = false, bookmarkPrivate = false)
                    feedback = sBookmarkRemoved
                } else {
                    // 切换公开/私密：先取消再重新收藏，兼容网页与 App API
                    if (cur?.isBookmarked == true && cur.bookmarkPrivate != (restrict == 1)) {
                        PixivApi.removeBookmark("illust", workId, cur.bookmarkId)
                    }
                    PixivApi.addBookmark(workId, restrict)
                    detail = cur?.copy(isBookmarked = true, bookmarkPrivate = restrict == 1)
                    feedback = if (restrict == 0) sBookmarkedPublic else sBookmarkedPrivate
                }
            } catch (e: Exception) {
                feedback = "收藏失败：${e.message}"
            }
            detail?.let { FeedCache.illustDetail[workId] = it }
            bookmarkBusy = false
        }
    }

    fun fileNameFor(page: PixivPage, index: Int): String {
        val url = page.original.ifBlank { page.url }
        val ext = url.substringAfterLast('.', "jpg").substringBefore('?')
            .takeIf { it.isNotBlank() && it.length <= 5 } ?: "jpg"
        return "${workId}_p$index.$ext"
    }

    fun downloadOne(page: PixivPage, index: Int) {
        if (downloadBusy) return
        scope.launch {
            downloadBusy = true
            try {
                val saved = PixivApi.downloadImage(page.original.ifBlank { page.url }, fileNameFor(page, index))
                downloadToast = saved ?: sDownloadFailed
            } catch (e: Exception) {
                logError("Pixo", "download failed", e)
                downloadToast = sDownloadFailed + "：" + (e.message ?: "")
            }
            downloadBusy = false
        }
    }

    fun downloadSelected() {
        if (downloadBusy) return
        scope.launch {
            downloadBusy = true
            var ok = 0
            var fail = 0
            for (index in selectedPages.toSortedSet()) {
                val page = pages.getOrNull(index) ?: continue
                try {
                    // writeDownloadFile 写失败返回 null（不抛异常），必须以返回值统计，
                    // 否则 SAF 目录失效时会“假成功”
                    val saved = PixivApi.downloadImage(page.original.ifBlank { page.url }, fileNameFor(page, index))
                    if (saved != null) ok++ else fail++
                } catch (e: Exception) {
                    logError("Pixo", "download page $index failed", e)
                    fail++
                }
            }
            downloadToast = if (fail == 0) sDownloadDoneFormat.format(ok) else "$sDownloadFailed（$fail）"
            downloadBusy = false
            downloadDialog = false
        }
    }

    // 顶栏「下载原图」：单图直接下载，多图弹选择对话框（默认全选）
    LaunchedEffect(downloadSignal) {
        if (downloadSignal <= 0) return@LaunchedEffect
        val p = pages
        if (p.isEmpty()) return@LaunchedEffect
        if (p.size == 1) {
            downloadOne(p[0], 0)
        } else {
            selectedPages = p.indices.toSet()
            downloadDialog = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            loading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { LoadingState() }
            error != null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                ErrorState(error ?: "", onRetry = { retryKey++ })
            }

            else -> {
                val d = detail
                val relatedWorks = rememberRelatedWorks(workId)
                LazyColumn(contentPadding = innerPadding, modifier = Modifier.scrollEndHaptic().overScrollVertical()) {
                    itemsIndexed(pages) { index, page ->
                        val isUgoira = ugoira != null && index == 0
                        Box(Modifier.fillMaxWidth().clickable { lightboxIndex = index }) {
                            if (isUgoira) {
                                if (ugoiraPlaying) {
                                    UgoiraAsyncImage(
                                        workId = workId,
                                        fallbackUrl = page.url,
                                        contentDescription = d?.title ?: "",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth(),
                                        playing = true,
                                    )
                                } else {
                                    val ctx = LocalPlatformContext.current
                                    AsyncImage(
                                        model = remember(page.url) { ImageRequest.Builder(ctx).data(page.url).crossfade(true).build() },
                                        contentDescription = d?.title ?: "",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    // 手动确认播放：点击覆盖层开始动图
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { ugoiraPlaying = true },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                        ) {
                                            Text(Str.play_animation(), style = MiuixTheme.textStyles.body2, color = Color.White)
                                        }
                                    }
                                }
                            } else {
                                val ctx = LocalPlatformContext.current
                                AsyncImage(
                                    model = remember(page.url) { ImageRequest.Builder(ctx).data(page.url).crossfade(true).build() },
                                    contentDescription = d?.title ?: "",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (isUgoira) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(Str.animated_badge(), style = MiuixTheme.textStyles.footnote2, color = Color.White)
                                }
                            }
                        }
                    }
                    item {
                        IllustInfo(
                            d = d,
                            onSearchTag = onSearchTag,
                            onOpenUser = onOpenUser,
                            onLongPressTag = { tag -> tagAction = tag },
                        )
                    }
                    relatedWorksSection(
                        works = relatedWorks,
                        onOpenDetail = onOpenDetail,
                    )
                }
                BottomToast(feedback)
                // 长按标签：复制 / 屏蔽（共享动作面板，见 BlockActions.kt）
                TagActionDialog(
                    tag = tagAction,
                    onDismiss = { tagAction = null },
                    onBlocked = { feedback = sTagBlockedFormat.replace("%s", it) },
                )
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
            onClick = { onOpenComments(workId, "illust") },
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

        OverlayDialog(show = downloadDialog, onDismissRequest = { if (!downloadBusy) downloadDialog = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Str.download_selected_hint(), style = MiuixTheme.textStyles.title3)
                Button(
                    onClick = {
                        selectedPages = if (selectedPages.size == pages.size) emptySet() else pages.indices.toSet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (selectedPages.size == pages.size) Str.deselect_all() else Str.select_all()) }
                LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(pages.size) { index ->
                        val checked = index in selectedPages
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedPages = if (checked) selectedPages - index else selectedPages + index }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (checked) MiuixTheme.colorScheme.primary else Color.Transparent)
                                    .border(1.5.dp, if (checked) Color.Transparent else MiuixTheme.colorScheme.onSurfaceVariantSummary, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (checked) {
                                    Text("✓", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onPrimary)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("${index + 1} / ${pages.size}", style = MiuixTheme.textStyles.body2)
                        }
                    }
                }
                Button(
                    onClick = { if (!downloadBusy && selectedPages.isNotEmpty()) downloadSelected() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Str.download()) }
                Button(onClick = { downloadDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
            }
        }

        BottomToast(downloadToast)

        if (lightboxIndex >= 0) {
            ImageLightbox(
                pages = pages,
                ugoira = ugoira,
                workId = workId,
                initialIndex = lightboxIndex,
                onClose = { lightboxIndex = -1 },
            )
        }
    }
}


/** 图片灯箱：点击图片全屏查看，横向滑动翻页。
 *  缩放交给成熟的 telephoto zoomable（双指捏合、缩放后拖动、双击放大/还原）；
 *  未放大时单指滑动交给 Pager 翻页，放大后 Pager 自动锁定。 */
@Composable
private fun ImageLightbox(
    pages: List<PixivPage>,
    ugoira: PixivUgoira?,
    workId: Long,
    initialIndex: Int,
    onClose: () -> Unit,
) {
    val ctx = LocalPlatformContext.current
    val safeIndex = initialIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeIndex) { pages.size.coerceAtLeast(1) }
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 6f))
    val isZoomed = (zoomableState.zoomFraction ?: 0f) > 0.001f
    var ugoiraPlaying by remember(workId) { mutableStateOf(!AppPrefs.ugoiraConfirm) }

    // 切页时回到未缩放状态
    LaunchedEffect(pagerState.currentPage) { zoomableState.resetZoom() }
    SystemBackHandler(enabled = true) { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isZoomed,
            modifier = Modifier.fillMaxSize(),
            overscrollEffect = null,
        ) { index ->
            val page = pages.getOrNull(index) ?: return@HorizontalPager
            val zoomModifier = Modifier
                .fillMaxSize()
                .zoomable(
                    state = zoomableState,
                    onClick = {
                        if ((zoomableState.zoomFraction ?: 0f) <= 0.001f) onClose()
                    },
                )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (ugoira != null && index == 0 && ugoiraPlaying) {
                    UgoiraAsyncImage(
                        workId = workId,
                        fallbackUrl = page.original.ifBlank { page.url },
                        contentDescription = "illust",
                        contentScale = ContentScale.Fit,
                        modifier = zoomModifier,
                        playing = true,
                    )
                } else {
                    AsyncImage(
                        model = remember(page.original.ifBlank { page.url }) { ImageRequest.Builder(ctx).data(page.original.ifBlank { page.url }).crossfade(true).build() },
                        contentDescription = "illust",
                        contentScale = ContentScale.Fit,
                        modifier = zoomModifier,
                    )
                    if (ugoira != null && index == 0 && !ugoiraPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { ugoiraPlaying = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text(Str.play_animation(), style = MiuixTheme.textStyles.body2, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        // 页码指示
        Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                text = "${pagerState.currentPage + 1} / ${pages.size}",
                style = MiuixTheme.textStyles.body2,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        // 关闭按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = MiuixIcons.Back, contentDescription = Str.action_close(), tint = Color.White)
        }
    }
}


/** 头像全屏放大预览：黑底 + 双指缩放，未放大时点击或返回键关闭。 */
@Composable
internal fun AvatarPreview(url: String, onClose: () -> Unit) {
    val ctx = LocalPlatformContext.current
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 6f))
    val isZoomed = (zoomableState.zoomFraction ?: 0f) > 0.001f
    SystemBackHandler(enabled = true) { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = remember(url) { ImageRequest.Builder(ctx).data(url).crossfade(true).build() },
                contentDescription = Str.avatar_desc(),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(
                        state = zoomableState,
                        onClick = {
                            if ((zoomableState.zoomFraction ?: 0f) <= 0.001f) onClose()
                        },
                    ),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = MiuixIcons.Back, contentDescription = Str.action_close(), tint = Color.White)
        }
    }
}



/** ugoira 动图播放：下载 zip → 解压帧 → 按帧延时轮播；失败时回退到静态图。 */
@Composable
private fun UgoiraAsyncImage(
    workId: Long,
    fallbackUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    playing: Boolean = true,
) {
    val ctx = LocalPlatformContext.current
    var frameBytes by remember(workId) { mutableStateOf(UgoiraCache.get(workId)?.frames.orEmpty()) }
    var delays by remember(workId) { mutableStateOf(UgoiraCache.get(workId)?.delays.orEmpty()) }
    var index by remember(workId) { mutableIntStateOf(0) }

    LaunchedEffect(workId) {
        if (UgoiraCache.get(workId) != null) return@LaunchedEffect
        try {
            val meta = PixivApi.fetchUgoiraMeta(workId)
            val bytes = loadUgoiraFrames(meta.zipUrl, meta.frames.map { it.file })
            if (bytes != null && bytes.isNotEmpty()) {
                val data = UgoiraFrameData(bytes, meta.frames.map { it.delayMs })
                UgoiraCache.put(workId, data)
                frameBytes = data.frames
                delays = data.delays
            }
        } catch (e: Exception) {
            logError("Pixo", "ugoira load fail", e)
        }
    }
    LaunchedEffect(frameBytes, playing) {
        if (frameBytes.isEmpty() || !playing) return@LaunchedEffect
        while (true) {
            delay(delays.getOrElse(index % frameBytes.size) { 125 }.toLong())
            index = (index + 1) % frameBytes.size
        }
    }
    // 帧字节直接解码为 ImageBitmap 再用 Image 绘制，避免每帧走 Coil 请求造成闪烁
    // 暂停时固定显示首帧（zip 首帧即静态图），不切回 Coil，避免“播放/暂停”反复闪烁
    val frame = if (frameBytes.isEmpty()) null
    else frameBytes.getOrNull((if (playing) index else 0) % frameBytes.size)
    val frameBitmap = remember(frame) {
        frame?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
    }
    if (frameBitmap != null) {
        Image(
            bitmap = frameBitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        AsyncImage(
            model = remember(fallbackUrl) { ImageRequest.Builder(ctx).data(fallbackUrl).crossfade(true).build() },
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}


@Composable
private fun IllustInfo(
    d: PixivIllustDetail?,
    onSearchTag: (String) -> Unit,
    onOpenUser: (Long) -> Unit,
    onLongPressTag: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(d?.title ?: "", style = MiuixTheme.textStyles.title3)
        Spacer(Modifier.height(10.dp))
        AuthorHeader(name = d?.author.orEmpty(), userId = d?.authorId ?: 0, avatar = d?.avatar.orEmpty(), onOpenUser = onOpenUser)
        Spacer(Modifier.height(10.dp))
        if (d?.tags?.isNotEmpty() == true) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                d.tags.forEachIndexed { index, tag ->
                    TextTagChip(
                        tag = tag,
                        translation = d.tagTranslations.getOrNull(index).orEmpty(),
                        onClick = { onSearchTag(tag) },
                        onLongClick = { onLongPressTag(tag) },
                    )
                }
            }
        }
    }
}


/** 作者信息行：头像 + 名字，点击进入作者主页。 */
@Composable
internal fun AuthorHeader(name: String, userId: Long, avatar: String, onOpenUser: (Long) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpenUser(userId) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuthorAvatar(avatar, 40.dp, userId, onOpenUser)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MiuixTheme.textStyles.body1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Str.author_page(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Text("›", style = MiuixTheme.textStyles.title3, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}


/** 收藏操作：未收藏时提供公开/私人；已收藏时提供切换与取消。 */
@Composable
private fun BookmarkButtons(
    isBookmarked: Boolean,
    bookmarkPrivate: Boolean,
    busy: Boolean,
    onBookmark: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isBookmarked) {
            Button(onClick = { onBookmark(0) }, enabled = !busy) { Text(Str.bookmark_public()) }
            Button(onClick = { onBookmark(1) }, enabled = !busy) { Text(Str.bookmark_private()) }
        } else {
            if (bookmarkPrivate) {
                Button(onClick = { onBookmark(0) }, enabled = !busy) { Text(Str.bookmark_public()) }
            } else {
                Button(onClick = { onBookmark(1) }, enabled = !busy) { Text(Str.bookmark_private()) }
            }
            Button(onClick = { onBookmark(-1) }, enabled = !busy) { Text(Str.unbookmark()) }
        }
    }
}


/**
 * 相关作品数据：命中缓存直接用，否则请求一次。
 *
 * 单独抽出来是为了把渲染交给父级 LazyColumn（见 [relatedWorksSection]）——
 * 在 LazyColumn 里再嵌一个可滚动容器会触发无限高度约束崩溃，
 * 之前正是因为这个才「只取前 4 个」来规避。
 */
@Composable
private fun rememberRelatedWorks(workId: Long): List<PixivWork>? {
    var works by remember(workId, SettingsRevision.current) {
        mutableStateOf(FeedCache.relatedWorks[workId])
    }
    LaunchedEffect(workId) {
        if (works == null) {
            works = try {
                PixivApi.fetchRelatedWorks(workId).also { FeedCache.relatedWorks[workId] = it }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
    return works
}

/**
 * 相关作品区：标题 + 两列网格。
 *
 * 每一行都是父级 LazyColumn 的独立 item（可虚拟化），
 * 所以不再需要「只取前 4 个」：接口返回多少就展示多少。
 */
private fun LazyListScope.relatedWorksSection(
    works: List<PixivWork>?,
    onOpenDetail: (Long, String) -> Unit,
) {
    item(key = "relatedTitle") {
        Text(
            text = Str.related_works(),
            style = MiuixTheme.textStyles.title3,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )
    }
    if (works == null || works.isEmpty()) {
        // 加载中或加载失败：给出占位，避免「相关推荐区凭空消失」
        item(key = "relatedPlaceholder") {
            Box(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (works == null) Str.related_loading() else Str.related_empty(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        return
    }
    items(
        items = works.chunked(2),
        key = { row -> row.joinToString("-") { it.id.toString() } },
    ) { pair ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pair.forEach { work ->
                RelatedWorkCard(
                    work = work,
                    onClick = { onOpenDetail(work.id, "illust") },
                    modifier = Modifier.weight(1f),
                )
            }
            // 奇数个时补位，保持卡片左对齐
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun RelatedWorkCard(work: PixivWork, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalPlatformContext.current
    Column(
        modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = remember(work.thumb) { ImageRequest.Builder(ctx).data(work.thumb).crossfade(true).build() },
                contentDescription = work.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (work.isR18) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("R-18", style = MiuixTheme.textStyles.footnote2, color = Color.White)
                }
            }
        }
        Text(
            text = work.title,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
