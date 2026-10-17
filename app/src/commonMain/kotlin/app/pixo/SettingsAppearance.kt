package app.pixo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

/** 外观设置页：语言 / 主题 / 动态取色 / 悬浮底栏 / 液态玻璃。 */
@Composable
fun AppearancePage(
    themeMode: ColorSchemeMode,
    onThemeModeChange: (ColorSchemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    floatingBar: Boolean,
    onFloatingBarChange: (Boolean) -> Unit,
    liquidGlass: Boolean,
    onLiquidGlassChange: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    val themeOptions = listOf(Str.theme_follow(), Str.theme_dark(), Str.theme_light())
    val languageOptions = listOf(Str.language_follow(), Str.language_zh(), Str.language_en())
    val languageValues = listOf("system", "zh", "en")
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.settings_appearance(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item { SectionTitle(Str.settings_section_general()) }
            item {
                SettingsGroup {
                    PixoDropdownPreference(
                        title = Str.settings_language(),
                        summary = languageOptions[languageValues.indexOf(language).coerceAtLeast(0)],
                        options = languageOptions,
                        selectedIndex = languageValues.indexOf(language).coerceAtLeast(0),
                        onSelectedIndexChange = { onLanguageChange(languageValues[it]) },
                    )
                    PixoDropdownPreference(
                        title = Str.theme_mode(),
                        summary = themeOptions[themeModes.indexOf(themeMode).coerceAtLeast(0)],
                        options = themeOptions,
                        selectedIndex = themeModes.indexOf(themeMode).coerceAtLeast(0),
                        onSelectedIndexChange = { onThemeModeChange(themeModes[it]) },
                    )
                    SwitchPreference(
                        title = Str.dynamic_color(),
                        summary = Str.dynamic_color_summary(),
                        checked = dynamicColor,
                        onCheckedChange = { onDynamicColorChange(it) },
                    )
                }
            }
            item { SectionTitle(Str.settings_section_bottom_bar()) }
            item {
                SettingsGroup {
                    SwitchPreference(
                        title = Str.floating_bar(),
                        summary = Str.floating_bar_summary(),
                        checked = floatingBar,
                        onCheckedChange = { onFloatingBarChange(it) },
                    )
                    // 液态玻璃只对悬浮底栏生效：悬浮底栏关闭时隐藏该项（带展开动画）
                    AnimatedVisibility(visible = floatingBar) {
                        SwitchPreference(
                            title = Str.liquid_glass(),
                            summary = Str.liquid_glass_summary(),
                            checked = liquidGlass,
                            onCheckedChange = { onLiquidGlassChange(it) },
                        )
                    }
                }
            }
        }
    }
}
