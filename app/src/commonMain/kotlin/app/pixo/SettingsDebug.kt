package app.pixo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 调试设置页：日志开关 / 查看日志 / 崩溃日志。
 *
 * 登录信息的导出 / 导入已移到「设置 → 登录」，这里只保留排障相关项。
 */
@Composable
fun DebugPage(
    onOpenLogs: () -> Unit,
    onOpenCrashLog: () -> Unit,
    onBack: () -> Unit,
) {
    var debugLog by remember { mutableStateOf(AppPrefs.debugLog) }
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.settings_debug(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item { SectionTitle(Str.settings_section_logs()) }
            item {
                SettingsGroup {
                    SwitchPreference(
                        title = Str.debug_log(),
                        summary = Str.debug_log_summary(),
                        checked = debugLog,
                        onCheckedChange = {
                            debugLog = it
                            AppPrefs.debugLog = it
                        },
                    )
                    SettingEntryRow(Str.view_logs(), onClick = onOpenLogs)
                    SettingEntryRow(Str.crash_log(), onClick = onOpenCrashLog)
                }
            }
        }
    }
}
