package app.pixo

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 更新设置页：手动检查更新 + 启动时自动检查。
 *
 * 数据来自 GitHub Releases API（见 [UpdateChecker]），只比较正式版，
 * nightly 预发布不会触发提示。
 */
@Composable
fun UpdatePage(onBack: () -> Unit) {
    var autoCheck by remember { mutableStateOf(AppPrefs.autoCheckUpdate) }
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateInfo?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val currentVersion = remember { appVersionName() }
    val latestText = Str.update_latest()
    val failedText = Str.update_failed()

    SettingsSubPage(title = Str.update(), onBack = onBack) {
        item { SectionTitle(Str.update()) }
        item {
            SettingsGroup {
                ArrowPreference(
                    title = Str.check_update(),
                    summary = if (checking) Str.update_checking() else "v$currentVersion",
                    onClick = {
                        if (checking) return@ArrowPreference
                        checking = true
                        message = null
                        scope.launch {
                            val outcome = runCatching { UpdateChecker.fetchLatest() }
                            checking = false
                            outcome.onSuccess { info ->
                                if (UpdateChecker.isNewer(info.version, currentVersion)) {
                                    result = info
                                } else {
                                    message = latestText
                                }
                            }.onFailure { e ->
                                message = failedText + "：" + e.friendlyMessage()
                            }
                        }
                    },
                )
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
        message?.let { text ->
            item {
                Text(
                    text = text,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                )
            }
        }
    }

    result?.let { info ->
        UpdateAvailableDialog(info = info, onDismiss = { result = null })
    }
}
