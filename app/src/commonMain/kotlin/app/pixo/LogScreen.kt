package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun LogViewerScreen(onBack: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.view_logs(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        val logs = remember(tick) { LogBuffer.snapshot() }
        val crashText = remember(tick) { readCrashLog().trim() }
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .scrollEndHaptic()
                .overScrollVertical()
                // 没有这行顶栏收不到滚动 → 大字标题不会收缩居中
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            if (crashText.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            Str.crash_log(),
                            style = MiuixTheme.textStyles.title3,
                            color = MiuixTheme.colorScheme.error,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                clipboard.setText(AnnotatedString(crashText))
                            }, enabled = crashText.isNotBlank()) { Text(Str.copy_title()) }
                            Button(onClick = {
                                clearCrashLog()
                                tick++
                            }) { Text(Str.clear_crash_log()) }
                        }
                    }
                }
                item {
                    Text(
                        text = crashText,
                        style = MiuixTheme.textStyles.footnote2,
                        fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(logs.joinToString("\n")))
                    }, enabled = logs.isNotEmpty()) { Text(Str.copy_title()) }
                    Button(onClick = { LogBuffer.clear() }) { Text(Str.clear_logs()) }
                }
            }
            if (logs.isEmpty() && crashText.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text(Str.no_comments(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            } else {
                items(logs) { line ->
                    Text(
                        text = line,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}


/** 崩溃日志独立页：展示持久化的崩溃堆栈，可复制 / 清空。 */
@Composable
fun CrashLogPage(onBack: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    val clipboard = LocalClipboardManager.current
    val text = remember(tick) { readCrashLog().trim() }
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(title = Str.crash_log(), onBack = onBack, scrollBehavior = scrollBehavior) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(text))
                    }, enabled = text.isNotBlank()) { Text(Str.copy_title()) }
                    Button(onClick = {
                        clearCrashLog()
                        tick++
                    }, enabled = text.isNotBlank()) { Text(Str.clear_crash_log()) }
                }
            }
            if (text.isBlank()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = Str.crash_none(),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = text,
                        style = MiuixTheme.textStyles.footnote2,
                        fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
