package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 登录页：导出 / 导入完整登录信息（Token + Cookie），用于换机或排查登录问题。
 *
 * 凭据只在本地读写，不经过任何服务器；导出内容是 JSON，导入也兼容旧版调试页的
 * `---- refresh_token ----` 分段文本（见 [LoginTransfer]）。
 */
@Composable
fun LoginPage(onBack: () -> Unit) {
    var exportText by remember { mutableStateOf<String?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var importDraft by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var messageError by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    val sImported = Str.login_imported()
    val sFailed = Str.login_import_failed()
    val selfId = AppPrefs.selfUserId
    val lastUser = AppPrefs.lastLoginUser
    val statusText = if (selfId <= 0L && lastUser.isBlank()) {
        Str.not_logged_in()
    } else {
        Str.login_status(lastUser.ifBlank { selfId.toString() })
    }

    SettingsSubPage(title = Str.settings_login(), onBack = onBack) {
        item { SectionTitle(Str.settings_login()) }
        item {
            SettingsGroup {
                ArrowPreference(
                    title = Str.export_login_info(),
                    summary = Str.export_login_summary(),
                    onClick = { exportText = LoginTransfer.export() },
                )
                ArrowPreference(
                    title = Str.import_login_info(),
                    summary = Str.import_login_summary(),
                    onClick = {
                        importDraft = ""
                        showImport = true
                    },
                )
            }
        }
        item {
            Text(
                text = statusText,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
            )
        }
        message?.let { text ->
            item {
                Text(
                    text = text,
                    style = MiuixTheme.textStyles.footnote2,
                    color = if (messageError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                )
            }
        }
    }

    // 导出：展示 JSON 全文 + 一键复制
    exportText?.let { text ->
        OverlayDialog(show = true, onDismissRequest = { exportText = null }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Str.export_login_dialog_title(), style = MiuixTheme.textStyles.title3)
                Text(
                    text = text,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                )
                Button(
                    onClick = { clipboard.setText(AnnotatedString(text)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Str.copy_all()) }
                Button(onClick = { exportText = null }, modifier = Modifier.fillMaxWidth()) {
                    Text(Str.cancel())
                }
            }
        }
    }

    // 导入：粘贴 JSON 或旧版分段文本
    if (showImport) {
        OverlayDialog(show = true, onDismissRequest = { showImport = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Str.import_login_info(), style = MiuixTheme.textStyles.title3)
                Card(Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = importDraft,
                        onValueChange = { importDraft = it },
                        textStyle = MiuixTheme.textStyles.footnote2.copy(
                            color = MiuixTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp)
                            .padding(12.dp),
                    )
                }
                Button(
                    onClick = {
                        val error = LoginTransfer.import(importDraft)
                        showImport = false
                        messageError = error != null
                        message = if (error == null) sImported else "$sFailed：$error"
                        if (error == null) SettingsRevision.bump()
                    },
                    enabled = importDraft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Str.import_action()) }
                Button(onClick = { showImport = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(Str.cancel())
                }
            }
        }
    }
}
