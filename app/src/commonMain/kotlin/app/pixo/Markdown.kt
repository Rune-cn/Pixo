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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.markdownAlertColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 文档弹窗：基于 Miuix 原生 [WindowBottomSheet]，从下往上滑出、可下拉/返回关闭，
 * 背景与文字跟随主题（不再固定深色）。内容由成熟的
 * mikepenz multiplatform-markdown-renderer 渲染（JetBrains Markdown 解析器，GFM 语法）。
 */
@Composable
internal fun DocSheet(title: String, content: String, onDismiss: () -> Unit) {
    var show by remember { mutableStateOf(true) }
    val scheme = MiuixTheme.colorScheme
    val isDark = remember(scheme) { scheme.surface.luminance() < 0.5f }

    val colors = remember(scheme, isDark) {
        DefaultMarkdownColors(
            text = scheme.onSurface,
            codeBackground = scheme.surfaceContainerHigh,
            inlineCodeBackground = scheme.surfaceContainer,
            dividerColor = scheme.dividerLine.copy(alpha = 0.6f),
            tableBackground = scheme.surfaceContainerHigh,
            alert = markdownAlertColors(darkTheme = isDark),
        )
    }
    val bodyStyle = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, color = scheme.onSurface)
    val typography = remember(scheme) {
        DefaultMarkdownTypography(
            h1 = bodyStyle.copy(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
            h2 = bodyStyle.copy(fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
            h3 = bodyStyle.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
            h4 = bodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            h5 = bodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            h6 = bodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            text = bodyStyle,
            code = bodyStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = scheme.primary,
            ),
            inlineCode = bodyStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = scheme.primary,
            ),
            quote = bodyStyle.copy(color = scheme.onSurfaceVariantSummary),
            paragraph = bodyStyle,
            ordered = bodyStyle,
            bullet = bodyStyle,
            list = bodyStyle,
            textLink = TextLinkStyles(style = SpanStyle(color = scheme.primary, fontWeight = FontWeight.Medium)),
            table = bodyStyle.copy(fontSize = 14.sp, lineHeight = 20.sp),
        )
    }

    WindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = { show = false },
        onDismissFinished = onDismiss,
        endAction = {
            IconButton(onClick = { show = false }) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = "关闭",
                    tint = scheme.onSurfaceVariantActions,
                )
            }
        },
        insideMargin = DpSize(0.dp, 0.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 30.dp),
        ) {
            Markdown(
                content = content,
                colors = colors,
                typography = typography,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
