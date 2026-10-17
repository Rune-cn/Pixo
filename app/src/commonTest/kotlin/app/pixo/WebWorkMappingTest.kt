package app.pixo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * 网页通道作品映射单测。
 *
 * 同一份 [PixivWork] 要兼容两套字段名（ajax 的 camelCase 与排行榜的 snake_case），
 * 之前 7 个端点各写一份映射，字段兜底逻辑并不一致。
 */
class WebWorkMappingTest {

    private fun json(text: String) = Json.parseToJsonElement(text).jsonObject

    @Test
    fun mapsAjaxShape() = with(WebPixivApi) {
        val work = json(
            """
            {
              "id": "123",
              "title": "标题",
              "userName": "画师",
              "userId": "9",
              "profileImageUrl": "https://i.pximg.net/avatar.jpg",
              "url": "https://i.pximg.net/c/360x360_70/img-master/img/a_p0_square1200.jpg",
              "illustType": 0,
              "pageCount": 4,
              "xRestrict": 0,
              "tags": ["猫", "風景"]
            }
            """.trimIndent(),
        ).toWebWork()

        assertEquals(123L, work.id)
        assertEquals("标题", work.title)
        assertEquals("画师", work.author)
        assertEquals(9L, work.authorId)
        assertEquals("https://i.pximg.net/avatar.jpg", work.avatar)
        assertEquals(0, work.illustType)
        assertEquals(4, work.pageCount)
        assertEquals(0, work.xRestrict)
        assertEquals(listOf("猫", "風景"), work.tags)
    }

    @Test
    fun mapsRankingShape() = with(WebPixivApi) {
        // 排行榜 JSON 里数字多为字符串，字段名是 snake_case
        val work = json(
            """
            {
              "illust_id": "456",
              "title": "排行作品",
              "user_name": "排行画师",
              "user_id": "77",
              "profile_img": "https://i.pximg.net/p.jpg",
              "url": "https://i.pximg.net/c/540x540_70/img-master/img/b_p0_master1200.jpg",
              "illust_type": "1",
              "illust_page_count": "6",
              "page_count": "2",
              "x_restrict": "1",
              "tags": ["R-18", "漫画"]
            }
            """.trimIndent(),
        ).toWebWork()

        assertEquals(456L, work.id)
        assertEquals("排行画师", work.author)
        assertEquals(77L, work.authorId)
        assertEquals(1, work.illustType)
        // 取两个分页字段的较大值
        assertEquals(6, work.pageCount)
        assertEquals(1, work.xRestrict)
        assertEquals(true, work.isR18)
    }

    @Test
    fun pageCountNeverBelowOne() = with(WebPixivApi) {
        val work = json("""{"id":"1","pageCount":0}""").toWebWork()
        assertEquals(1, work.pageCount)
    }

    @Test
    fun mangaRankingForcesIllustType() = with(WebPixivApi) {
        val work = json("""{"illust_id":"8","illust_type":"0"}""").toWebWork(minIllustType = 1)
        assertEquals(1, work.illustType)
    }

    @Test
    fun tagsAcceptObjectsAndStrings() = with(WebPixivApi) {
        val work = json(
            """{"id":"1","tags":[{"tag":"对象标签"},"字符串标签"]}""",
        ).toWebWork()
        assertEquals(listOf("对象标签", "字符串标签"), work.tags)
    }
}
