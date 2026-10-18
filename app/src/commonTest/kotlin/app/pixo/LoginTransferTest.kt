package app.pixo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 登录信息解析单测。
 *
 * 导入要同时认两种格式：新版 JSON，以及旧版调试页的 `---- key ----` 分段文本
 * （用户可能已经把那段文本存下来了，不兼容就等于丢凭据）。
 */
class LoginTransferTest {

    @Test
    fun parsesJsonExport() {
        val text = """
            {
              "refreshToken": "rt-123",
              "accessToken": "at-456",
              "cookie": "PHPSESSID=1_abc",
              "selfUserId": 121772297,
              "lastLoginUser": "Xtne (@user_jerm2477)"
            }
        """.trimIndent()
        val backup = LoginTransfer.parse(text)!!
        assertEquals("rt-123", backup.refreshToken)
        assertEquals("at-456", backup.accessToken)
        assertEquals("PHPSESSID=1_abc", backup.cookie)
        assertEquals(121772297L, backup.selfUserId)
        assertEquals("Xtne (@user_jerm2477)", backup.lastLoginUser)
    }

    @Test
    fun parsesLegacyDebugText() {
        val text = """
            Pixo login info (debug)
            ---- refresh_token ----
            g2QWS-aP6pW8gX6V6HNDtG
            ---- access_token ----
            bHylboyHyqMHUtMosHm74kgz
            ---- cookie ----
            PHPSESSID=121772297_Uhla7; device_token=4f36
            ---- self_user_id ----
            121772297
            ---- last_login_user ----
            Xtne (@user_jerm2477)
        """.trimIndent()
        val backup = LoginTransfer.parse(text)!!
        assertEquals("g2QWS-aP6pW8gX6V6HNDtG", backup.refreshToken)
        assertEquals("bHylboyHyqMHUtMosHm74kgz", backup.accessToken)
        assertEquals("PHPSESSID=121772297_Uhla7; device_token=4f36", backup.cookie)
        assertEquals(121772297L, backup.selfUserId)
    }

    @Test
    fun treatsEmptyPlaceholderAsBlank() {
        val text = """
            ---- refresh_token ----
            (empty)
            ---- cookie ----
            PHPSESSID=1_abc
        """.trimIndent()
        val backup = LoginTransfer.parse(text)!!
        assertEquals("", backup.refreshToken)
        assertEquals("PHPSESSID=1_abc", backup.cookie)
    }

    @Test
    fun rejectsUnrecognizedContent() {
        assertNull(LoginTransfer.parse(""))
        assertNull(LoginTransfer.parse("hello world"))
    }
}
