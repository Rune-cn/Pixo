package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

/** 内容设置页：作品浏览相关偏好（动图播放需确认等）。 */
@Composable
fun ContentPage(
    ugoiraConfirm: Boolean,
    onUgoiraConfirmChange: (Boolean) -> Unit,
    waterfall: Boolean,
    onWaterfallChange: (Boolean) -> Unit,
    imageQuality: String,
    onImageQualityChange: (String) -> Unit,
    disableSwipeBack: Boolean,
    onDisableSwipeBackChange: (Boolean) -> Unit,
    onOpenTagBlock: () -> Unit,
    onOpenAuthorBlock: () -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.settings_content(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item { SectionTitle(Str.settings_section_artwork()) }
            item {
                SettingsGroup {
                    SwitchPreference(
                        title = Str.ugoira_confirm(),
                        summary = Str.ugoira_confirm_summary(),
                        checked = ugoiraConfirm,
                        onCheckedChange = { onUgoiraConfirmChange(it) },
                    )
                    SwitchPreference(
                        title = Str.waterfall(),
                        summary = Str.waterfall_summary(),
                        checked = waterfall,
                        onCheckedChange = { onWaterfallChange(it) },
                    )
                    PixoDropdownPreference(
                        title = Str.image_quality(),
                        summary = if (imageQuality == "high") Str.quality_high() else Str.quality_standard(),
                        options = listOf(Str.quality_standard(), Str.quality_high()),
                        selectedIndex = if (imageQuality == "high") 1 else 0,
                        onSelectedIndexChange = { onImageQualityChange(if (it == 1) "high" else "standard") },
                    )
                    SwitchPreference(
                        title = Str.disable_swipe_back(),
                        summary = Str.disable_swipe_back_summary(),
                        checked = disableSwipeBack,
                        onCheckedChange = { onDisableSwipeBackChange(it) },
                    )
                }
            }
            item { SectionTitle(Str.settings_section_block()) }
            item {
                SettingsGroup {
                    val blockCount = TagBlock.patternCount()
                    val keywordCount = TagBlock.keywordCount()
                    ArrowPreference(
                        title = Str.tag_block(),
                        summary = if (blockCount + keywordCount > 0)
                            Str.tag_block_count(blockCount + keywordCount) else Str.tag_block_summary(),
                        onClick = onOpenTagBlock,
                    )
                    val authorCount = AuthorBlock.patternCount()
                    ArrowPreference(
                        title = Str.author_block(),
                        summary = if (authorCount > 0) Str.tag_block_count(authorCount) else Str.author_block_hint(),
                        onClick = onOpenAuthorBlock,
                    )
                }
            }
        }
    }
}


/** 标签屏蔽规则编辑页：每行一条正则，保存进 AppPrefs.tagBlockPatterns。 */
@Composable
fun TagBlockScreen(onBack: () -> Unit) {
    var regex by remember { mutableStateOf(AppPrefs.tagBlockPatterns) }
    var keywords by remember { mutableStateOf(AppPrefs.tagBlockKeywords) }
    // 用共享的设置子页骨架：内容可滚动 + 挂上 scrollBehavior，
    // 大字标题才会随下拉收缩并居中（此前用非滚动 Column，标题永远停在左上角）
    SettingsSubPage(
        title = Str.tag_block(),
        onBack = onBack,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RuleEditor(
                hint = Str.tag_block_hint(),
                value = regex,
                onValueChange = { regex = it },
                minHeight = 160.dp,
            )
        }
        item {
            RuleEditor(
                hint = Str.tag_block_keyword_label() + "：" + Str.tag_block_keyword_hint(),
                value = keywords,
                onValueChange = { keywords = it },
                minHeight = 120.dp,
            )
        }
        item {
            RuleSaveButton(Str.tag_block_save()) {
                AppPrefs.tagBlockPatterns = regex
                AppPrefs.tagBlockKeywords = keywords
                invalidateFeedCaches()
                onBack()
            }
        }
    }
}


/** 屏蔽人编辑页：每行一个人（作者名或 ID）。 */
@Composable
fun AuthorBlockScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf(AppPrefs.blockedAuthors) }
    SettingsSubPage(
        title = Str.author_block(),
        onBack = onBack,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RuleEditor(
                hint = Str.author_block_hint(),
                value = text,
                onValueChange = { text = it },
                minHeight = 180.dp,
            )
        }
        item {
            RuleSaveButton(Str.author_block_save()) {
                AppPrefs.blockedAuthors = text
                invalidateFeedCaches()
                onBack()
            }
        }
    }
}
