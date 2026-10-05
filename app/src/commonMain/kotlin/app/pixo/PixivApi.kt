package app.pixo

import okhttp3.OkHttpClient

/**
 * Pixiv API 门面：按逆向仓库（pixiv_client.py）统一走网页 ajax（Cookie + CSRF）。
 * 读接口无需 CSRF；写接口（收藏/评论）自动带 x-csrf-token。
 */
object PixivApi {
    val client: OkHttpClient
        get() = PixivHttp.client

    fun refreshClient() {
        PixivHttp.refreshClient()
    }

    // ---------- 插画 / 漫画 ----------

    suspend fun fetchRanking(mode: String, page: Int = 1): List<PixivWork> =
        WebPixivApi.fetchRanking(mode, page)

    suspend fun fetchTopFeed(offset: Int = 0, limit: Int = 30): List<PixivWork> =
        // 登录（Token）时走 App API 个性化推荐，未登录回退网页发现页
        if (PixivAuth.isTokenMode) {
            try {
                AppPixivApi.fetchTopFeed(offset, limit)
            } catch (_: Exception) {
                WebPixivApi.fetchTopFeed(offset, limit)
            }
        } else WebPixivApi.fetchTopFeed(offset, limit)

    suspend fun searchArtworks(word: String, page: Int = 1): List<PixivWork> =
        WebPixivApi.searchArtworks(word, page)

    suspend fun fetchIllustBookmarks(
        rest: String,
        offset: Int = 0,
        maxBookmarkId: Long? = null,
        limit: Int = 24,
    ): PixivBookmarkPage =
        // 收藏列表：Token 模式走 App API（max_bookmark_id 游标分页），Cookie 模式走网页 ajax（offset 分页）
        if (PixivAuth.isTokenMode) AppPixivApi.fetchIllustBookmarks(rest, offset, maxBookmarkId, limit)
        else {
            val works = WebPixivApi.fetchIllustBookmarks(rest, offset, limit)
            PixivBookmarkPage(works = works, hasMore = works.size >= limit)
        }

    // ---------- 小说 ----------

    suspend fun searchNovels(word: String, page: Int = 1): List<PixivNovel> =
        WebPixivApi.searchNovels(word, page)

    suspend fun fetchNovelRanking(mode: String, page: Int = 1): List<PixivNovel> =
        WebPixivApi.fetchNovelRanking(mode, page)

    // ---------- 详情 ----------

    suspend fun fetchIllustDetail(id: Long): PixivIllustDetail =
        WebPixivApi.fetchIllustDetail(id)

    suspend fun fetchIllustPages(id: Long): List<PixivPage> =
        WebPixivApi.fetchIllustPages(id)

    /** ugoira（动图）元数据：zip 下载地址 + 帧序列。 */
    suspend fun fetchUgoiraMeta(id: Long): PixivUgoira =
        WebPixivApi.fetchUgoiraMeta(id)

    suspend fun fetchNovelDetail(id: Long): PixivNovelDetail =
        WebPixivApi.fetchNovelDetail(id)

    // ---------- 评论 ----------

    suspend fun fetchComments(kind: String, id: Long): List<PixivComment> =
        WebPixivApi.fetchComments(kind, id)

    /** 追评列表：Token 模式走 App API `/v1/{kind}/comment/replies`（[appNextUrl] 游标），否则网页 ajax（[webPage] 从 1 开始）。 */
    suspend fun fetchCommentReplies(
        kind: String,
        id: Long,
        commentId: Long,
        appNextUrl: String? = null,
        webPage: Int = 1,
    ): PixivReplyPage =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchCommentReplies(kind, id, commentId, appNextUrl)
        else WebPixivApi.fetchCommentReplies(kind, id, commentId, webPage)

    /** 发表评论/追评：[parentCommentId] > 0 时为回复某个评论。 */
    suspend fun addComment(kind: String, id: Long, text: String, parentCommentId: Long = 0) =
        // 写评论：Token 模式走 App API（POST /v1/*/comment/add），否则网页 CSRF
        if (PixivAuth.isTokenMode) AppPixivApi.addComment(kind, id, text, parentCommentId)
        else WebPixivApi.addComment(kind, id, text, parentCommentId)

    // ---------- 收藏 ----------

    suspend fun addBookmark(workId: Long, restrict: Int): String =
        if (PixivAuth.isTokenMode) AppPixivApi.addBookmark(workId, restrict)
        else WebPixivApi.addBookmark(workId, restrict)

    suspend fun addNovelBookmark(novelId: Long, restrict: Int): String =
        if (PixivAuth.isTokenMode) AppPixivApi.addNovelBookmark(novelId, restrict)
        else WebPixivApi.addNovelBookmark(novelId, restrict)

    suspend fun removeBookmark(kind: String, workId: Long, bookmarkId: Long) =
        if (PixivAuth.isTokenMode) AppPixivApi.removeBookmark(kind, workId, bookmarkId)
        else WebPixivApi.removeBookmark(kind, workId, bookmarkId)

    // ---------- 用户 ----------

    suspend fun fetchUser(userId: Long): PixivUser =
        // 用户主页：Token 模式走 App API（/v1/user/detail，offset 分页稳定），
        // 网页模式回退 ajax（作者作品分页用 ids[] 批量，见 WebPixivApi）
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUser(userId)
        else WebPixivApi.fetchUser(userId)

    suspend fun fetchUserWorks(userId: Long): PixivUserWorks =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUserWorks(userId)
        else WebPixivApi.fetchUserWorks(userId)

    suspend fun fetchUserIllusts(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivWork> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUserIllusts(userId, offset, limit)
        else WebPixivApi.fetchUserIllusts(userId, offset, limit)

    suspend fun fetchUserNovels(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivNovel> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUserNovels(userId, offset, limit)
        else WebPixivApi.fetchUserNovels(userId, offset, limit)

    /** 关注列表：[nextUrl] 为 App API 游标；网页通道忽略游标、用 [offset] 分页。 */
    suspend fun fetchUserFollowing(userId: Long, offset: Int = 0, nextUrl: String? = null): PixivRelationPage =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUserFollowing(userId, offset, nextUrl)
        else {
            val page = WebPixivApi.fetchUserFollowing(userId, offset, nextUrl)
            if (page.users.isEmpty() && page.total == null) WebPixivApi.fetchUserFollowing(userId, offset, null)
            else page
        }

    /** 粉丝列表：[nextUrl] 为 App API 游标；网页通道忽略游标、用 [offset] 分页。 */
    suspend fun fetchUserFollowers(userId: Long, offset: Int = 0, nextUrl: String? = null): PixivRelationPage =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUserFollowers(userId, offset, nextUrl)
        else {
            val page = WebPixivApi.fetchUserFollowers(userId, offset, nextUrl)
            if (page.users.isEmpty() && page.total == null) WebPixivApi.fetchUserFollowers(userId, offset, null)
            else page
        }

    // ---------- 推荐 ----------

    suspend fun fetchTopArtworks(): List<PixivWork> =
        // 推荐板块：Token 模式用 App API /v1/illust/recommended（需登录），否则网页发现页
        if (PixivAuth.isTokenMode) {
            try {
                AppPixivApi.fetchTopArtworks()
            } catch (_: Exception) {
                WebPixivApi.fetchTopArtworks()
            }
        } else WebPixivApi.fetchTopArtworks()
}
