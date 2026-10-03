package app.pixo

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.URLEncoder

/**
 * 网页 API（www.pixiv.net/ajax/...）传输通道。
 * 使用 Cookie（PHPSESSID）鉴权；写操作需要 x-csrf-token。
 */
object WebPixivApi {

    // ---------- 插画 / 漫画列表 ----------

    suspend fun fetchRanking(mode: String, page: Int = 1): List<PixivWork> {
        val root = PixivHttp.getJson(
            PixivHttp.webJsonHeaders()
                .url("https://www.pixiv.net/ranking.php?mode=$mode&content=illust&p=$page&format=json"),
        )
        return parseWorks(root.arr("contents")) { item ->
            PixivWork(
                id = item.lng("illust_id"),
                title = item.str("title"),
                author = item.str("user_name"),
                authorId = item.lng("user_id"),
                avatar = item.str("profile_img"),
                thumb = item.str("url"),
                illustType = item.int("illust_type"),
                pageCount = item.int("illust_page_count").coerceAtLeast(item.int("page_count")).coerceAtLeast(1),
            )
        }
    }

    suspend fun fetchTopFeed(offset: Int = 0, limit: Int = 30): List<PixivWork> {
        val root = PixivHttp.getJson(
            PixivHttp.webJsonHeaders()
                .url("https://www.pixiv.net/ajax/discovery/artworks?mode=all&limit=$limit&offset=$offset&lang=zh"),
        )
        if (root.str("error") == "true") {
            throw IOException("Pixiv 返回错误：${root.str("message")}")
        }
        val body = root.obj("body")
        val illust = body?.obj("thumbnails")?.arr("illust")
        if (illust == null || illust.isEmpty()) {
            throw IOException("推荐接口未返回作品数据，请确认已登录")
        }
        return parseWorks(illust) { item ->
            PixivWork(
                id = item.lng("id"),
                title = item.str("title"),
                author = item.str("userName"),
                authorId = item.lng("userId"),
                avatar = item.str("profileImageUrl"),
                thumb = item.str("url"),
                illustType = item.int("illustType"),
                pageCount = item.int("pageCount").coerceAtLeast(1),
            )
        }
    }

    suspend fun searchArtworks(word: String, page: Int = 1): List<PixivWork> {
        val encoded = URLEncoder.encode(word, "UTF-8")
        val url = "https://www.pixiv.net/ajax/search/artworks/$encoded" +
            "?word=$encoded&order=popular_d&mode=all&p=$page&s_mode=s_tag&type=all"
        val root = PixivHttp.getJson(PixivHttp.webJsonHeaders().url(url))
        val body = root.obj("body")
        val data = body?.obj("illustManga")?.arr("data") ?: body?.arr("data")
        return parseWorks(data) { item ->
            PixivWork(
                id = item.lng("id"),
                title = item.str("title"),
                author = item.str("userName"),
                authorId = item.lng("userId"),
                avatar = item.str("profileImageUrl"),
                thumb = item.str("url"),
                illustType = item.int("illustType"),
                pageCount = item.int("pageCount").coerceAtLeast(1),
            )
        }
    }

    suspend fun fetchIllustBookmarks(rest: String, offset: Int = 0, limit: Int = 24): List<PixivWork> {
        val uid = PixivHttp.selfUserId()
        if (uid <= 0L) throw IOException("无法获取登录用户，请重新设置 Cookie")
        val root = PixivHttp.getJson(
            PixivHttp.webJsonHeaders().url(
                "https://www.pixiv.net/ajax/user/$uid/illusts/bookmarks?tag=&rest=$rest&offset=$offset&limit=$limit&lang=zh",
            ),
        )
        if (root.str("error") == "true") {
            throw IOException("收藏获取失败：${root.str("message")}")
        }
        val works = root.obj("body")?.arr("works")
        return parseWorks(works) { item ->
            PixivWork(
                id = item.lng("id"),
                title = item.str("title"),
                author = item.str("userName"),
                authorId = item.lng("userId"),
                avatar = item.str("profileImageUrl"),
                thumb = item.str("url"),
                illustType = item.int("illustType"),
                pageCount = item.int("pageCount").coerceAtLeast(1),
            )
        }
    }

    // ---------- 小说列表 ----------

    suspend fun searchNovels(word: String, page: Int = 1): List<PixivNovel> {
        val encoded = URLEncoder.encode(word, "UTF-8")
        val url = "https://www.pixiv.net/ajax/search/novels/$encoded" +
            "?word=$encoded&order=popular_d&mode=all&p=$page&s_mode=s_tag"
        val root = PixivHttp.getJson(PixivHttp.webJsonHeaders().url(url))
        // 搜索结果在 body.novel.data（novel 是带 data/total/lastPage 的对象，不是数组）
        val data = root.obj("body")?.obj("novel")?.arr("data")
        return parseNovels(data)
    }

    suspend fun fetchNovelRanking(mode: String, page: Int = 1): List<PixivNovel> {
        val root = PixivHttp.getJson(
            PixivHttp.webJsonHeaders().url("https://www.pixiv.net/ajax/ranking/novel?mode=$mode&p=$page&lang=zh"),
        )
        val body = root.obj("body")
        val rank = body?.arr("rank") ?: body?.obj("display_a")?.arr("rank_a")
        return parseNovels(rank)
    }

    // ---------- 详情 ----------

    suspend fun fetchIllustDetail(id: Long): PixivIllustDetail {
        val root = PixivHttp.getJson(PixivHttp.webJsonHeaders().url("https://www.pixiv.net/ajax/illust/$id"))
        val body = root.obj("body") ?: throw IOException("响应缺少 body")
        val authorId = body.lng("userId")
        val tags = buildList {
            val arr = body.obj("tags")?.arr("tags")
            if (arr != null) {
                for (el in arr) {
                    val tag = (el as? JsonObject)?.str("tag")?.trim()
                    if (!tag.isNullOrEmpty()) add(tag)
                }
            }
        }
        var avatar = body.str("profileImageUrl")
        if (avatar.isBlank() && authorId > 0) {
            avatar = fetchUserAvatar(authorId)
        }
        val bm = fetchIllustBookmarkData(id)
        return PixivIllustDetail(
            title = body.str("title").ifBlank { "无标题" },
            author = body.str("userName"),
            authorId = authorId,
            avatar = avatar,
            tags = tags,
            illustType = body.int("illustType"),
            bookmarkCount = body.int("bookmarkCount"),
            likeCount = body.int("likeCount"),
            commentCount = body.int("commentCount"),
            isBookmarked = bm?.isBookmarked ?: false,
            bookmarkPrivate = bm?.isPrivate ?: false,
            bookmarkId = bm?.bookmarkId ?: 0,
        )
    }

    suspend fun fetchIllustPages(id: Long): List<PixivPage> {
        val root = PixivHttp.getJson(PixivHttp.webJsonHeaders().url("https://www.pixiv.net/ajax/illust/$id/pages"))
        val arr = root.arr("body")
        return buildList {
            if (arr == null) return@buildList
            for (el in arr) {
                val urls = (el as? JsonObject)?.obj("urls")
                val url = urls?.str("regular")?.takeIf { it.isNotBlank() }
                    ?: urls?.str("original") ?: ""
                add(PixivPage(url))
            }
        }
    }

    suspend fun fetchNovelDetail(id: Long): PixivNovelDetail {
        val root = PixivHttp.getJson(PixivHttp.webJsonHeaders().url("https://www.pixiv.net/ajax/novel/$id?lang=zh"))
        val body = root.obj("body") ?: throw IOException("响应缺少 body")
        val authorId = body.lng("userId")
        val tags = buildList {
            val arr = body.arr("tags") ?: body.obj("tags")?.arr("tags")
            if (arr != null) {
                for (el in arr) {
                    val tag = if (el is JsonObject) el.str("tag") else (el as? JsonPrimitive)?.content.orEmpty()
                    if (tag.isNotBlank()) add(tag)
                }
            }
        }
        var content = body.str("content")
        if (content.isBlank()) {
            content = try {
                val publish = body.obj("publish")
                val data = publish?.obj("data")
                val c = data?.arr("content")
                c?.joinToString("") { (it as? JsonPrimitive)?.content.orEmpty() }.orEmpty()
            } catch (_: Exception) {
                ""
            }
        }
        var avatar = body.str("profileImageUrl").ifBlank { body.str("profile_img") }
        if (avatar.isBlank() && authorId > 0) {
            avatar = fetchUserAvatar(authorId)
        }
        val bd = body.obj("bookmarkData")
        return PixivNovelDetail(
            title = body.str("title").ifBlank { "无标题" },
            author = body.str("userName").ifBlank { body.str("user_name") },
            authorId = authorId,
            avatar = avatar,
            tags = tags,
            content = content,
            isBookmarked = bd != null,
            bookmarkPrivate = bd?.str("private") == "true",
            bookmarkId = bd?.lng("id") ?: 0,
        )
    }

    // ---------- 评论 ----------

    suspend fun fetchComments(kind: String, id: Long): List<PixivComment> {
        val url = if (kind == "novel") {
            "https://www.pixiv.net/ajax/novels/comments/roots?novel_id=$id&offset=0&limit=30"
        } else {
            "https://www.pixiv.net/ajax/illusts/comments/roots?illust_id=$id&offset=0&limit=30"
        }
        val root = PixivHttp.getJson(PixivHttp.webJsonHeaders().url(url))
        val arr = root.obj("body")?.arr("comments")
        return buildList {
            if (arr == null) return@buildList
            for (el in arr) {
                try {
                    val o = el as JsonObject
                    val content = o.str("comment").ifBlank { o.str("userComment") }
                    if (content.isBlank()) continue
                    add(
                        PixivComment(
                            id = o.lng("id"),
                            user = o.str("userName").ifBlank { o.str("user_name") },
                            userId = o.lng("userId"),
                            avatar = o.str("img").ifBlank { o.str("profileImageUrl") },
                            content = content,
                            date = o.str("commentDate"),
                        ),
                    )
                } catch (_: Exception) {
                    // 跳过异常条目
                }
            }
        }
    }

    suspend fun addComment(kind: String, id: Long, text: String) {
        val csrf = fetchCsrfToken(id)
        // 评论接口需要作品作者的 user_id，缺失会返回 400/422
        val authorId = if (kind == "novel") fetchNovelDetail(id).authorId else fetchIllustDetail(id).authorId
        val builder = PixivHttp.webJsonHeaders()
            .header("x-csrf-token", csrf)
            .header(
                "Referer",
                if (kind == "novel") "https://www.pixiv.net/novel/show.php?id=$id" else "https://www.pixiv.net/artworks/$id",
            )
        val form = StringBuilder("type=comment")
        if (kind == "novel") {
            form.append("&novel_id=$id")
        } else {
            form.append("&illust_id=$id")
        }
        form.append("&author_user_id=$authorId&comment=${URLEncoder.encode(text, "UTF-8")}")
        PixivHttp.postJson(
            url = if (kind == "novel") "https://www.pixiv.net/novel/rpc/post_comment.php"
            else "https://www.pixiv.net/rpc/post_comment.php",
            payload = form.toString(),
            headers = builder,
            form = true,
        )
    }

    // ---------- 收藏 ----------

    suspend fun addBookmark(workId: Long, restrict: Int): String {
        val csrf = fetchCsrfToken(workId)
        val payload = """{"illust_id":"$workId","restrict":$restrict,"comment":"","tags":[]}"""
        val builder = PixivHttp.webJsonHeaders()
            .header("x-csrf-token", csrf)
            .header("Referer", "https://www.pixiv.net/artworks/$workId")
        val root = PixivHttp.postJson(
            url = "https://www.pixiv.net/ajax/illusts/bookmarks/add",
            payload = payload,
            headers = builder,
        )
        if (root.str("error") == "true") {
            throw IOException("收藏失败：${root.str("message")}")
        }
        return if (restrict == 0) "已公开收藏" else "已私人收藏"
    }

    suspend fun addNovelBookmark(novelId: Long, restrict: Int): String {
        val csrf = fetchCsrfToken(novelId)
        val payload = """{"novel_id":"$novelId","restrict":$restrict,"comment":"","tags":[]}"""
        val builder = PixivHttp.webJsonHeaders()
            .header("x-csrf-token", csrf)
            .header("Referer", "https://www.pixiv.net/novel/show.php?id=$novelId")
        val root = PixivHttp.postJson(
            url = "https://www.pixiv.net/ajax/novels/bookmarks/add",
            payload = payload,
            headers = builder,
        )
        if (root.str("error") == "true") {
            throw IOException("收藏失败：${root.str("message")}")
        }
        return if (restrict == 0) "已公开收藏" else "已私人收藏"
    }

    suspend fun removeBookmark(kind: String, workId: Long, bookmarkId: Long) {
        val csrf = fetchCsrfToken(workId)
        val builder = PixivHttp.webJsonHeaders()
            .header("x-csrf-token", csrf)
            .header("Referer", "https://www.pixiv.net/artworks/$workId")
        if (kind == "novel") {
            PixivHttp.postJson(
                url = "https://www.pixiv.net/ajax/novels/bookmarks/delete",
                payload = "book_id=$workId&del=1",
                headers = builder,
                form = true,
            )
        } else {
            val id = if (bookmarkId > 0) bookmarkId else workId
            PixivHttp.postJson(
                url = "https://www.pixiv.net/ajax/illusts/bookmarks/delete",
                payload = "bookmark_id=$id",
                headers = builder,
                form = true,
            )
        }
    }

    // ---------- 用户 ----------

    /**
     * 用户资料。
     *
     * `/ajax/user/{id}` 只返回名字与头像，作品数与简介要从 profile 接口补齐：
     * 作品数在 `/profile/all`，简介在 `/profile/top` 的 ogp.description。
     * 这两项缺失只会让作者页少显示统计与简介，所以补齐失败时按空值继续。
     */
    suspend fun fetchUser(userId: Long): PixivUser {
        val body = fetchUserBasic(userId)
        val counts = fetchUserCounts(userId)
        return PixivUser(
            id = body.lng("userId").takeIf { it > 0 } ?: userId,
            name = body.str("name"),
            account = body.str("account"),
            avatar = body.str("imageBig").ifBlank { body.str("image") },
            comment = fetchUserBio(userId),
            isFollowed = body.str("isFollowed") == "true",
            totalIllusts = counts.first,
            totalNovels = counts.second,
        )
    }

    /** 作品数：illusts 可能是 id 数组，也可能是 {id: ""} 映射，两种都要能数。 */
    private suspend fun fetchUserCounts(userId: Long): Pair<Int, Int> = try {
        val root = PixivHttp.getJson(
            PixivHttp.webJsonHeaders().url("https://www.pixiv.net/ajax/user/$userId/profile/all"),
        )
        val b = root.obj("body")
        fun countOf(key: String): Int = b?.arr(key)?.size ?: b?.obj(key)?.size ?: 0
        countOf("illusts") to countOf("novels")
    } catch (_: Exception) {
        0 to 0
    }

    /** 个人简介。 */
    private suspend fun fetchUserBio(userId: Long): String = try {
        val root = PixivHttp.getJson(
            PixivHttp.webJsonHeaders().url("https://www.pixiv.net/ajax/user/$userId/profile/top"),
        )
        root.obj("body")?.obj("extraData")?.obj("meta")?.obj("ogp")?.str("description").orEmpty()
    } catch (_: Exception) {
        ""
    }

    suspend fun fetchUserWorks(userId: Long): PixivUserWorks {
        val root = PixivHttp.getJson(
            PixivHttp.webJsonHeaders().url("https://www.pixiv.net/ajax/user/$userId/works/latest"),
        )
        val body = root.obj("body")
        val illusts = buildList {
            val map = body?.obj("illusts")
            if (map != null) {
                for ((_, v) in map) {
                    val o = v as? JsonObject ?: continue
                    try {
                        add(
                            PixivWork(
                                id = o.lng("id"),
                                title = o.str("title"),
                                author = o.str("userName"),
                                authorId = o.lng("userId"),
                                avatar = o.str("profileImageUrl"),
                                thumb = o.str("url"),
                                illustType = o.int("illustType"),
                                pageCount = o.int("pageCount").coerceAtLeast(1),
                            ),
                        )
                    } catch (_: Exception) {
                        // 跳过异常条目
                    }
                }
            }
        }
        val novels = buildList {
            val map = body?.obj("novels")
            if (map != null) {
                for ((_, v) in map) {
                    val o = v as? JsonObject ?: continue
                    try {
                        add(parseNovel(o))
                    } catch (_: Exception) {
                        // 跳过异常条目
                    }
                }
            }
        }
        return PixivUserWorks(illusts = illusts, novels = novels)
    }

    /** 作者插画分页（无限流用）：/ajax/user/{id}/illusts 返回 body.works。 */
    suspend fun fetchUserIllusts(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivWork> {
        val root = PixivHttp.getJson(
            PixivHttp.webJsonHeaders().url(
                "https://www.pixiv.net/ajax/user/$userId/illusts?offset=$offset&limit=$limit&type=illust&lang=zh",
            ),
        )
        return parseWorks(root.obj("body")?.arr("works")) { item ->
            PixivWork(
                id = item.lng("id"),
                title = item.str("title"),
                author = item.str("userName"),
                authorId = item.lng("userId"),
                avatar = item.str("profileImageUrl"),
                thumb = item.str("url"),
                illustType = item.int("illustType"),
                pageCount = item.int("pageCount").coerceAtLeast(1),
            )
        }
    }

    /** 作者小说分页（无限流用）：/ajax/user/{id}/novels 返回 body.works。 */
    suspend fun fetchUserNovels(userId: Long, offset: Int = 0, limit: Int = 30): List<PixivNovel> {
        val root = PixivHttp.getJson(
            PixivHttp.webJsonHeaders().url(
                "https://www.pixiv.net/ajax/user/$userId/novels?offset=$offset&limit=$limit&lang=zh",
            ),
        )
        return parseNovels(root.obj("body")?.arr("works"))
    }

    /** 推荐板块：/ajax/top/illust（thumbnails.illust 为 id→条目 映射）。 */
    suspend fun fetchTopArtworks(): List<PixivWork> {
        val root = PixivHttp.getJson(
            PixivHttp.webJsonHeaders().url("https://www.pixiv.net/ajax/top/illust?mode=all&lang=zh"),
        )
        val map = root.obj("body")?.obj("thumbnails")?.obj("illust") ?: return emptyList()
        return map.values.mapNotNull { v ->
            (v as? JsonObject)?.let { o ->
                PixivWork(
                    id = o.lng("id"),
                    title = o.str("title"),
                    author = o.str("userName"),
                    authorId = o.lng("userId"),
                    avatar = o.str("profileImageUrl"),
                    thumb = o.str("url"),
                    illustType = o.int("illustType"),
                    pageCount = o.int("pageCount").coerceAtLeast(1),
                )
            }
        }
    }

    /** 只取名字与头像的轻量请求：详情页只需要头像时走它，省掉两次 profile 请求。 */
    private suspend fun fetchUserBasic(userId: Long): JsonObject {
        val root = PixivHttp.getJson(PixivHttp.webJsonHeaders().url("https://www.pixiv.net/ajax/user/$userId"))
        return root.obj("body") ?: throw IOException("用户不存在或已删除")
    }

    suspend fun fetchUserAvatar(userId: Long): String {
        return try {
            val body = fetchUserBasic(userId)
            body.str("imageBig").ifBlank { body.str("image") }
        } catch (_: Exception) {
            ""
        }
    }

    // ---------- 工具 ----------

    private suspend fun fetchIllustBookmarkData(illustId: Long): PixivBookmarkState? {
        return try {
            val root = PixivHttp.getJson(
                PixivHttp.webJsonHeaders().url("https://www.pixiv.net/ajax/illust/$illustId/bookmarkData"),
            )
            val body = root.obj("body") ?: return null
            val bd = body.obj("bookmarkData")
            PixivBookmarkState(
                isBookmarked = bd != null,
                isPrivate = bd?.str("private") == "true",
                bookmarkId = bd?.lng("id") ?: 0,
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 抓取 CSRF token。
     *
     * 优先抓作品页，其次首页；每页按 `pixivAccount.tt` → `__NEXT_DATA__.api.token` → 通用 token 三个来源解析。
     * 写操作（收藏/评论）都会带上作品 id，确保 Referer 与 token 都取自作品页，最贴近官方 WEB 行为。
     */
    private suspend fun fetchCsrfToken(wordId: Long? = null): String {
        val urls = buildList {
            if (wordId != null) add("https://www.pixiv.net/artworks/$wordId")
            add("https://www.pixiv.net/")
        }
        for (url in urls) {
            val html = try {
                PixivHttp.getText(
                    PixivHttp.webJsonHeaders()
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .url(url),
                )
            } catch (_: Exception) {
                continue
            }
            // 1) pixivAccount.tt（官方主用来源，位于作品页/登录后页面）
            Regex("""["']pixivAccount\.tt["']\s*:\s*["']([^"']+)["']""").find(html)?.let {
                return it.groupValues[1].trim()
            }
            // 2) __NEXT_DATA__ 中 serverSerializedPreloadedState.api.token
            Regex(
                """<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""",
                RegexOption.DOT_MATCHES_ALL,
            ).find(html)?.let { m ->
                try {
                    val nd = Json.parseToJsonElement(m.groupValues[1]).jsonObject
                    val preloaded = nd["props"]?.jsonObject
                        ?.get("pageProps")?.jsonObject
                        ?.get("serverSerializedPreloadedState") as? JsonPrimitive
                    if (preloaded != null) {
                        val token = Json.parseToJsonElement(preloaded.content).jsonObject
                            .get("api")?.jsonObject?.get("token")?.jsonPrimitive?.content
                        if (!token.isNullOrBlank() && token.length >= 16) return token
                    }
                } catch (_: Exception) {
                    // 继续尝试下一来源
                }
            }
            // 3) 通用 csrf token
            Regex("""[Cc]srf[_\-]?token["':= ]+([a-zA-Z0-9_\-]{16,})""").find(html)?.let {
                return it.groupValues[1].trim()
            }
        }
        throw IOException("无法获取 CSRF token，请重新登录并刷新 Cookie")
    }

    private fun parseWorks(arr: JsonArray?, mapper: (JsonObject) -> PixivWork): List<PixivWork> = buildList {
        if (arr == null) return@buildList
        for (el in arr) {
            try {
                add(mapper(el as JsonObject))
            } catch (_: Exception) {
                // 跳过异常条目
            }
        }
    }

    private fun parseNovels(arr: JsonArray?): List<PixivNovel> = buildList {
        if (arr == null) return@buildList
        for (el in arr) {
            try {
                add(parseNovel(el as JsonObject))
            } catch (_: Exception) {
                // 跳过异常条目
            }
        }
    }

    private fun parseNovel(o: JsonObject): PixivNovel {
        val tags = buildList {
            val tagA = o.arr("tag_a")
            if (tagA != null) {
                for (t in tagA) {
                    val s = (t as? JsonPrimitive)?.content
                    if (!s.isNullOrBlank()) add(s)
                }
            } else {
                val tagsArr = o.arr("tags")
                if (tagsArr != null) {
                    for (t in tagsArr) {
                        when (t) {
                            is JsonPrimitive -> if (t.content.isNotBlank()) add(t.content)
                            is JsonObject -> t.str("tag").takeIf { it.isNotBlank() }?.let { add(it) }
                            else -> {}
                        }
                    }
                }
            }
        }
        return PixivNovel(
            id = o.lng("id").takeIf { it > 0 } ?: o.lng("novelId"),
            title = o.str("title"),
            author = o.str("userName").ifBlank { o.str("user_name") },
            authorId = o.lng("userId").takeIf { it > 0 } ?: o.lng("user_id"),
            avatar = o.str("profileImageUrl").ifBlank { o.str("profile_img") },
            cover = coverUrl(o),
            tags = tags,
            bookmarkCount = o.int("bookmarkCount").coerceAtLeast(o.int("bookmark_count")),
            textCount = o.int("textLength").coerceAtLeast(o.int("textCount"))
                .coerceAtLeast(o.int("character_count")).coerceAtLeast(o.int("word_count")),
        )
    }

    private fun coverUrl(o: JsonObject): String {
        val urls = o.obj("cover")?.obj("urls")
        if (urls != null) {
            return urls.str("240mw")
                .ifBlank { urls.str("480mw") }
                .ifBlank { urls.str("128x128") }
        }
        return o.str("url")
    }
}
