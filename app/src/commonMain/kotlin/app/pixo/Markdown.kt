package app.pixo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close

// ---------- 控制台配色（固定深色终端风，不随主题） ----------

internal val ConsoleBg = Color(0xFF0C1015)
internal val ConsolePanel = Color(0xFF11161D)
internal val ConsoleFg = Color(0xFFC9D1D9)
internal val ConsoleHeading = Color(0xFF58A6FF)
internal val ConsoleStrong = Color(0xFFE6EDF3)
internal val ConsoleCode = Color(0xFF79C0FF)
internal val ConsoleLink = Color(0xFF4493F8)
internal val ConsoleQuote = Color(0xFF8B949E)
internal val ConsoleAccent = Color(0xFF3FB950)
internal val ConsoleMuted = Color(0xFF6E7681)

// ---------- Markdown 块模型 ----------

internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class ListItem(val ordered: Boolean, val index: String, val indent: Int, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class CodeBlock(val text: String) : MdBlock
    data object Hr : MdBlock
}

private val headingRe = Regex("^#{1,6}\\s+")
private val orderedRe = Regex("^\\d+\\.\\s+")

/** 轻量 Markdown 解析：标题 / 段落 / 无序有序列表（缩进 2 空格一级）/ 引用 / 代码块 / 分隔线。 */
internal fun parseMarkdown(source: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val paragraph = StringBuilder()
    val quote = StringBuilder()
    fun flushParagraph() {
        if (paragraph.isNotBlank()) {
            out += MdBlock.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }
    }
    fun flushQuote() {
        if (quote.isNotBlank()) {
            out += MdBlock.Quote(quote.toString().trim())
            quote.clear()
        }
    }
    val lines = source.replace("\r\n", "\n").split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trimEnd()
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("```") -> {
                flushParagraph(); flushQuote()
                val buf = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    buf.appendLine(lines[i])
                    i++
                }
                i++
                out += MdBlock.CodeBlock(buf.toString().trimEnd())
                continue
            }
            headingRe.containsMatchIn(trimmed) -> {
                flushParagraph(); flushQuote()
                val level = headingRe.find(trimmed)!!.value.count { it == '#' }.coerceIn(1, 6)
                out += MdBlock.Heading(level, trimmed.substringAfter(headingRe.find(trimmed)!!.value).trim())
            }
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                flushParagraph(); flushQuote()
                out += MdBlock.Hr
            }
            trimmed.startsWith(">") -> {
                flushParagraph()
                quote.appendLine(trimmed.removePrefix(">").trim())
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                flushParagraph(); flushQuote()
                val indent = (line.length - trimmed.length) / 2
                out += MdBlock.ListItem(false, "•", indent, trimmed.drop(2).trim())
            }
            orderedRe.containsMatchIn(trimmed) -> {
                flushParagraph(); flushQuote()
                val indent = (line.length - trimmed.length) / 2
                val m = orderedRe.find(trimmed)!!
                out += MdBlock.ListItem(true, trimmed.substring(0, m.value.length - 1), indent, trimmed.substring(m.value.length).trim())
            }
            line.isBlank() -> { flushParagraph(); flushQuote() }
            else -> {
                flushQuote()
                paragraph.appendLine(trimmed)
            }
        }
        i++
    }
    flushParagraph(); flushQuote()
    return out
}

/** 行内语法：`code`、**bold**、*italic*、[label](url)。 */
internal fun buildInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            c == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i) {
                    pushStyle(SpanStyle(color = ConsoleCode, fontFamily = FontFamily.Monospace))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append(c); i++
                }
            }
            c == '[' -> {
                val close = text.indexOf(']', i + 1)
                if (close > i && close + 1 < text.length && text[close + 1] == '(') {
                    val end = text.indexOf(')', close + 2)
                    if (end > close) {
                        val label = text.substring(i + 1, close)
                        val url = text.substring(close + 2, end)
                        pushStringAnnotation("link", url)
                        pushStyle(SpanStyle(color = ConsoleLink, textDecoration = TextDecoration.Underline))
                        append(label)
                        pop(); pop()
                        i = end + 1
                    } else {
                        append(c); i++
                    }
                } else {
                    append(c); i++
                }
            }
            c == '*' && i + 1 < text.length && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end > i) {
                    pushStyle(SpanStyle(color = ConsoleStrong, fontWeight = FontWeight.Bold))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                } else {
                    append(c); i++
                }
            }
            c == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end > i) {
                    pushStyle(SpanStyle(color = ConsoleFg, fontStyle = FontStyle.Italic))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append(c); i++
                }
            }
            else -> {
                append(c); i++
            }
        }
    }
}

/** 控制台风格富文本（等宽 + 可点链接）。 */
@Composable
private fun ConsoleRichText(
    text: String,
    color: Color = ConsoleFg,
    size: TextUnit = 13.sp,
    modifier: Modifier = Modifier,
) {
    val annotated = remember(text) { buildInline(text) }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = size,
            lineHeight = size * 1.6f,
        ),
        onClick = { offset ->
            annotated.getStringAnnotations("link", offset, offset).firstOrNull()?.let {
                openUrlInBrowser(it.item)
            }
        },
    )
}

/** Markdown 内容渲染（控制台样式）。 */
@Composable
internal fun ConsoleMarkdown(content: String) {
    val blocks = remember(content) { parseMarkdown(content) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = block.text,
                    color = ConsoleHeading,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = when (block.level) {
                        1 -> 18.sp
                        2 -> 15.sp
                        else -> 13.5.sp
                    },
                    modifier = Modifier.padding(top = if (block.level <= 2) 6.dp else 2.dp),
                )
                is MdBlock.Paragraph -> ConsoleRichText(block.text)
                is MdBlock.ListItem -> Row(modifier = Modifier.padding(start = (block.indent * 12).dp)) {
                    Text(
                        text = if (block.ordered) block.index else "•",
                        color = ConsoleAccent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.width(if (block.ordered) 36.dp else 18.dp),
                    )
                    ConsoleRichText(block.text, modifier = Modifier.weight(1f))
                }
                is MdBlock.Quote -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ConsoleAccent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    ConsoleRichText(block.text, color = ConsoleQuote)
                }
                is MdBlock.CodeBlock -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    ConsoleRichText(block.text, color = ConsoleCode, size = 12.5.sp)
                }
                MdBlock.Hr -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ConsoleMuted.copy(alpha = 0.25f)),
                )
            }
        }
    }
}

/**
 * 文档控制台：点击「隐私政策 / 许可证 / 免责声明」后从底部滑出的终端式面板，
 * 固定深色 + 等宽字体 + Markdown 渲染；点击遮罩或右上角关闭。
 */
@Composable
internal fun DocConsoleSheet(title: String, content: String, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { visible = true }
    fun close() {
        scope.launch {
            visible = false
            delay(240)
            onDismiss()
        }
    }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { close() },
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(280)) { it },
            exit = slideOutVertically(tween(240)) { it },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.78f)
                        .background(ConsoleBg, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                ) {
                    // 顶部拖拽指示条
                    Box(
                        Modifier
                            .padding(top = 10.dp)
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .background(ConsoleMuted.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
                    )
                    // 标题栏（终端提示符风格）
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = ">",
                            color = ConsoleAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title,
                            color = ConsoleHeading,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { close() }) {
                            Icon(
                                imageVector = MiuixIcons.Close,
                                contentDescription = "关闭",
                                tint = ConsoleFg,
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ConsoleMuted.copy(alpha = 0.2f)),
                    )
                    // 内容区
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        ConsoleMarkdown(content)
                        Spacer(Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}
