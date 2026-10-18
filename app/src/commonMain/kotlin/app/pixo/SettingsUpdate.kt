package app.pixo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 更新设置页：只负责「启动时自动检查更新」。
 *
 * 手动「检查更新」入口放在「关于」页（那里本来就有版本信息与更新入口），
 * 避免两处重复。
 */
@Composable
fun UpdatePage(onBack: () -> Unit) {
    var autoCheck by remember { mutableStateOf(AppPrefs.autoCheckUpdate) }

    SettingsSubPage(title = Str.update(), onBack = onBack) {
        item { SectionTitle(Str.update()) }
        item {
            SettingsGroup {
                SwitchPreference(
                    title = Str.auto_check_update(),
                    summary = Str.auto_check_summary(),
                    checked = autoCheck,
                    onCheckedChange = {
                        autoCheck = it
                        AppPrefs.autoCheckUpdate = it
                    },
                )
            }
        }
    }
}
