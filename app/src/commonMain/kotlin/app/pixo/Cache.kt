package app.pixo

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.key
import java.util.concurrent.ConcurrentHashMap

/**
 * 进程内缓存：列表分页结果、详情、评论、用户资料，以及滚动位置。
 *
 * 键约定：列表类用 `"<页面>:<参数>"`（如 `"home:feed"`、`"search:tag:猫"`），
 * 详情/评论按 id 索引。
 *
 * 并发：网络结果由 IO 协程写入、UI 协程读取，因此底层用 [ConcurrentHashMap]，
 * 避免普通 HashMap 并发读写导致的 `ConcurrentModificationException`。
 */
object FeedCache {
    val illust = ConcurrentHashMap<String, List<PixivWork>>()
    val novels = ConcurrentHashMap<String, List<PixivNovel>>()
    val users = ConcurrentHashMap<String, List<PixivUser>>()
    val indexes = ConcurrentHashMap<String, Int>()

    /** 详情与作品分页：按作品 id 缓存，重开不再请求。 */
    val illustDetail = ConcurrentHashMap<Long, PixivIllustDetail>()
    val novelDetail = ConcurrentHashMap<Long, PixivNovelDetail>()
    val illustPages = ConcurrentHashMap<Long, List<PixivPage>>()

    /** 评论：key = "$kind:$workId"。 */
    val comments = ConcurrentHashMap<String, List<PixivComment>>()

    /** 详情页相关作品：按作品 id 缓存，重进不再请求。 */
    val relatedWorks = ConcurrentHashMap<Long, List<PixivWork>>()

    /** 用户资料与主页作品：按用户 id 缓存。 */
    val userProfiles = ConcurrentHashMap<Long, PixivUser>()
    val userWorksCache = ConcurrentHashMap<Long, PixivUserWorks>()

    /**
     * 只清「列表类」缓存（列表、滚动位置、相关作品、用户主页）。
     *
     * 屏蔽规则与画质在解析列表时生效，改完设置必须清掉这些，
     * 否则已缓存的列表仍会显示被屏蔽内容 / 旧画质。
     * 详情与评论与这两项设置无关，保留以免返回时重新请求。
     */
    fun clearLists() {
        illust.clear()
        novels.clear()
        users.clear()
        indexes.clear()
        relatedWorks.clear()
        userWorksCache.clear()
        userProfiles.clear()
    }

    /** 清空全部缓存（设置页「清除缓存」）。 */
    fun clear() {
        clearLists()
        illustDetail.clear()
        novelDetail.clear()
        illustPages.clear()
        comments.clear()
        UgoiraCache.clear()
    }
}

/** 已解压的 ugoira 帧数据（帧字节 + 每帧停留毫秒）。 */
internal class UgoiraFrameData(val frames: List<ByteArray>, val delays: List<Int>)

/**
 * ugoira 帧缓存：按总字节数封顶的 LRU。
 *
 * 之前是无上限的 `mutableMapOf`，且「清除缓存」不会清它 ——
 * 连续看多张动图会把每张的全部帧字节常驻内存，低内存机型有 OOM 风险。
 */
internal object UgoiraCache {
    /** 上限按「帧数据总量」估算：单张动图约 1~8 MB。 */
    private const val MAX_BYTES = 48L * 1024 * 1024
    private const val MAX_ENTRIES = 8

    private val entries = object : LinkedHashMap<Long, UgoiraFrameData>(0, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, UgoiraFrameData>): Boolean =
            size > MAX_ENTRIES || totalBytes() > MAX_BYTES
    }

    private fun totalBytes(): Long = entries.values.sumOf { data -> data.frames.sumOf { it.size.toLong() } }

    @Synchronized
    fun get(id: Long): UgoiraFrameData? = entries[id]

    @Synchronized
    fun put(id: Long, data: UgoiraFrameData) {
        entries[id] = data
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
