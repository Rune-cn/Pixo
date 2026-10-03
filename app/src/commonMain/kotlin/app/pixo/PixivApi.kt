package app.pixo

import okhttp3.OkHttpClient

/**
 * Pixiv API 门面：读/写各司其职。
 * - 读（排行/搜索/详情/分页/评论/收藏列表/用户）一律走网页 ajax（Cookie，稳定）。
 * - 写（收藏/取消收藏/发评论）在配置了 Token 时走 App API（Bearer、更可靠），否则退回网页。
 * - 作者作品无限流：Token 模式用 App API 分页；否则用 works/latest。
 */
object PixivApi {
    val client: OkHttpClient
        get() = PixivHttp.client

    fun refreshClient() {
        PixivHttp.refreshClient()
    }

    // ---------- 插画 / 漫画（读 → 网页） ----------

    suspend fun fetchRanking(mode: String, page: Int = 1): List<PixivWork> =
        WebPixivApi.fetchRanking(mode, page)

    suspend fun fetchTopFeed(offset: Int = 0, limit: Int = 30): List<PixivWork> =
        WebPixivApi.fetchTopFeed(offset, limit)

    suspend fun searchArtworks(word: String, page: Int = 1): List<PixivWork> =
        WebPixivApi.searchArtworks(word, page)

    suspend fun fetchIllustBookmarks(rest: String, offset: Int = 0, limit: Int = 24): List<PixivWork> =
        WebPixivApi.fetchIllustBookmarks(rest, offset, limit)

    // ---------- 小说（读 → 网页） ----------

    suspend fun searchNovels(word: String, page: Int = 1): List<PixivNovel> =
        WebPixivApi.searchNovels(word, page)

    suspend fun fetchNovelRanking(mode: String, page: Int = 1): List<PixivNovel> =
        WebPixivApi.fetchNovelRanking(mode, page)

    // ---------- 详情（读 → 网页） ----------

    suspend fun fetchIllustDetail(id: Long): PixivIllustDetail =
        WebPixivApi.fetchIllustDetail(id)

    suspend fun fetchIllustPages(id: Long): List<PixivPage> =
        WebPixivApi.fetchIllustPages(id)

    suspend fun fetchNovelDetail(id: Long): PixivNovelDetail =
        WebPixivApi.fetchNovelDetail(id)

    // ---------- 评论（读 → 网页；写 → Token/网页） ----------

    suspend fun fetchComments(kind: String, id: Long): List<PixivComment> =
        WebPixivApi.fetchComments(kind, id)

    suspend fun addComment(kind: String, id: Long, text: String) {
        if (PixivAuth.isTokenMode) AppPixivApi.addComment(kind, id, text) else WebPixivApi.addComment(kind, id, text)
    }

    // ---------- 收藏（写 → Token/网页） ----------

    suspend fun addBookmark(workId: Long, restrict: Int): String =
        if (PixivAuth.isTokenMode) AppPixivApi.addBookmark(workId, restrict) else WebPixivApi.addBookmark(workId, restrict)

    suspend fun addNovelBookmark(novelId: Long, restrict: Int): String =
        if (PixivAuth.isTokenMode) AppPixivApi.addNovelBookmark(novelId, restrict)
        else WebPixivApi.addNovelBookmark(novelId, restrict)

    suspend fun removeBookmark(kind: String, workId: Long, bookmarkId: Long) {
        if (PixivAuth.isTokenMode) AppPixivApi.removeBookmark(kind, workId, bookmarkId)
        else WebPixivApi.removeBookmark(kind, workId, bookmarkId)
    }

    // ---------- 用户（读 → 网页；作者无限流 → Token 分页） ----------

    suspend fun fetchUser(userId: Long): PixivUser =
        WebPixivApi.fetchUser(userId)

    suspend fun fetchUserWorks(userId: Long): PixivUserWorks =
        WebPixivApi.fetchUserWorks(userId)

    suspend fun fetchUserIllusts(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivWork> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUserIllusts(userId, offset, limit)
        else WebPixivApi.fetchUserIllusts(userId, offset, limit)

    suspend fun fetchUserNovels(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivNovel> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUserNovels(userId, offset, limit)
        else WebPixivApi.fetchUserNovels(userId, offset, limit)

    // ---------- 推荐（读 → 网页） ----------

    suspend fun fetchTopArtworks(): List<PixivWork> =
        WebPixivApi.fetchTopArtworks()
}