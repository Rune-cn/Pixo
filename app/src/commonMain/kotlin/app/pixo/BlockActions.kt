package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 屏蔽相关动作的共享实现。
 *
 * 「长按标签 → 复制/屏蔽」「作者页 ⋮ → 浏览器/分享/屏蔽此人」
 * 之前在详情页、小说详情页、作者页、瀑布流卡片里各写了一份，
 * 连去重逻辑都靠复制粘贴保持一致；现在只有这一份。
 */

/**
 * 往「一行一条」的屏蔽名单里追加一条规则（去重、忽略大小写）。
 *
 * @return 追加后的完整文本；规则已存在时返回 null（调用方无需写回）。
 */
internal fun appendBlockRule(existing: String, rule: String): String? {
    val trimmed = rule.trim()
    if (trimmed.isEmpty()) return null
    val items = existing.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    if (items.any { it.equals(trimmed, ignoreCase = true) }) return null
    return (items + trimmed).joinToString("\n")
}

/** 把标签加入屏蔽名单并让列表缓存失效。 */
internal fun blockTag(tag: String) {
    appendBlockRule(AppPrefs.tagBlockKeywords, tag)?.let {
        AppPrefs.tagBlockKeywords = it
        invalidateFeedCaches()
    }
}

/** 把作者（按 ID）加入屏蔽名单并让列表缓存失效。 */
internal fun blockAuthor(authorId: Long) {
    appendBlockRule(AppPrefs.blockedAuthors, authorId.toString())?.let {
        AppPrefs.blockedAuthors = it
        invalidateFeedCaches()
    }
}

/**
 * 长按标签的动作面板：复制 / 屏蔽。
 *
 * @param tag 非 null 时展示面板
 * @param onBlocked 屏蔽后回调（用于在页面上弹提示）
 */
@Composable
internal fun TagActionDialog(
    tag: String?,
    onDismiss: () -> Unit,
    onBlocked: (String) -> Unit,
) {
    if (tag == null) return
    val clipboard = LocalClipboardManager.current
    OverlayDialog(show = true, onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("#$tag", style = MiuixTheme.textStyles.title3)
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(tag))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(Str.copy_tag()) }
            Button(
                onClick = {
                    blockTag(tag)
                    onBlocked(tag)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(Str.block_tag()) }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
        }
    }
}

/**
 * 作者「更多」动作面板：在浏览器打开 / 分享主页 / 屏蔽此人。
 *
 * @param userId 目标作者，屏蔽时按 ID 记录（比按昵称稳定）
 */
@Composable
internal fun UserActionDialog(
    show: Boolean,
    userId: Long,
    onDismiss: () -> Unit,
) {
    if (!show) return
    OverlayDialog(show = true, onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(Str.more_actions(), style = MiuixTheme.textStyles.title3)
            Button(
                onClick = {
                    onDismiss()
                    openUrlInBrowser("https://www.pixiv.net/users/$userId")
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(Str.open_in_browser()) }
            Button(
                onClick = {
                    onDismiss()
                    shareText("https://www.pixiv.net/users/$userId")
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(Str.share()) }
            Button(
                onClick = {
                    onDismiss()
                    blockAuthor(userId)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(Str.block_this_author()) }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(Str.cancel()) }
        }
    }
}
