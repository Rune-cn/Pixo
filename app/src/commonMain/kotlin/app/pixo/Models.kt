package app.pixo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 设置备份（数据页导出 / 导入、WebDAV 云备份用）。
 *
 * 不含 Pixiv 登录凭据（cookie / token），但**包含 WebDAV 账号与密码**，
 * 导出的 JSON 与上传到 WebDAV 的文件都含明文密码 —— 分享备份文件前请注意。
 *
 * 新增设置项时只需在此加一个字段，并补上 [fromPrefs] / [applyToPrefs] 的一行，
 * 不再需要在 Platform.kt 里分别维护导出与导入两份清单。
 */
@Serializable
data class SettingsBackup(
    val debugLog: Boolean = false,
    val language: String = "system",
    val themeMode: String = "system",
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
) {
    companion object {
        fun fromPrefs(): SettingsBackup = SettingsBackup(
            debugLog = AppPrefs.debugLog,
            language = AppPrefs.language,
            themeMode = AppPrefs.themeMode,
            dynamicColor = AppPrefs.dynamicColor,
            floatingBar = AppPrefs.floatingBar,
            liquidGlass = AppPrefs.liquidGlass,
            downloadDir = AppPrefs.downloadDir,
            ugoiraConfirm = AppPrefs.ugoiraConfirm,
            tagBlockPatterns = AppPrefs.tagBlockPatterns,
            disableSwipeBack = AppPrefs.disableSwipeBack,
            imageQuality = AppPrefs.imageQuality,
            tagBlockKeywords = AppPrefs.tagBlockKeywords,
            blockedAuthors = AppPrefs.blockedAuthors,
            webdavUrl = AppPrefs.webdavUrl,
            webdavUser = AppPrefs.webdavUser,
            webdavPassword = AppPrefs.webdavPassword,
            waterfall = AppPrefs.waterfall,
        )
    }

    fun applyToPrefs() {
        AppPrefs.debugLog = debugLog
        AppPrefs.language = language
        AppPrefs.themeMode = themeMode
        AppPrefs.dynamicColor = dynamicColor
        AppPrefs.floatingBar = floatingBar
        AppPrefs.liquidGlass = liquidGlass
        AppPrefs.downloadDir = downloadDir
        AppPrefs.ugoiraConfirm = ugoiraConfirm
        AppPrefs.tagBlockPatterns = tagBlockPatterns
        AppPrefs.disableSwipeBack = disableSwipeBack
        AppPrefs.imageQuality = imageQuality
        AppPrefs.tagBlockKeywords = tagBlockKeywords
        AppPrefs.blockedAuthors = blockedAuthors
        AppPrefs.webdavUrl = webdavUrl
        AppPrefs.webdavUser = webdavUser
        AppPrefs.webdavPassword = webdavPassword
        AppPrefs.waterfall = waterfall
    }
}

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
/**
 * 屏蔽匹配的纯逻辑（不读偏好、无副作用），便于单元测试。
 *
 * 规则解析（读偏好、缓存编译结果）留在 [TagBlock] / [AuthorBlock]；
 * 这里只负责「给定规则，这条作品是否命中」。
 */
internal object BlockMatcher {
    /** 任一标签命中正则或字面量关键词即视为屏蔽。 */
    fun matchesTag(tags: List<String>, patterns: List<Regex>, keywords: List<String>): Boolean {
        if (tags.isEmpty()) return false
        if (patterns.isEmpty() && keywords.isEmpty()) return false
        return tags.any { tag ->
            patterns.any { it.containsMatchIn(tag) } ||
                keywords.any { tag.contains(it, ignoreCase = true) }
        }
    }

    /** 作者 ID 完全相等，或作者名包含规则串（忽略大小写）即视为屏蔽。 */
    fun matchesAuthor(authorId: Long, author: String, rules: List<String>): Boolean {
        if (rules.isEmpty()) return false
        val id = authorId.toString()
        return rules.any { rule -> id == rule || author.contains(rule, ignoreCase = true) }
    }
}

object TagBlock {
    /**
     * 编译结果缓存：按「原始规则文本」作为版本号。
     *
     * 列表解析时每个作品、每条标签都会调用 [isBlocked]，
     * 之前每次都重新 `Regex(...)` 编译一遍，一页 30 个作品就是上千次编译。
     */
    private class CompiledRules {
        @Volatile private var raw: String? = null
        @Volatile private var compiled: List<Regex> = emptyList()

        fun of(patterns: String): List<Regex> {
            if (patterns == raw) return compiled
            val next = patterns.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                // 非法正则直接丢弃（与旧行为一致：单条规则写错不影响其它规则）
                .mapNotNull { runCatching { Regex(it, RegexOption.IGNORE_CASE) }.getOrNull() }
                .toList()
            raw = patterns
            compiled = next
            return next
        }
    }

    private val regexCache = CompiledRules()

    /** 常规屏蔽词（一行一条，字面量忽略大小写匹配）。 */
    private fun keywordRules(): List<String> = AppPrefs.tagBlockKeywords
        .lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

    fun patternCount(): Int = regexRules().size

    fun keywordCount(): Int = keywordRules().size

    private fun regexRules(): List<Regex> = regexCache.of(AppPrefs.tagBlockPatterns)

    fun isBlocked(tags: List<String>): Boolean =
        BlockMatcher.matchesTag(tags, regexRules(), keywordRules())
}

/**
 * 高清画质：把列表缩略图 URL 提升到 1200 宽的大图。
 *
 * 缩略图尺寸段有 `c/540x540_70/`、`c/360x360_70/`（App API 的 square_medium）、
 * `c/250x250_80_a2/` 等多种前缀，只替换单一前缀会让「高清」在部分通道静默失效。
 * 这里统一改写尺寸段，并保留原扩展名（原图可能是 png/gif，硬编码 .jpg 会 404）。
 */
private val THUMB_SIZE_SEGMENT = Regex("/c/\\d+x\\d+(_[0-9a-zA-Z]+)*/")

fun upscaleThumb(url: String): String {
    if (url.isBlank()) return url
    // 只替换尺寸段，保留后面的 /img-master/ 与原文件名
    return THUMB_SIZE_SEGMENT.replace(url, "/c/1200x1200/")
}

/** 人（作者）屏蔽：作者名/ID，一行一条（字面量匹配，不区分大小写）。 */
object AuthorBlock {
    @Volatile private var cachedRaw: String? = null
    @Volatile private var cachedRules: List<String> = emptyList()

    private fun rules(): List<String> {
        val raw = AppPrefs.blockedAuthors
        if (raw == cachedRaw) return cachedRules
        val next = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        cachedRaw = raw
        cachedRules = next
        return next
    }

    fun patternCount(): Int = rules().size

    fun isBlocked(authorId: Long, author: String): Boolean =
        BlockMatcher.matchesAuthor(authorId, author, rules())
}

/**
 * 列表统一策略：屏蔽过滤 + 画质提升。
 *
 * 插画与小说共用同一份实现 —— 之前小说列表完全没走屏蔽规则（只给 `parseWorks` 加了），
 * 这类「只改了一边」的漂移在统一入口后不会再发生。
 */
internal fun <T> List<T>.filterBlocked(
    tags: (T) -> List<String>,
    authorId: (T) -> Long,
    author: (T) -> String,
): List<T> = filterNot { TagBlock.isBlocked(tags(it)) || AuthorBlock.isBlocked(authorId(it), author(it)) }

internal fun List<PixivWork>.applyListPolicy(
    filter: Boolean = true,
    upscale: Boolean = isHighQuality,
): List<PixivWork> {
    val base = if (filter) filterBlocked({ it.tags }, { it.authorId }, { it.author }) else this
    return if (upscale) base.map { it.copy(thumb = upscaleThumb(it.thumb)) } else base
}

internal fun List<PixivNovel>.applyListPolicy(): List<PixivNovel> =
    filterBlocked({ it.tags }, { it.authorId }, { it.author })

/** 当前画质是否为「高清」（列表解析时决定是否重写缩略图 URL）。 */
internal val isHighQuality: Boolean
    get() = AppPrefs.imageQuality == "high"

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
