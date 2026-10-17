package app.pixo

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 「高清画质」URL 改写单测。
 *
 * 旧实现只替换 `c/540x540_70/img-master/img/` 一种前缀，
 * 而 App API 的缩略图是 `c/360x360_70/..._square1200.jpg` —— 高清开关在 Token 通道下等于没开。
 */
class UpscaleThumbTest {

    @Test
    fun upgradesWebThumbnail() {
        assertEquals(
            "https://i.pximg.net/c/1200x1200/img-master/img/2024/01/01/00/00/00/123_p0_master1200.jpg",
            upscaleThumb("https://i.pximg.net/c/540x540_70/img-master/img/2024/01/01/00/00/00/123_p0_master1200.jpg"),
        )
    }

    @Test
    fun upgradesAppApiSquareThumbnail() {
        assertEquals(
            "https://i.pximg.net/c/1200x1200/img-master/img/2024/01/01/00/00/00/123_p0_square1200.jpg",
            upscaleThumb("https://i.pximg.net/c/360x360_70/img-master/img/2024/01/01/00/00/00/123_p0_square1200.jpg"),
        )
    }

    @Test
    fun keepsOriginalExtension() {
        // png 原图必须保留扩展名，否则拼出的 .jpg 会 404
        assertEquals(
            "https://i.pximg.net/c/1200x1200/img-master/img/2024/01/01/00/00/00/123_p0_square1200.png",
            upscaleThumb("https://i.pximg.net/c/360x360_70/img-master/img/2024/01/01/00/00/00/123_p0_square1200.png"),
        )
    }

    @Test
    fun upgradesAlternatePrefixes() {
        assertEquals(
            "https://i.pximg.net/c/1200x1200/img-master/img/2024/01/01/00/00/00/123_p0_master1200.jpg",
            upscaleThumb("https://i.pximg.net/c/250x250_80_a2/img-master/img/2024/01/01/00/00/00/123_p0_master1200.jpg"),
        )
    }

    @Test
    fun alreadyLargeUrlIsIdempotent() {
        val once = upscaleThumb("https://i.pximg.net/c/360x360_70/img-master/img/a_p0_square1200.jpg")
        assertEquals(once, upscaleThumb(once))
    }

    @Test
    fun blankAndForeignUrlsAreUntouched() {
        assertEquals("", upscaleThumb(""))
        // 非 pixiv 缩略图（例如表情章）不做改写
        assertEquals(
            "https://s.pximg.net/common/images/stamp/generated-stamps/1_s.jpg",
            upscaleThumb("https://s.pximg.net/common/images/stamp/generated-stamps/1_s.jpg"),
        )
    }
}
