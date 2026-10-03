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

/** 插画分页图。 */
data class PixivPage(val url: String)

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
