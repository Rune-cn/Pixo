package app.pixo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 评论区（独立页面）。
 *
 * MIUI 卡片风格：头像 + 昵称 + 日期 + 内容的评论卡片列表，底部用 MIUI 控件发布评论。
 */
@Composable
fun CommentsScreen(
    workId: Long,
    kind: String,
    onBack: () -> Unit,
    onOpenUser: (Long) -> Unit,
) {
    val cacheKey = "$kind:$workId"
    var comments by remember(cacheKey) { mutableStateOf(FeedCache.comments[cacheKey]) }
    var loading by remember { mutableStateOf(comments == null) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var posting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackError by remember { mutableStateOf(false) }
    var copyMenu by remember { mutableStateOf<String?>(null) }
    var copySelect by remember { mutableStateOf<String?>(null) }
    /** 正在回复的目标：(评论 id, 用户名)；为空表示发表一级评论。 */
    var replyTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }
    /** 评论/追评刷新标记：发评论后 +1，触发已展开的追评区重拉数据。 */
    var refreshTick by remember { mutableIntStateOf(0) }
    val replyStates = remember { mutableStateOf(emptyMap<Long, ReplyUiState>()) }
    var replyStatesMap by replyStates

    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val sPosted = Str.comment_posted()
    val sFailPrefix = Str.comment_post_failed("")
    val sHint = Str.comment_hint()
    val sSend = Str.comment_send()

    LaunchedEffect(cacheKey, retryKey) {
        // 已缓存则直接使用，避免重复加载
        if (comments != null && retryKey == 0) return@LaunchedEffect
        loading = true
        error = null
        try {
            val list = PixivApi.fetchComments(kind, workId)
            FeedCache.comments[cacheKey] = list
            comments = list
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load comments failed", e)
        }
        loading = false
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || posting) return
        scope.launch {
            posting = true
            try {
                PixivApi.addComment(kind, workId, text, replyTarget?.first ?: 0)
                val list = PixivApi.fetchComments(kind, workId)
                FeedCache.comments[cacheKey] = list
                comments = list
                draft = ""
                replyTarget = null
                refreshTick++
                feedback = sPosted
                feedbackError = false
            } catch (e: Exception) {
                feedback = sFailPrefix + (e.message ?: "")
                feedbackError = true
            }
            posting = false
        }
    }

    fun refresh() {
        if (refreshing) return
        scope.launch {
            refreshing = true
            try {
                val list = PixivApi.fetchComments(kind, workId)
                FeedCache.comments[cacheKey] = list
                comments = list
                error = null
            } catch (e: Exception) {
                error = e.friendlyMessage()
                logError("Pixo", "refresh comments failed", e)
            }
            refreshing = false
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.comments(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        loading && comments == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingState()
                        }
                        error != null && comments == null -> ErrorState(error ?: "", onRetry = { retryKey++ })
                        else -> PullToRefresh(
                            isRefreshing = refreshing,
                            onRefresh = { refresh() },
                            refreshTexts = refreshTexts(),
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                                overscrollEffect = null,
                            ) {
                                if (comments.orEmpty().isEmpty()) {
                                    item {
                                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(Str.no_comments(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                        }
                                    }
                                }
                                itemsIndexed(comments.orEmpty(), key = { _, c -> c.id }) { _, c ->
                                    RootCommentCard(
                                        c = c,
                                        kind = kind,
                                        workId = workId,
                                        onOpenUser = onOpenUser,
                                        onCopy = { t -> copyMenu = t },
                                        onReply = { t -> replyTarget = t.id to t.user },
                                        refreshTick = refreshTick,
                                        state = replyStatesMap[c.id] ?: ReplyUiState(),
                                        onStateChange = { s -> replyStatesMap = replyStatesMap + (c.id to s) },
                                    )
                                }
                            }
                        }
                    }
                }
                feedback?.let {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            it,
                            style = MiuixTheme.textStyles.footnote2,
                            color = if (feedbackError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
                        )
                    }
                }
                // 回复目标提示：点评论卡片下的「回复」后出现
                replyTarget?.let { target ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Str.reply_action() + " @" + target.second.ifBlank { Str.anonymous() },
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = "×",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { replyTarget = null }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = sHint,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { send() }, enabled = draft.isNotBlank() && !posting) {
                        Text(sSend)
                    }
                }
                OverlayDialog(show = copyMenu != null, onDismissRequest = { copyMenu = null }) {
                    val menuText = copyMenu.orEmpty()
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Str.copy_title(), style = MiuixTheme.textStyles.title3)
                        Button(onClick = {
                            clipboard.setText(AnnotatedString(menuText))
                            copyMenu = null
                        }, modifier = Modifier.fillMaxWidth()) { Text(Str.copy_all()) }
                        Button(onClick = {
                            copySelect = menuText
                            copyMenu = null
                        }, modifier = Modifier.fillMaxWidth()) { Text(Str.copy_select()) }
                    }
                }
                OverlayDialog(show = copySelect != null, onDismissRequest = { copySelect = null }) {
                    val text = copySelect.orEmpty()
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Str.copy_title(), style = MiuixTheme.textStyles.title3)
                        SelectionContainer {
                            Text(text, style = MiuixTheme.textStyles.body2)
                        }
                    }
                }
            }
        }
    }
}


/** 一级评论卡片：整卡长按复制文字；深色追评区内嵌卡片里，点「查看追评」展开、可对一级评论回复。 */

/** 追评 UI 状态：提升到评论区持有（LazyColumn 滚出视口会销毁 item，状态在外层才不丢失、不收起）。 */
private data class ReplyUiState(
    val expanded: Boolean = false,
    val replies: List<PixivComment> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val appNextUrl: String? = null,
    val webPage: Int = 0,
    val error: String? = null,
)


/** 表情章图片（s.pximg.net generated-stamps）。 */
/** Pixiv 评论括号表情 `(love2)` 等 → Unicode 表情字符渲染（pixiv 不公开表情图 URL）。 */
private object CommentEmoji {
    private val MAP = mapOf(
        "love" to "❤️", "love2" to "💗", "like" to "👍", "like2" to "❤️",
        "good" to "✨", "joke" to "😜", "surprise" to "😲", "sad" to "😢",
        "sad2" to "😞", "angry" to "😠", "angry2" to "😡", "cry" to "😭",
        "cry2" to "😭", "tear" to "🥺", "laugh" to "😄", "laugh2" to "😂",
        "smile" to "🙂", "smile2" to "🙃", "shock" to "😱", "wink" to "😉",
        "cool" to "😎", "happy" to "😆", "embarrassed" to "😳", "sleepy" to "😴",
        "heart" to "💛", "heart2" to "💖", "star" to "⭐", "sparkle" to "✨",
        "musical_note" to "🎵", "fire" to "🔥", "clap" to "👏", "ok" to "👌",
        "peace" to "✌️", "victory" to "✌️", "wave" to "👋", "bow" to "🙇",
        "please" to "🙏", "sorry" to "😣", "thank_you" to "🙏", "hug" to "🤗",
        "kiss" to "😘", "love_letter" to "💌", "blush" to "😊", "joyful" to "😄",
        "funny" to "😂", "pity" to "😥", "thinking" to "🤔", "idea" to "💡",
        "question" to "❓", "dizzy" to "😵", "sweat" to "😅",
    )
    private val pattern = Regex("\\(([a-zA-Z0-9_]+)\\)")

    /** 把 `(xxx)` 表情替换为对应 Unicode 表情；未命中的原样保留。 */
    fun render(text: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        var last = 0
        for (m in pattern.findAll(text)) {
            if (m.range.first > last) builder.append(text.substring(last, m.range.first))
            val name = m.groupValues[1]
            val emoji = MAP[name]
            if (emoji != null) builder.append(emoji) else builder.append(m.value)
            last = m.range.last + 1
        }
        if (last < text.length) builder.append(text.substring(last))
        return builder.toAnnotatedString()
    }
}


@Composable
private fun CommentStamp(stampId: String) {
    val ctx = LocalPlatformContext.current
    AsyncImage(
        model = remember(stampId) {
            ImageRequest.Builder(ctx)
                .data("https://s.pximg.net/common/images/stamp/generated-stamps/${stampId}_s.jpg")
                .crossfade(true)
                .build()
        },
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(64.dp),
    )
}


@Composable
private fun RootCommentCard(
    c: PixivComment,
    kind: String,
    workId: Long,
    onOpenUser: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onReply: (PixivComment) -> Unit,
    refreshTick: Int,
    state: ReplyUiState,
    onStateChange: (ReplyUiState) -> Unit,
) {
    val scope = rememberCoroutineScope()

    suspend fun loadPage() {
        val next = state.copy(
            loading = state.replies.isEmpty(),
            loadingMore = state.replies.isNotEmpty(),
            error = null,
        )
        onStateChange(next)
        try {
            val res = PixivApi.fetchCommentReplies(kind, workId, c.id, next.appNextUrl, next.webPage + 1)
            val seen = next.replies.mapTo(HashSet()) { it.id }
            onStateChange(
                next.copy(
                    replies = next.replies + res.replies.filter { it.id !in seen },
                    hasMore = res.hasMore,
                    appNextUrl = res.nextUrl,
                    webPage = next.webPage + 1,
                    loading = false,
                    loadingMore = false,
                ),
            )
        } catch (e: Exception) {
            onStateChange(next.copy(loading = false, loadingMore = false, error = e.friendlyMessage()))
        }
    }

    // 首次展开或发过新评论（refreshTick 变化）时拉取/刷新追评；
    // 状态已提升到评论区持有，滚动出屏再回来不丢失、不收起。
    LaunchedEffect(c.id, state.expanded, refreshTick) {
        if (state.expanded && (c.hasReplies || c.replies.isNotEmpty())) {
            if (refreshTick > 0 || state.replies.isEmpty()) {
                loadPage()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { onCopy(c.content) }),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AuthorAvatar(c.avatar, 36.dp, c.userId, onOpenUser)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = c.user.ifBlank { Str.anonymous() },
                            style = MiuixTheme.textStyles.body1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        c.date.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(it, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (c.content.isNotBlank()) {
                        Text(CommentEmoji.render(c.content), style = MiuixTheme.textStyles.body2)
                    } else if (c.stampId != null) {
                        CommentStamp(c.stampId)
                    }
                }
            }
            // 操作行：回复（对一级评论也能回复），追评卡片紧随其下
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = Str.reply_action(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReply(c) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            // 深色追评区：追评卡片嵌在一级评论卡片里面（回复按钮下方），用更深颜色区分
            if (c.hasReplies || c.replies.isNotEmpty() || state.expanded) {
                if (!state.expanded) {
                    // 收起态：深色条，点击展开
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .clickable { onStateChange(state.copy(expanded = true)) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = Str.view_replies(),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        when {
                            state.loading && state.replies.isEmpty() -> Box(
                                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(Str.load_more(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }

                            state.error != null && state.replies.isEmpty() -> Column(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(state.error ?: "", style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                Text(
                                    text = Str.retry(),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { scope.launch { loadPage() } }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }

                            state.replies.isEmpty() -> Box(
                                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(Str.no_replies(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }

                            else -> {
                                state.replies.forEach { r ->
                                    ReplyCard(c = r, onOpenUser = onOpenUser, onCopy = { t -> onCopy(t) }, onReply = { onReply(r) })
                                }
                                if (state.loadingMore) {
                                    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(Str.load_more(), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                    }
                                }
                            }
                        }
                        // 底部操作：加载更多 / 收起
                        Row(
                            Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (state.hasMore) {
                                Text(
                                    text = Str.load_more_replies(),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = !state.loadingMore) { scope.launch { loadPage() } }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            Text(
                                text = Str.collapse_replies(),
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onStateChange(state.copy(expanded = false)) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}


/** 追评卡片（用于一级评论卡片内的深色追评区）：头像 + 昵称 + 日期 + 内容 + 回复入口。 */
@Composable
private fun ReplyCard(
    c: PixivComment,
    onOpenUser: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onReply: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .combinedClickable(onClick = {}, onLongClick = { onCopy(c.content) })
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuthorAvatar(c.avatar, 24.dp, c.userId, onOpenUser)
            Spacer(Modifier.width(8.dp))
            Text(
                text = c.user.ifBlank { Str.anonymous() },
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            c.date.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.width(6.dp))
                Text(it, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        if (c.content.isNotBlank()) {
            Text(CommentEmoji.render(c.content), style = MiuixTheme.textStyles.footnote2)
        } else if (c.stampId != null) {
            CommentStamp(c.stampId)
        }
        // 卡片下点击回复，唤起回复输入框
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = Str.reply_action(),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onReply() }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
