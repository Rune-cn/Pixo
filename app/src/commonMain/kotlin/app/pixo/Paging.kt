package app.pixo

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 一页数据。
 *
 * [hasMore] 为 null 时按「本页条数 >= pageSize」推断；
 * 像收藏这种接口自带游标/是否还有下一页的，直接传明确值。
 */
data class Page<T>(val items: List<T>, val hasMore: Boolean? = null)

/**
 * 分页列表状态机：缓存恢复、首屏加载、下拉刷新、触底加载、去重追加、滚动位置记忆。
 *
 * 这些逻辑此前在 8 个列表页里各写一遍（每个页面 60~80 行，字段名、去重方式、
 * 失败处理都靠复制粘贴保持一致）。现在只有这一份实现，页面只提供
 * 「缓存键 + 缓存表 + 每页怎么取」三件事。
 */
@Stable
class PagedFeed<T>(
    private val cacheKey: String,
    private val restore: (String) -> List<T>?,
    private val persist: (String, List<T>) -> Unit,
    private val pageSize: Int,
    private val scope: CoroutineScope,
    private val idOf: (T) -> Long,
    private val load: suspend (page: Int) -> Page<T>,
) {
    /** 当前列表；null 表示首屏还没拿到数据（用于展示加载态）。 */
    var items: List<T>? by mutableStateOf(restore(cacheKey))
        private set

    /** 由 [rememberPagedFeed] 注入：带滚动位置记忆的列表状态。 */
    lateinit var listState: LazyListState
        internal set

    var loading: Boolean by mutableStateOf(items == null)
        private set

    var loadingMore: Boolean by mutableStateOf(false)
        private set

    var refreshing: Boolean by mutableStateOf(false)
        private set

    var hasMore: Boolean by mutableStateOf(true)
        private set

    var error: String? by mutableStateOf(null)
        private set

    /** 已加载到第几页（1 起）。 */
    var page: Int by mutableIntStateOf(1)
        private set

    private var firstLoadDone: Boolean = items != null
    private var retryToken: Int by mutableIntStateOf(0)

    /** 首屏加载（命中缓存则跳过）。 */
    suspend fun loadFirst() {
        if (firstLoadDone) return
        firstLoadDone = true
        loading = true
        error = null
        try {
            applyFirstPage(load(1))
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "load $cacheKey failed", e)
            // 失败时清掉「已加载」标记：外部条件变化（如登录）后可再次尝试
            firstLoadDone = false
        }
        loading = false
    }

    /** 下拉刷新：重新取第 1 页并替换列表。 */
    suspend fun refresh() {
        refreshing = true
        try {
            applyFirstPage(load(1))
            error = null
        } catch (e: Exception) {
            error = e.friendlyMessage()
            logError("Pixo", "refresh $cacheKey failed", e)
        }
        refreshing = false
    }

    /** 触底加载下一页；失败保持原列表，下次滚动再试。 */
    suspend fun loadMore() {
        if (loading || loadingMore || !hasMore) return
        loadingMore = true
        val next = page + 1
        try {
            val result = load(next)
            val merged = items.orEmpty().toMutableList()
            val seen = HashSet(merged.map(idOf))
            merged.addAll(result.items.filter { idOf(it) !in seen })
            persist(cacheKey, merged)
            items = merged
            page = next
            hasMore = result.hasMore ?: (result.items.size >= pageSize)
        } catch (e: Exception) {
            logError("Pixo", "load more $cacheKey failed", e)
        }
        loadingMore = false
    }

    /** 非挂起版本，供按钮/下拉回调使用。 */
    fun refreshNow() {
        scope.launch { refresh() }
    }

    fun loadMoreNow() {
        scope.launch { loadMore() }
    }

    /** 条件不满足（例如搜索页还没输入关键词）时进入空闲态，不显示「加载中」。 */
    fun markIdle() {
        loading = false
    }

    /** 首屏失败后的重试：清掉失败标记并重新触发 [loadFirst]。 */
    fun retry() {
        firstLoadDone = false
        error = null
        retryToken++
    }

    /** 供 Composable 观察的重试令牌（作为 LaunchedEffect 的 key）。 */
    internal val retrySignal: Int get() = retryToken

    private fun applyFirstPage(result: Page<T>) {
        persist(cacheKey, result.items)
        items = result.items
        page = 1
        hasMore = result.hasMore ?: (result.items.size >= pageSize)
    }
}

/**
 * 建立分页列表状态。
 *
 * @param cacheKey 缓存键（同时决定滚动位置记忆的键；切换查询条件时变化）
 * @param cache 对应的缓存表，例如 [FeedCache.illust]
 * @param enabled 为 false 时不发起首屏请求（例如搜索页还没输入关键词）
 * @param idOf 去重用的稳定 id
 * @param load 取第 page 页（1 起）
 */
@Composable
fun <T> rememberPagedFeed(
    cacheKey: String,
    cache: ConcurrentHashMap<String, List<T>>,
    idOf: (T) -> Long,
    pageSize: Int = 30,
    enabled: Boolean = true,
    reloadKey: Any? = null,
    load: suspend (page: Int) -> Page<T>,
): PagedFeed<T> = rememberPagedFeedByKey(
    cacheKey = cacheKey,
    idOf = idOf,
    pageSize = pageSize,
    enabled = enabled,
    reloadKey = reloadKey,
    restore = { cache[it] },
    persist = { key, list -> cache[key] = list },
    load = load,
)

/**
 * 同上，但缓存读写由调用方提供（例如搜索页按类型分散在多个缓存表里）。
 *
 * @param restore 恢复上次的列表（没有则返回 null）
 * @param persist 保存最新列表
 */
@Composable
fun <T> rememberPagedFeedByKey(
    cacheKey: String,
    idOf: (T) -> Long,
    pageSize: Int = 30,
    enabled: Boolean = true,
    reloadKey: Any? = null,
    restore: (String) -> List<T>?,
    persist: (String, List<T>) -> Unit,
    load: suspend (page: Int) -> Page<T>,
): PagedFeed<T> {
    // 屏蔽规则/画质变更后重建状态，列表随之重新拉取
    val revision = SettingsRevision.current
    val scope = rememberCoroutineScope()
    val feed = remember(cacheKey, revision) {
        PagedFeed(cacheKey, restore, persist, pageSize, scope, idOf, load)
    }
    val listState: LazyListState = key(cacheKey, revision) {
        rememberLazyListState(initialFirstVisibleItemIndex = FeedCache.indexes[cacheKey] ?: 0)
    }
    feed.listState = listState

    // enabled 控制「该不该请求」，reloadKey 用于登录态等外部条件变化时重新拉取
    LaunchedEffect(feed, enabled, reloadKey, feed.retrySignal) {
        if (enabled) feed.loadFirst() else feed.markIdle()
    }

    // 触底加载：最后可见项接近末尾（剩余 <= 4）时取下一页
    LaunchedEffect(listState, feed.items?.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && last >= total - 4
        }.collect { nearEnd -> if (nearEnd) feed.loadMore() }
    }

    SideEffect { FeedCache.indexes[cacheKey] = listState.firstVisibleItemIndex }

    return feed
}
