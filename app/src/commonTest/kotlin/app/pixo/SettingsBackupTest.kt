package app.pixo

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 设置备份的序列化契约单测。
 *
 * 备份要跨版本兼容：老备份（没有 themeMode 字段）必须仍能导入，
 * 新增字段必须有默认值，否则「导入设置」会直接失败。
 */
class SettingsBackupTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun roundTripKeepsAllFields() {
        val backup = SettingsBackup(
            debugLog = true,
            language = "zh",
            themeMode = "Dark",
            dynamicColor = false,
            floatingBar = false,
            liquidGlass = false,
            downloadDir = "content://tree/x",
            ugoiraConfirm = false,
            tagBlockPatterns = "^R-18\n(?i)guro",
            disableSwipeBack = true,
            imageQuality = "high",
            tagBlockKeywords = "AI生成",
            blockedAuthors = "123\n某画师",
            webdavUrl = "https://dav.example.com",
            webdavUser = "user",
            webdavPassword = "pass",
            waterfall = true,
        )

        val decoded = json.decodeFromString<SettingsBackup>(json.encodeToString(backup))
        assertEquals(backup, decoded)
    }

    @Test
    fun oldBackupWithoutNewFieldsStillDecodes() {
        // 1.0.x 的备份没有 themeMode 字段
        val legacy = """{"language":"zh","imageQuality":"high","waterfall":true}"""
        val decoded = json.decodeFromString<SettingsBackup>(legacy)

        assertEquals("zh", decoded.language)
        assertEquals("high", decoded.imageQuality)
        assertTrue(decoded.waterfall)
        assertEquals("system", decoded.themeMode, "缺失字段应回退默认值，而不是导入失败")
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val future = """{"language":"en","someFutureFlag":true,"themeMode":"Light"}"""
        val decoded = json.decodeFromString<SettingsBackup>(future)
        assertEquals("en", decoded.language)
        assertEquals("Light", decoded.themeMode)
    }
}
