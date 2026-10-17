package app.pixo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 设置备份（数据页导出 / 导入用，仅偏好设置，不含任何登录凭据）。 */
@Serializable
data class SettingsBackup(
    val debugLog: Boolean = false,
    val language: String = "system",
    val predictiveBack: Boolean = true,
    val dynamicColor: Boolean = true,
    val floatingBar: Boolean = true,
    val liquidGlass: Boolean = true,
    val downloadDir: String = "",
    val ugoiraConfirm: Boolean = true,
    val tagBlockPatterns: String = "",
    val disableSwipeBack: Boolean = false,
    val imageQuality: String = "standard",
    val tagBlockKeywords: String = "",
    val blockedAuthors: String = "",
    val webdavUrl: String = "",
    val webdavUser: String = "",
    val webdavPassword: String = "",
    val waterfall: Boolean = false,
)

/** 插画/漫画列表条目。 */
data class PixivWork(
    val id: Long,
    val title: String,
    val author: String,
    val authorId: Long,
    val avatar: String,
    val thumb: String,
    val illustType: Int,
    val pageCount: Int,
    /** 年龄分级：0=全年龄, 1=R-18, 2=R-18G；部分通道（如网页排行）缺失时由标签兜底。 */
    val xRestrict: Int = 0,
    val tags: List<String> = emptyList(),
) {
    val isR18: Boolean
        get() = xRestrict > 0 || tags.any { it == "R-18" || it == "R18G" }
}

/** 小说列表条目。 */
data class PixivNovel(
    val id: Long,
    val title: String,
    val author: String,
    val authorId: Long,
    val avatar: String,
    val cover: String,
    val tags: List<String>,
    val bookmarkCount: Int,
    val textCount: Int,
    /** 年龄分级：0=全年龄, 1=R-18。 */
    val xRestrict: Int = 0,
) {
    val isR18: Boolean
        get() = xRestrict > 0 || tags.any { it == "R-18" || it == "R18G" }
}

/** 用户主页资料。 */
data class PixivUser(
    val id: Long,
    val name: String,
    val account: String,
    val avatar: String,
    val comment: String,
    val isFollowed: Boolean,
    /** 是否 Pixiv 会员（premium）。 */
    val premium: Boolean = false,
    val totalIllusts: Int,
    /** 漫画数（illustType=1）；部分通道拿不到时按 0 处理。 */
    val totalManga: Int = 0,
    val totalNovels: Int,
    /** 关注数；未知时为 null。 */
    val followingCount: Int? = null,
    /** 粉丝数；未知时为 null。 */
    val followerCount: Int? = null,
)

/** 关注/粉丝列表分页结果。 */
data class PixivRelationPage(
    val users: List<PixivUser>,
    /** 总数；接口没给时为 null。 */
    val total: Int? = null,
    /** 下一页 offset（网页通道）。 */
    val nextOffset: Int? = null,
    /** 下一页完整 URL 游标（App API 通道）。 */
    val nextUrl: String? = null,
    val hasMore: Boolean = false,
)

/** 用户主页作品列表。 */
data class PixivUserWorks(
    val illusts: List<PixivWork>,
    val novels: List<PixivNovel>,
)

/** 插画详情。 */
data class PixivIllustDetail(
    val title: String,
    val author: String,
    val authorId: Long,
    val avatar: String,
    val tags: List<String>,
    /** 标签官方翻译（与 tags 下标对齐，无翻译为空串）。 */
    val tagTranslations: List<String> = emptyList(),
    val illustType: Int,
    val bookmarkCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val isBookmarked: Boolean = false,
    val bookmarkPrivate: Boolean = false,
    val bookmarkId: Long = 0,
)

/** 小说详情。 */
data class PixivNovelDetail(
    val title: String,
    val author: String,
    val authorId: Long,
    val avatar: String,
    val tags: List<String>,
    val content: String,
    val isBookmarked: Boolean = false,
    val bookmarkPrivate: Boolean = false,
    val bookmarkId: Long = 0,
)

/** 插画分页图。[url] 为列表用图（regular），[original] 为原图（灯箱/全屏用）。 */
data class PixivPage(val url: String, val original: String = "")

/** Pixiv ugoira（动图）元数据：zip 下载地址与帧序列。 */
data class PixivUgoira(
    val zipUrl: String,
    val frames: List<PixivUgoiraFrame>,
)

/** ugoira 单帧：[file] 为 zip 内的文件名，[delayMs] 为该帧停留毫秒数。 */
data class PixivUgoiraFrame(
    val file: String,
    val delayMs: Int,
)

/** 评论。 */
data class PixivComment(
    val id: Long,
    val user: String,
    val userId: Long,
    val avatar: String,
    val content: String,
    val date: String,
    /** 表情章 id（content 为空时存在；渲染为 s.pximg.net generated-stamps 图片）。 */
    val stampId: String? = null,
    /** 追评（一级评论卡片内已嵌入的前几条）。 */
    val replies: List<PixivComment> = emptyList(),
    /** 是否还有更多追评（需要单独拉取）。 */
    val hasReplies: Boolean = false,
)

/** 追评分页结果：replies + 分页游标（Token 通道用 [nextUrl]，网页通道用 [page]）。 */
data class PixivReplyPage(
    val replies: List<PixivComment>,
    val hasMore: Boolean = false,
    val nextUrl: String? = null,
)

/** 收藏状态。 */
data class PixivBookmarkState(
    val isBookmarked: Boolean,
    val isPrivate: Boolean,
    val bookmarkId: Long = 0,
)

/** 收藏分页结果：works + 下一页游标。App API 只认 `max_bookmark_id`（offset 恒被忽略，会一直返回第一页）。 */
data class PixivBookmarkPage(
    val works: List<PixivWork>,
    val nextMaxBookmarkId: Long? = null,
    val hasMore: Boolean = false,
)

/** 已下载到本地目录的图片（下载页展示用）。[uri] 为可直接交给图片加载器的字符串（Android content://，桌面 file://）。 */
data class DownloadedImage(
    val name: String,
    val uri: String,
    val size: Long,
)

/** 会话用户信息缓存：避免重复拉取（如反复进出设置页时头像不闪、不重新加载）。 */
object SessionCache {
    var selfInfo: PixivUser? = null
}

/** 标签屏蔽：`AppPrefs.tagBlockPatterns` 每行一条正则，命中作品任一标签即过滤。 */
object TagBlock {
    /** 正则规则（一行一条）。 */
    private fun regexRules(): List<String> = AppPrefs.tagBlockPatterns
        .lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

    /** 常规屏蔽词（一行一条，字面量忽略大小写匹配）。 */
    private fun keywordRules(): List<String> = AppPrefs.tagBlockKeywords
        .lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

    fun patternCount(): Int = regexRules().size

    fun keywordCount(): Int = keywordRules().size

    fun isBlocked(tags: List<String>): Boolean {
        if (tags.isEmpty()) return false
        val patterns = regexRules()
        val keywords = keywordRules()
        if (patterns.isEmpty() && keywords.isEmpty()) return false
        return tags.any { tag ->
            patterns.any { p ->
                runCatching { Regex(p, RegexOption.IGNORE_CASE).containsMatchIn(tag) }.getOrDefault(false)
            } || keywords.any { tag.contains(it, ignoreCase = true) }
        }
    }
}

/** 高清画质：把列表缩略图 URL（master1200）转换为原图级（img-original）。 */
fun upscaleThumb(url: String): String =
    url.replace("c/540x540_70/img-master/img/", "img-original/img/")
        .replace("_master1200.jpg", ".jpg")

/** 作者屏蔽：作者名/ID，一行一条（字面量匹配，不区分大小写）；列表解析层统一过滤。 */
object AuthorBlock {
    private fun rules(): List<String> = AppPrefs.blockedAuthors
        .lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

    fun patternCount(): Int = rules().size

    fun isBlocked(authorId: Long, author: String): Boolean {
        val rules = rules()
        if (rules.isEmpty()) return false
        val id = authorId.toString()
        return rules.any { r -> id == r || author.contains(r, ignoreCase = true) }
    }
}

/** 本地浏览历史条目（kind：illust / novel）。 */
@Serializable
data class HistoryItem(
    val id: Long,
    val kind: String,
    val title: String,
    val thumb: String,
    val time: Long,
)

/** 本地浏览历史：最近 200 条，JSON 持久化在 [AppPrefs.history]。 */
object HistoryStore {
    private const val MAX = 200
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): List<HistoryItem> = runCatching {
        json.decodeFromString<List<HistoryItem>>(AppPrefs.history)
    }.getOrDefault(emptyList())

    fun add(item: HistoryItem) {
        val list = load().filterNot { it.id == item.id && it.kind == item.kind }
        val next = (listOf(item) + list).take(MAX)
        AppPrefs.history = runCatching { json.encodeToString(next) }.getOrDefault("")
    }

    fun clear() {
        AppPrefs.history = ""
    }
}
