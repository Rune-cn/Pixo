package app.pixo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.URLEncoder

/**
 * App API（app-api.pixiv.net）传输通道。
 * 使用 OAuth Access Token（Authorization: Bearer）鉴权，写操作无需 CSRF。
 */
object AppPixivApi {
    private const val BASE = "https://app-api.pixiv.net/v1"
    private const val BASE_V2 = "https://app-api.pixiv.net/v2"

    /**
     * App API 写操作响应校验：业务失败时返回 HTTP 200 + `{"error":{...}}`，
     * 只查状态码会把失败误判为成功。
     */
    private fun JsonObject.throwIfApiError(prefix: String) {
        val err = this["error"] ?: return
        if (err is JsonObject) {
            val msg = err.str("user_message").ifBlank { err.str("message") }.ifBlank { err.str("reason") }
            throw IOException(prefix + msg)
        }
        if (err is JsonPrimitive && err.content == "true") throw IOException(prefix)
    }

    // ---------- 插画 / 漫画列表 ----------

    suspend fun fetchRanking(mode: String, page: Int = 1): List<PixivWork> {
        val offset = (page - 1) * 50
        val root = PixivHttp.getJson(
            PixivHttp.appApiHeaders().url("$BASE/illust/ranking?mode=$mode&offset=$offset&limit=50&filter=for_android"),
        )
        return parseIllusts(root.arr("illusts"))
    }

    suspend fun fetchTopFeed(offset: Int = 0, limit: Int = 30): List<PixivWork> {
        val root = PixivHttp.getJson(
            PixivHttp.appApiHeaders().url("$BASE/illust/recommended?offset=$offset&limit=$limit&filter=for_android"),
        )
        return parseIllusts(root.arr("illusts"))
    }

    suspend fun searchArtworks(word: String, page: Int = 1): List<PixivWork> {
        val encoded = URLEncoder.encode(word, "UTF-8")
        val offset = (page - 1) * 60
        val url = "$BASE/search/illust?word=$encoded&search_target=partial_match_for_tags&sort=popular_desc&offset=$offset"
        val root = PixivHttp.getJson(PixivHttp.appApiHeaders().url(url))
        return parseIllusts(root.arr("illusts"))
    }

    suspend fun fetchIllustBookmarks(rest: String, offset: Int = 0, limit: Int = 24): List<PixivWork> {
        val restrict = if (rest == "hide") "private" else "public"
        val root = PixivHttp.getJson(
            PixivHttp.appApiHeaders().url("$BASE/user/bookmarks/illust?user_id=${AppPrefs.selfUserId}&restrict=$restrict&offset=$offset&limit=$limit&filter=for_android"),
        )
        return parseIllusts(root.arr("illusts"))
    }

    // ---------- 小说列表 ----------

    suspend fun searchNovels(word: String, page: Int = 1): List<PixivNovel> {
        val encoded = URLEncoder.encode(word, "UTF-8")
        val offset = (page - 1) * 60
        val url = "$BASE/search/novel?word=$encoded&search_target=partial_match_for_tags&sort=popular_desc&offset=$offset"
        val root = PixivHttp.getJson(PixivHttp.appApiHeaders().url(url))
        return parseNovels(root.arr("novels"))
    }

    suspend fun fetchNovelRanking(mode: String, page: Int = 1): List<PixivNovel> {
        val offset = (page - 1) * 50
        val root = PixivHttp.getJson(
            PixivHttp.appApiHeaders().url("$BASE/novel/ranking?mode=$mode&offset=$offset&limit=50&filter=for_android"),
        )
        return parseNovels(root.arr("novels"))
    }

    // ---------- 详情 ----------

    suspend fun fetchIllustDetail(id: Long): PixivIllustDetail {
        val root = PixivHttp.getJson(PixivHttp.appApiHeaders().url("$BASE/illust/detail?illust_id=$id"))
        val illust = root.obj("illust") ?: throw IOException("作品不存在")
        val user = root.obj("user")
        val bd = illust.obj("bookmark_data")
        val tags = buildList {
            val arr = root.obj("tags")?.arr("tags")
            if (arr != null) for (el in arr) {
                val tag = (el as? JsonObject)?.str("name")?.trim()
                if (!tag.isNullOrEmpty()) add(tag)
            }
        }
        return PixivIllustDetail(
            title = illust.str("title").ifBlank { "无标题" },
            author = user?.str("name").orEmpty().ifBlank { illust.str("userName") },
            authorId = user?.lng("id")?.takeIf { it > 0 } ?: illust.lng("userId"),
            avatar = user?.obj("profile_image_urls")?.str("medium").orEmpty(),
            tags = tags,
            illustType = illust.int("illust_type"),
            bookmarkCount = illust.int("bookmark_count"),
            likeCount = illust.int("like_count"),
            commentCount = illust.int("comment_count"),
            isBookmarked = illust.str("is_bookmarked") == "true",
            bookmarkPrivate = bd?.str("private") == "true",
            bookmarkId = bd?.lng("id") ?: 0,
        )
    }

    suspend fun fetchIllustPages(id: Long): List<PixivPage> {
        val root = PixivHttp.getJson(PixivHttp.appApiHeaders().url("$BASE/illust/pages?illust_id=$id"))
        val arr = root.arr("pages")
        return buildList {
            if (arr == null) return@buildList
            for (el in arr) {
                val urls = (el as? JsonObject)?.obj("image_urls")
                val url = urls?.str("large")?.takeIf { it.isNotBlank() }
                    ?: urls?.str("original") ?: ""
                add(PixivPage(url))
            }
        }
    }

    suspend fun fetchNovelDetail(id: Long): PixivNovelDetail {
        val root = PixivHttp.getJson(PixivHttp.appApiHeaders().url("$BASE/novel/detail?novel_id=$id"))
        val novel = root.obj("novel") ?: throw IOException("小说不存在")
        val user = root.obj("user")
        val bd = novel.obj("bookmark_data")
        val tags = buildList {
            val arr = root.obj("tags")?.arr("tags")
            if (arr != null) for (el in arr) {
                val tag = (el as? JsonObject)?.str("name")?.trim()
                if (!tag.isNullOrEmpty()) add(tag)
            }
        }
        val content = fetchNovelText(id).ifBlank { novel.str("caption") }
        return PixivNovelDetail(
            title = novel.str("title").ifBlank { "无标题" },
            author = user?.str("name").orEmpty().ifBlank { novel.str("userName") },
            authorId = user?.lng("id")?.takeIf { it > 0 } ?: novel.lng("userId"),
            avatar = user?.obj("profile_image_urls")?.str("medium").orEmpty(),
            tags = tags,
            content = content,
            isBookmarked = novel.str("is_bookmarked") == "true",
            bookmarkPrivate = bd?.str("private") == "true",
            bookmarkId = bd?.lng("id") ?: 0,
        )
    }

    private suspend fun fetchNovelText(id: Long): String {
        return try {
            val root = PixivHttp.getJson(PixivHttp.appApiHeaders().url("$BASE/novel/text?novel_id=$id"))
            root.str("novel_text")
        } catch (_: Exception) {
            ""
        }
    }

    // ---------- 评论 ----------

    suspend fun fetchComments(kind: String, id: Long): List<PixivComment> {
        // App API 评论端点是单数 comment
        val url = if (kind == "novel") "$BASE/novel/comment?novel_id=$id&offset=0"
        else "$BASE/illust/comment?illust_id=$id&offset=0"
        val root = PixivHttp.getJson(PixivHttp.appApiHeaders().url(url))
        val arr = root.arr("comments")
        return buildList {
            if (arr == null) return@buildList
            for (el in arr) {
                try {
                    val o = el as JsonObject
                    val commenter = o.obj("user")
                    val content = o.str("comment")
                    if (content.isBlank()) continue
                    add(
                        PixivComment(
                            id = o.lng("id"),
                            user = commenter?.str("name").orEmpty().ifBlank { o.str("userName") },
                            userId = commenter?.lng("id") ?: 0L,
                            avatar = commenter?.obj("profile_image_urls")?.str("medium").orEmpty(),
                            content = content,
                            date = o.str("comment_date"),
                        ),
                    )
                } catch (_: Exception) {
                    // 跳过异常条目
                }
            }
        }
    }

    suspend fun addComment(kind: String, id: Long, text: String) {
        val url = if (kind == "novel") "$BASE/novel/comment/add" else "$BASE/illust/comment/add"
        val idField = if (kind == "novel") "novel_id" else "illust_id"
        val payload = "$idField=$id&comment=${URLEncoder.encode(text, "UTF-8")}"
        val root = PixivHttp.postJson(
            url = url,
            payload = payload,
            headers = PixivHttp.appApiHeaders(),
            form = true,
        )
        root.throwIfApiError("评论失败：")
    }

    // ---------- 收藏 ----------

    suspend fun addBookmark(workId: Long, restrict: Int): String {
        val restrictName = if (restrict == 0) "public" else "private"
        val payload = "illust_id=$workId&restrict=$restrictName"
        val root = PixivHttp.postJson(
            url = "$BASE_V2/illust/bookmark/add",
            payload = payload,
            headers = PixivHttp.appApiHeaders(),
            form = true,
        )
        root.throwIfApiError("收藏失败：")
        return if (restrict == 0) "已公开收藏" else "已私人收藏"
    }

    suspend fun addNovelBookmark(novelId: Long, restrict: Int): String {
        val restrictName = if (restrict == 0) "public" else "private"
        val payload = "novel_id=$novelId&restrict=$restrictName"
        val root = PixivHttp.postJson(
            url = "$BASE_V2/novel/bookmark/add",
            payload = payload,
            headers = PixivHttp.appApiHeaders(),
            form = true,
        )
        root.throwIfApiError("收藏失败：")
        return if (restrict == 0) "已公开收藏" else "已私人收藏"
    }

    suspend fun removeBookmark(kind: String, workId: Long, bookmarkId: Long) {
        val url = if (kind == "novel") "$BASE/novel/bookmark/delete" else "$BASE/illust/bookmark/delete"
        val idField = if (kind == "novel") "novel_id" else "illust_id"
        val root = PixivHttp.postJson(
            url = url,
            payload = "$idField=$workId",
            headers = PixivHttp.appApiHeaders(),
            form = true,
        )
        root.throwIfApiError("取消收藏失败：")
    }

    // ---------- 用户 ----------

    suspend fun fetchUser(userId: Long): PixivUser {
        val root = PixivHttp.getJson(PixivHttp.appApiHeaders().url("$BASE/user/detail?user_id=$userId"))
        val user = root.obj("user") ?: throw IOException("用户不存在")
        val profile = root.obj("profile")
        return PixivUser(
            id = user.lng("id").takeIf { it > 0 } ?: userId,
            name = user.str("name"),
            account = user.str("account").ifBlank { user.str("id") },
            avatar = user.obj("profile_image_urls")?.str("medium").orEmpty(),
            comment = user.str("comment"),
            isFollowed = user.str("is_followed") == "true",
            totalIllusts = profile?.int("total_illusts") ?: 0,
            totalNovels = profile?.int("total_novels") ?: 0,
        )
    }

    suspend fun fetchUserWorks(userId: Long): PixivUserWorks {
        val illusts = try {
            val root = PixivHttp.getJson(
                PixivHttp.appApiHeaders().url("$BASE/user/illusts?user_id=$userId&type=illust&offset=0&limit=30"),
            )
            parseIllusts(root.arr("illusts"))
        } catch (_: Exception) {
            emptyList()
        }
        val novels = try {
            val root = PixivHttp.getJson(
                PixivHttp.appApiHeaders().url("$BASE/user/novels?user_id=$userId&offset=0&limit=30"),
            )
            parseNovels(root.arr("novels"))
        } catch (_: Exception) {
            emptyList()
        }
        return PixivUserWorks(illusts = illusts, novels = novels)
    }

    /** 作者插画分页（无限流用）。 */
    suspend fun fetchUserIllusts(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivWork> {
        val root = PixivHttp.getJson(
            PixivHttp.appApiHeaders().url("$BASE/user/illusts?user_id=$userId&type=illust&offset=$offset&limit=$limit"),
        )
        return parseIllusts(root.arr("illusts"))
    }

    /** 作者小说分页（无限流用）。 */
    suspend fun fetchUserNovels(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivNovel> {
        val root = PixivHttp.getJson(
            PixivHttp.appApiHeaders().url("$BASE/user/novels?user_id=$userId&offset=$offset&limit=$limit"),
        )
        return parseNovels(root.arr("novels"))
    }

    /** 推荐板块（App API）。 */
    suspend fun fetchTopArtworks(): List<PixivWork> {
        val root = PixivHttp.getJson(
            PixivHttp.appApiHeaders().url("$BASE/illust/recommended?offset=0"),
        )
        return parseIllusts(root.arr("illusts"))
    }

    // ---------- 解析 ----------

    private fun parseIllusts(arr: JsonArray?): List<PixivWork> = buildList {
        if (arr == null) return@buildList
        for (el in arr) {
            try {
                val o = el as JsonObject
                val user = o.obj("user")
                add(
                    PixivWork(
                        id = o.lng("id"),
                        title = o.str("title"),
                        author = user?.str("name").orEmpty().ifBlank { o.str("userName") },
                        authorId = user?.lng("id")?.takeIf { it > 0 } ?: o.lng("userId"),
                        avatar = user?.obj("profile_image_urls")?.str("medium").orEmpty(),
                        thumb = o.obj("image_urls")?.str("square_medium")
                            ?.takeIf { it.isNotBlank() } ?: o.obj("image_urls")?.str("medium").orEmpty(),
                        illustType = o.int("illust_type"),
                        pageCount = o.int("page_count").coerceAtLeast(1),
                    ),
                )
            } catch (_: Exception) {
                // 跳过异常条目
            }
        }
    }

    private fun parseNovels(arr: JsonArray?): List<PixivNovel> = buildList {
        if (arr == null) return@buildList
        for (el in arr) {
            try {
                val o = el as JsonObject
                val user = o.obj("user")
                val urls = o.obj("image_urls")
                val tags = buildList {
                    val arr2 = o.arr("tags")
                    if (arr2 != null) for (t in arr2) {
                        when (t) {
                            is JsonPrimitive -> if (t.content.isNotBlank()) add(t.content)
                            is JsonObject -> t.str("name").takeIf { it.isNotBlank() }?.let { add(it) }
                            else -> {}
                        }
                    }
                }
                add(
                    PixivNovel(
                        id = o.lng("id"),
                        title = o.str("title"),
                        author = user?.str("name").orEmpty().ifBlank { o.str("userName") },
                        authorId = user?.lng("id")?.takeIf { it > 0 } ?: o.lng("userId"),
                        avatar = user?.obj("profile_image_urls")?.str("medium").orEmpty(),
                        cover = urls?.str("medium")?.takeIf { it.isNotBlank() }
                            ?: urls?.str("square_medium").orEmpty(),
                        tags = tags,
                        bookmarkCount = o.int("total_bookmarks").coerceAtLeast(o.int("bookmark_count")),
                        textCount = o.int("text_length").coerceAtLeast(o.int("word_count")),
                    ),
                )
            } catch (_: Exception) {
                // 跳过异常条目
            }
        }
    }
}
