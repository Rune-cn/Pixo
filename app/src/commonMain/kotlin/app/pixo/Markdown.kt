package app.pixo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 文档弹窗：基于 Miuix 原生 [WindowBottomSheet]，从下往上滑出、可下拉/返回关闭。
 * 内容用 [MarkdownBlock] 渲染（JetBrains Markdown 解析器 + Compose 渲染）。
 */
@Composable
internal fun DocSheet(title: String, content: String, onDismiss: () -> Unit) {
    var show by remember { mutableStateOf(true) }

    WindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = { show = false },
        onDismissFinished = onDismiss,
        insideMargin = DpSize(0.dp, 0.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 30.dp),
        ) {
            MarkdownBlock(
                content = content,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
