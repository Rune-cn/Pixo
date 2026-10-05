package app.pixo

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
)

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
)

/** 用户主页资料。 */
data class PixivUser(
    val id: Long,
    val name: String,
    val account: String,
    val avatar: String,
    val comment: String,
    val isFollowed: Boolean,
    val totalIllusts: Int,
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

/** 评论。 */
data class PixivComment(
    val id: Long,
    val user: String,
    val userId: Long,
    val avatar: String,
    val content: String,
    val date: String,
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
