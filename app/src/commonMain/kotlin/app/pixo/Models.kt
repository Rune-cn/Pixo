package app.pixo

import kotlinx.serialization.Serializable

/** 设置备份（数据页导出 / 导入用，含登录凭据，用于本机或可信设备迁移）。 */
@Serializable
data class SettingsBackup(
    val cookie: String = "",
    val refreshToken: String = "",
    val accessToken: String = "",
    val lastLoginUser: String = "",
    val selfUserId: Long = 0L,
    val debugLog: Boolean = false,
    val language: String = "system",
    val predictiveBack: Boolean = true,
    val dynamicColor: Boolean = true,
    val floatingBar: Boolean = true,
    val liquidGlass: Boolean = true,
    val downloadDir: String = "",
    val ugoiraConfirm: Boolean = true,
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
