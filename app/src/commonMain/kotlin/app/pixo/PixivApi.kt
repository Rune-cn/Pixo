package app.pixo

import okhttp3.OkHttpClient

/**
 * Pixiv API 门面：根据登录方式自动选择传输通道。
 * - 已配置 Refresh Token / Access Token → App API（Bearer，无 CSRF）。
 * - 仅 Cookie → 网页 ajax API。
 * 上层（Screens）只依赖本对象的公开方法。
 */
object PixivApi {
    val client: OkHttpClient
        get() = PixivHttp.client

    fun refreshClient() {
        PixivHttp.refreshClient()
    }

    // ---------- 插画 / 漫画 ----------

    suspend fun fetchRanking(mode: String, page: Int = 1): List<PixivWork> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchRanking(mode, page) else WebPixivApi.fetchRanking(mode, page)

    suspend fun fetchTopFeed(offset: Int = 0, limit: Int = 30): List<PixivWork> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchTopFeed(offset, limit) else WebPixivApi.fetchTopFeed(offset, limit)

    suspend fun searchArtworks(word: String, page: Int = 1): List<PixivWork> =
        if (PixivAuth.isTokenMode) AppPixivApi.searchArtworks(word, page) else WebPixivApi.searchArtworks(word, page)

    suspend fun fetchIllustBookmarks(rest: String, offset: Int = 0, limit: Int = 24): List<PixivWork> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchIllustBookmarks(rest, offset, limit)
        else WebPixivApi.fetchIllustBookmarks(rest, offset, limit)

    // ---------- 小说 ----------

    suspend fun searchNovels(word: String, page: Int = 1): List<PixivNovel> =
        if (PixivAuth.isTokenMode) AppPixivApi.searchNovels(word, page) else WebPixivApi.searchNovels(word, page)

    suspend fun fetchNovelRanking(mode: String, page: Int = 1): List<PixivNovel> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchNovelRanking(mode, page) else WebPixivApi.fetchNovelRanking(mode, page)

    // ---------- 详情 ----------

    suspend fun fetchIllustDetail(id: Long): PixivIllustDetail =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchIllustDetail(id) else WebPixivApi.fetchIllustDetail(id)

    suspend fun fetchIllustPages(id: Long): List<PixivPage> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchIllustPages(id) else WebPixivApi.fetchIllustPages(id)

    suspend fun fetchNovelDetail(id: Long): PixivNovelDetail =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchNovelDetail(id) else WebPixivApi.fetchNovelDetail(id)

    // ---------- 评论 ----------

    suspend fun fetchComments(kind: String, id: Long): List<PixivComment> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchComments(kind, id) else WebPixivApi.fetchComments(kind, id)

    suspend fun addComment(kind: String, id: Long, text: String) {
        if (PixivAuth.isTokenMode) AppPixivApi.addComment(kind, id, text) else WebPixivApi.addComment(kind, id, text)
    }

    // ---------- 收藏 ----------

    suspend fun addBookmark(workId: Long, restrict: Int): String =
        if (PixivAuth.isTokenMode) AppPixivApi.addBookmark(workId, restrict) else WebPixivApi.addBookmark(workId, restrict)

    suspend fun addNovelBookmark(novelId: Long, restrict: Int): String =
        if (PixivAuth.isTokenMode) AppPixivApi.addNovelBookmark(novelId, restrict)
        else WebPixivApi.addNovelBookmark(novelId, restrict)

    suspend fun removeBookmark(kind: String, workId: Long, bookmarkId: Long) {
        if (PixivAuth.isTokenMode) AppPixivApi.removeBookmark(kind, workId, bookmarkId)
        else WebPixivApi.removeBookmark(kind, workId, bookmarkId)
    }

    // ---------- 用户 ----------

    suspend fun fetchUser(userId: Long): PixivUser =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUser(userId) else WebPixivApi.fetchUser(userId)

    suspend fun fetchUserWorks(userId: Long): PixivUserWorks =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUserWorks(userId) else WebPixivApi.fetchUserWorks(userId)

    suspend fun fetchUserIllusts(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivWork> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUserIllusts(userId, offset, limit)
        else WebPixivApi.fetchUserIllusts(userId, offset, limit)

    suspend fun fetchUserNovels(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivNovel> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchUserNovels(userId, offset, limit)
        else WebPixivApi.fetchUserNovels(userId, offset, limit)

    suspend fun fetchTopArtworks(): List<PixivWork> =
        if (PixivAuth.isTokenMode) AppPixivApi.fetchTopArtworks() else WebPixivApi.fetchTopArtworks()
}
