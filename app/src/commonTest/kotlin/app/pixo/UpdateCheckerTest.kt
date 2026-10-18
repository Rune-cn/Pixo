package app.pixo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 版本比较单测。
 *
 * 「检查更新」靠它判断是否真的有新版本：比错了要么天天提示、要么永远不提示。
 */
class UpdateCheckerTest {

    @Test
    fun stripsVersionPrefix() {
        assertEquals("1.1.1", UpdateChecker.normalize("v1.1.1"))
        assertEquals("1.1.1", UpdateChecker.normalize("  V1.1.1 "))
    }

    @Test
    fun detectsNewerPatch() {
        assertTrue(UpdateChecker.isNewer("1.1.1", "1.1.0"))
        assertTrue(UpdateChecker.isNewer("v1.2.0", "1.1.9"))
    }

    @Test
    fun sameVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("1.1.1", "1.1.1"))
        assertFalse(UpdateChecker.isNewer("v1.1.1", "1.1.1"))
    }

    @Test
    fun olderVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("1.0.9", "1.1.0"))
        assertFalse(UpdateChecker.isNewer("1.1.0", "1.1.1"))
    }

    @Test
    fun missingSegmentsCountAsZero() {
        assertFalse(UpdateChecker.isNewer("1.2", "1.2.0"))
        assertTrue(UpdateChecker.isNewer("1.2.1", "1.2"))
    }

    @Test
    fun numericComparisonNotLexicographic() {
        // 字符串比较会把 "1.10.0" 判成小于 "1.9.0"
        assertTrue(UpdateChecker.isNewer("1.10.0", "1.9.0"))
        assertTrue(UpdateChecker.isNewer("1.1.10", "1.1.9"))
    }
}
