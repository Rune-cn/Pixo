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
        WebPixivApi.fetchTopFeed(offset, limit)

    suspend fun searchArtworks(word: String, page: Int = 1): List<PixivWork> =
        WebPixivApi.searchArtworks(word, page)

    suspend fun fetchIllustBookmarks(rest: String, offset: Int = 0, limit: Int = 24): List<PixivWork> =
        WebPixivApi.fetchIllustBookmarks(rest, offset, limit)

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

    suspend fun fetchNovelDetail(id: Long): PixivNovelDetail =
        WebPixivApi.fetchNovelDetail(id)

    // ---------- 评论 ----------

    suspend fun fetchComments(kind: String, id: Long): List<PixivComment> =
        WebPixivApi.fetchComments(kind, id)

    suspend fun addComment(kind: String, id: Long, text: String) =
        WebPixivApi.addComment(kind, id, text)

    // ---------- 收藏 ----------

    suspend fun addBookmark(workId: Long, restrict: Int): String =
        WebPixivApi.addBookmark(workId, restrict)

    suspend fun addNovelBookmark(novelId: Long, restrict: Int): String =
        WebPixivApi.addNovelBookmark(novelId, restrict)

    suspend fun removeBookmark(kind: String, workId: Long, bookmarkId: Long) =
        WebPixivApi.removeBookmark(kind, workId, bookmarkId)

    // ---------- 用户 ----------

    suspend fun fetchUser(userId: Long): PixivUser =
        WebPixivApi.fetchUser(userId)

    suspend fun fetchUserWorks(userId: Long): PixivUserWorks =
        WebPixivApi.fetchUserWorks(userId)

    suspend fun fetchUserIllusts(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivWork> =
        WebPixivApi.fetchUserIllusts(userId, offset, limit)

    suspend fun fetchUserNovels(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivNovel> =
        WebPixivApi.fetchUserNovels(userId, offset, limit)

    // ---------- 推荐 ----------

    suspend fun fetchTopArtworks(): List<PixivWork> =
        WebPixivApi.fetchTopArtworks()
}