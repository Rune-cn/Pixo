package app.pixo

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 屏蔽匹配逻辑单测。
 *
 * 这些规则直接影响「用户能不能屏蔽掉不想看的内容」，
 * 之前完全没有测试，正则/关键词哪条失效只能靠用户反馈发现。
 */
class BlockMatcherTest {

    @Test
    fun emptyRulesNeverBlock() {
        assertFalse(BlockMatcher.matchesTag(listOf("猫", "R-18"), emptyList(), emptyList()))
        assertFalse(BlockMatcher.matchesAuthor(1L, "画师", emptyList()))
    }

    @Test
    fun regexMatchesAnyTag() {
        val patterns = listOf(Regex("r-?18", RegexOption.IGNORE_CASE))
        assertTrue(BlockMatcher.matchesTag(listOf("風景", "R-18"), patterns, emptyList()))
        assertTrue(BlockMatcher.matchesTag(listOf("r18"), patterns, emptyList()))
        assertFalse(BlockMatcher.matchesTag(listOf("風景", "日常"), patterns, emptyList()))
    }

    @Test
    fun keywordMatchesIgnoringCase() {
        assertTrue(BlockMatcher.matchesTag(listOf("AI生成"), emptyList(), listOf("ai生成")))
        assertTrue(BlockMatcher.matchesTag(listOf("Fate/GrandOrder"), emptyList(), listOf("fate")))
        assertFalse(BlockMatcher.matchesTag(listOf("原神"), emptyList(), listOf("fate")))
    }

    @Test
    fun regexAndKeywordAreBothApplied() {
        val patterns = listOf(Regex("^R-18"))
        val keywords = listOf("guro")
        assertTrue(BlockMatcher.matchesTag(listOf("R-18G"), patterns, keywords))
        assertTrue(BlockMatcher.matchesTag(listOf("Guro"), patterns, keywords))
        assertFalse(BlockMatcher.matchesTag(listOf("R-15"), patterns, keywords))
    }

    @Test
    fun authorBlockedByIdOrName() {
        val rules = listOf("123", "某画师")
        assertTrue(BlockMatcher.matchesAuthor(123L, "别人", rules))
        assertTrue(BlockMatcher.matchesAuthor(9L, "某画师的作品", rules))
        // 名字匹配忽略大小写
        assertTrue(BlockMatcher.matchesAuthor(9L, "SOME ARTIST", listOf("some artist")))
        assertFalse(BlockMatcher.matchesAuthor(9L, "正常画师", rules))
    }

    @Test
    fun authorIdMustMatchExactly() {
        // ID 必须完全相等：规则 "1" 不应把 ID 12 也屏蔽掉
        assertFalse(BlockMatcher.matchesAuthor(12L, "画师", listOf("1")))
    }
}
