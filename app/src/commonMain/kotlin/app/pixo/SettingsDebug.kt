package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 调试设置页：日志开关 / 查看日志 / 崩溃日志。 */
@Composable
fun DebugPage(
    onOpenLogs: () -> Unit,
    onOpenCrashLog: () -> Unit,
    onBack: () -> Unit,
) {
    var debugLog by remember { mutableStateOf(AppPrefs.debugLog) }
    var showExport by remember { mutableStateOf(false) }
    var exportCopied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    fun exportText(): String = buildString {
        appendLine("Pixo login info (debug)")
        appendLine("---- refresh_token ----")
        appendLine(AppPrefs.refreshToken.ifBlank { "(empty)" })
        appendLine("---- access_token ----")
        appendLine(AppPrefs.accessToken.ifBlank { "(empty)" })
        appendLine("---- cookie ----")
        appendLine(AppPrefs.cookie.ifBlank { "(empty)" })
        appendLine("---- self_user_id ----")
        appendLine(AppPrefs.selfUserId.toString())
        appendLine("---- last_login_user ----")
        appendLine(AppPrefs.lastLoginUser.ifBlank { "(empty)" })
    }
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
            item { SectionTitle(Str.settings_section_login()) }
            item {
                SettingsGroup {
                    SettingEntryRow(Str.export_login_info(), onClick = { showExport = true })
                    Text(
                        text = Str.export_login_summary(),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }

    OverlayDialog(show = showExport, onDismissRequest = { showExport = false }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Str.export_login_dialog_title(), style = MiuixTheme.textStyles.title3)
            Text(
                text = exportText(),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(exportText()))
                    exportCopied = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(Str.copy_all())
            }
            if (exportCopied) {
                Text(
                    text = Str.export_copied(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            Button(onClick = { showExport = false }, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
        }
    }
}
