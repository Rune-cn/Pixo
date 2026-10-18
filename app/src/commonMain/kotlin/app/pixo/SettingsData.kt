package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 数据页：导出 / 导入设置、下载目录、清除缓存。 */
@Composable
fun DataPage(onBack: () -> Unit) {
    var toast by remember { mutableStateOf<String?>(null) }
    var downloadDir by remember { mutableStateOf(AppPrefs.downloadDir) }
    val sExportSuccess = Str.export_success()
    val sExportFailed = Str.export_failed()
    val sImportSuccess = Str.import_success()
    val sImportFailed = Str.import_failed()
    val sCacheCleared = Str.cache_cleared()
    val sWebdavSuccess = Str.webdav_success()
    val sWebdavRestored = Str.webdav_restored()
    val sWebdavFailed = Str.webdav_failed()
    var webdavUrl by remember { mutableStateOf(AppPrefs.webdavUrl) }
    var webdavUser by remember { mutableStateOf(AppPrefs.webdavUser) }
    var webdavPass by remember { mutableStateOf(AppPrefs.webdavPassword) }
    var webdavBusy by remember { mutableStateOf(false) }
    // 草稿与已保存配置不同 → 提示先保存，避免「改了但备份还是旧配置」
    val webdavDirty = webdavUrl != AppPrefs.webdavUrl ||
        webdavUser != AppPrefs.webdavUser ||
        webdavPass != AppPrefs.webdavPassword
    val sSaved = Str.saved()
    val scope = rememberCoroutineScope()
    val pickFolder = rememberDownloadFolderPicker { dir -> downloadDir = dir }
    val exportFile = rememberExportSettingsFile { ok ->
        toast = if (ok) sExportSuccess else sExportFailed
    }
    val importFile = rememberImportSettingsFile { json ->
        if (json == null) return@rememberImportSettingsFile
        toast = if (importSettingsJson(json)) sImportSuccess else sImportFailed
    }
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.settings_data(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                overscrollEffect = null,
            ) {
                item { SectionTitle(Str.settings_section_data()) }
                item {
                    SettingsGroup {
                        ArrowPreference(
                            title = Str.export_settings(),
                            summary = Str.export_settings_summary(),
                            onClick = { exportFile() },
                        )
                        ArrowPreference(
                            title = Str.import_settings(),
                            summary = Str.import_settings_summary(),
                            onClick = { importFile() },
                        )
                        ArrowPreference(
                            title = Str.clear_cache(),
                            summary = Str.clear_cache_summary(),
                            onClick = {
                                clearAppCache()
                                toast = sCacheCleared
                            },
                        )
                    }
                }
                item { SectionTitle(Str.settings_section_storage()) }
                item {
                    SettingsGroup {
                        ArrowPreference(
                            title = Str.download_folder(),
                            summary = downloadDirLabel(downloadDir),
                            onClick = { pickFolder() },
                        )
                    }
                }
                item { SectionTitle("WebDAV") }
                item {
                    SettingsGroup {
                        Column(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = Str.webdav_desc(),
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            TextField(
                                value = webdavUrl,
                                onValueChange = { webdavUrl = it },
                                label = Str.webdav_url(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            TextField(
                                value = webdavUser,
                                onValueChange = { webdavUser = it },
                                label = Str.webdav_user(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            TextField(
                                value = webdavPass,
                                onValueChange = { webdavPass = it },
                                label = Str.webdav_password(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (webdavDirty) {
                                Text(
                                    text = Str.webdav_unsaved(),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.primary,
                                )
                            }
                            Button(
                                onClick = {
                                    AppPrefs.webdavUrl = webdavUrl.trim()
                                    AppPrefs.webdavUser = webdavUser.trim()
                                    AppPrefs.webdavPassword = webdavPass
                                    toast = sSaved
                                },
                                enabled = webdavDirty,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(Str.webdav_save()) }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            webdavBusy = true
                                            val ok = WebDavApi.backup(exportSettingsJson())
                                            toast = if (ok) sWebdavSuccess else sWebdavFailed
                                            webdavBusy = false
                                        }
                                    },
                                    enabled = !webdavBusy && !webdavDirty,
                                    modifier = Modifier.weight(1f),
                                ) { Text(Str.webdav_backup()) }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            webdavBusy = true
                                            val json = WebDavApi.restore()
                                            toast = if (json != null && importSettingsJson(json)) sWebdavRestored else sWebdavFailed
                                            webdavBusy = false
                                        }
                                    },
                                    enabled = !webdavBusy && !webdavDirty,
                                    modifier = Modifier.weight(1f),
                                ) { Text(Str.webdav_restore()) }
                            }
                        }
                    }
                }
            }
            BottomToast(toast)
        }
    }
}


/** 关于页右侧值文本：与列表项等高的主题次要色。 */
@Composable
internal fun ValueText(text: String) {
    Text(
        text = text,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
    )
}


/** 下载目录的可读显示：SAF tree URI → 路径；空 → 默认 Pictures/Pixo。 */
internal fun downloadDirLabel(dir: String): String {
    if (dir.isBlank()) return "Pictures/Pixo"
    val i = dir.lastIndexOf("tree/")
    if (i >= 0) {
        val docId = dir.substring(i + 5)
            .replace("%3A", ":").replace("%2F", "/")
            .removePrefix("primary:").removePrefix("home:")
        return docId.ifBlank { dir }
    }
    return dir
}
