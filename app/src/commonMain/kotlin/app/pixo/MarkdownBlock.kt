package app.pixo

// Markdown 渲染：JetBrains intellij-markdown AST 解析器 + Compose 节点渲染。
// 解析一次 AST，按节点类型渲染段落 / 标题 / 列表 / 引用 / 表格 / 代码 / 行内样式（加粗、斜体、行内代码、链接）。

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val markdownFlavour by lazy { GFMFlavourDescriptor(makeHttpsAutoLinks = true, useSafeLinks = true) }
private val markdownParser by lazy { MarkdownParser(markdownFlavour) }

/** 渲染用配色（映射到主题色板）。 */
private data class MdColors(
    val primary: Color,
    val onSurface: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color,
)

private val LocalMdTextStyle = staticCompositionLocalOf { TextStyle.Default }

/**
 * Markdown 渲染器：解析 [content] 为 AST 后渲染全部块级节点。
 */
@Composable
fun MarkdownBlock(
    content: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val colors = remember(scheme) {
        MdColors(
            primary = scheme.primary,
            onSurface = scheme.onSurface,
            surface = scheme.surface,
            surfaceVariant = scheme.surfaceContainer,
            onSurfaceVariant = scheme.onSurfaceVariantSummary,
            outlineVariant = scheme.dividerLine,
        )
    }
    val tree = remember(content) { markdownParser.buildMarkdownTreeFromString(content) }
    Column(modifier = modifier) {
        tree.children.forEach { child ->
            MarkdownNode(node = child, content = content, colors = colors)
        }
    }
}

/** 文本组件：沿用传入的样式 + 可覆盖颜色/字重/字号。 */
@Composable
private fun MdText(
    text: AnnotatedString,
    style: TextStyle = LocalMdTextStyle.current,
    color: Color? = null,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit? = null,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        style = style.copy(
            color = color ?: style.color,
            fontWeight = fontWeight ?: style.fontWeight,
            fontSize = fontSize ?: style.fontSize,
        ),
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
private fun MdText(
    text: String,
    style: TextStyle = LocalMdTextStyle.current,
    color: Color? = null,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit? = null,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) = MdText(
    AnnotatedString(text), style, color, fontWeight, fontSize, modifier, maxLines, overflow,
)

@Composable
private fun MarkdownNode(
    node: ASTNode,
    content: String,
    colors: MdColors,
    listLevel: Int = 0,
) {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE ->
            node.children.forEach { MarkdownNode(it, content, colors, listLevel) }

        MarkdownElementTypes.PARAGRAPH -> MarkdownParagraph(node, content, colors)

        MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 ->
            MarkdownHeading(node, content, colors)

        MarkdownElementTypes.UNORDERED_LIST -> MarkdownUnorderedList(node, content, colors, listLevel)
        MarkdownElementTypes.ORDERED_LIST -> MarkdownOrderedList(node, content, colors, listLevel)

        MarkdownElementTypes.BLOCK_QUOTE -> MarkdownBlockquote(node, content, colors)
        MarkdownElementTypes.INLINE_LINK -> MarkdownInlineLink(node, content, colors)

        MarkdownElementTypes.EMPH -> ProvideMdTextStyle(TextStyle(fontStyle = FontStyle.Italic)) {
            node.children.forEach { MarkdownNode(it, content, colors, listLevel) }
        }

        MarkdownElementTypes.STRONG -> ProvideMdTextStyle(TextStyle(fontWeight = FontWeight.SemiBold)) {
            node.children.forEach { MarkdownNode(it, content, colors, listLevel) }
        }

        GFMElementTypes.STRIKETHROUGH -> MdText(
            node.getTextInNode(content),
            style = LocalMdTextStyle.current.copy(textDecoration = TextDecoration.LineThrough),
        )

        GFMElementTypes.TABLE -> MarkdownTable(node, content, colors)

        MarkdownTokenTypes.HORIZONTAL_RULE -> MdText(
            "—",
            color = colors.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
        )

        GFMElementTypes.INLINE_MATH -> MdText(
            node.getTextInNode(content),
            style = LocalMdTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 0.9.em),
        )

        MarkdownElementTypes.CODE_SPAN -> MdText(
            node.getTextInNode(content).trim('`'),
            style = LocalMdTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 0.9.em),
            color = colors.primary,
        )

        MarkdownElementTypes.CODE_FENCE -> MarkdownCodeFence(node, content, colors)
        MarkdownElementTypes.CODE_BLOCK -> MarkdownCodeBlock(node.getTextInNode(content), colors)

        MarkdownTokenTypes.TEXT -> MdText(node.getTextInNode(content))

        else -> node.children.forEach { MarkdownNode(it, content, colors, listLevel) }
    }
}

@Composable
private fun ProvideMdTextStyle(style: TextStyle, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMdTextStyle provides LocalMdTextStyle.current.merge(style)) { content() }
}

@Composable
private fun MarkdownParagraph(
    node: ASTNode,
    content: String,
    colors: MdColors,
) {
    val annotated = remember(content, colors) {
        buildAnnotatedString {
            node.children.forEach { child -> appendMarkdownInline(child, content, colors) }
        }
    }
    MdText(
        text = annotated,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun MarkdownHeading(
    node: ASTNode,
    content: String,
    colors: MdColors,
) {
    val level = when (node.type) {
        MarkdownElementTypes.ATX_1 -> 1
        MarkdownElementTypes.ATX_2 -> 2
        MarkdownElementTypes.ATX_3 -> 3
        MarkdownElementTypes.ATX_4 -> 4
        MarkdownElementTypes.ATX_5 -> 5
        else -> 6
    }
    val fontSize = when (level) {
        1 -> 24.sp; 2 -> 22.sp; 3 -> 20.sp; 4 -> 18.sp; 5 -> 16.sp; else -> 14.sp
    }
    val verticalPadding = when (level) {
        1 -> 16.dp; 2 -> 14.dp; 3 -> 12.dp; 4 -> 10.dp; 5 -> 8.dp; else -> 6.dp
    }
    val contentNode = node.children.firstOrNull { it.type == MarkdownTokenTypes.ATX_CONTENT }
    if (contentNode != null) {
        ProvideMdTextStyle(TextStyle(fontWeight = FontWeight.Bold, fontSize = fontSize)) {
            MarkdownParagraph(contentNode, content, colors)
        }
    } else {
        Spacer(Modifier.height(verticalPadding))
    }
}

@Composable
private fun MarkdownUnorderedList(
    node: ASTNode,
    content: String,
    colors: MdColors,
    level: Int,
) {
    val bullet = when (level % 3) {
        0 -> "• "; 1 -> "◦ "; else -> "▪ "
    }
    Column(Modifier.padding(start = (level * 8).dp)) {
        node.children.forEach { child ->
            if (child.type == MarkdownElementTypes.LIST_ITEM) {
                MarkdownListItem(child, content, bullet, colors, level)
            }
        }
    }
}

@Composable
private fun MarkdownOrderedList(
    node: ASTNode,
    content: String,
    colors: MdColors,
    level: Int,
) {
    Column(Modifier.padding(start = (level * 8).dp)) {
        var index = 1
        node.children.forEach { child ->
            if (child.type == MarkdownElementTypes.LIST_ITEM) {
                val numberText = child.findChildOfTypeRecursive(MarkdownTokenTypes.LIST_NUMBER)
                    ?.getTextInNode(content) ?: "$index. "
                MarkdownListItem(child, content, numberText, colors, level)
                index++
            }
        }
    }
}

@Composable
private fun MarkdownListItem(
    node: ASTNode,
    content: String,
    bulletText: String,
    colors: MdColors,
    level: Int,
) {
    val (directContent, nestedLists) = separateContentAndLists(node)
    Column {
        if (directContent.isNotEmpty()) {
            Row {
                MdText(bulletText, color = colors.primary, modifier = Modifier.padding(end = 4.dp))
                Column {
                    directContent.forEach { MarkdownNode(it, content, colors, level) }
                }
            }
        }
        nestedLists.forEach { MarkdownNode(it, content, colors, level + 1) }
    }
}

private fun separateContentAndLists(listItemNode: ASTNode): Pair<List<ASTNode>, List<ASTNode>> {
    val directContent = mutableListOf<ASTNode>()
    val nestedLists = mutableListOf<ASTNode>()
    listItemNode.children.forEach { child ->
        when (child.type) {
            MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> nestedLists.add(child)
            else -> directContent.add(child)
        }
    }
    return directContent to nestedLists
}

@Composable
private fun MarkdownBlockquote(
    node: ASTNode,
    content: String,
    colors: MdColors,
) {
    ProvideMdTextStyle(LocalMdTextStyle.current.copy(fontStyle = FontStyle.Italic)) {
        val borderColor = colors.onSurfaceVariant.copy(alpha = 0.3f)
        val bgColor = colors.surfaceVariant.copy(alpha = 0.2f)
        Column(
            modifier = Modifier
                .drawWithContent {
                    drawRect(color = bgColor, size = size)
                    drawContent()
                    drawRect(color = borderColor, size = Size(10f, size.height))
                }
                .padding(8.dp),
        ) {
            node.children.forEach { MarkdownNode(it, content, colors) }
        }
    }
}

@Composable
private fun MarkdownInlineLink(
    node: ASTNode,
    content: String,
    colors: MdColors,
) {
    val linkDest = node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content) ?: ""
    val linkText = node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_TEXT)?.getTextInNode(content) ?: linkDest
    MdText(
        linkText,
        color = colors.primary,
        style = LocalMdTextStyle.current.copy(textDecoration = TextDecoration.Underline),
        modifier = Modifier.clickable { openUrlInBrowser(linkDest) },
    )
}

@Composable
private fun MarkdownCodeFence(
    node: ASTNode,
    content: String,
    colors: MdColors,
) {
    val contentStartIndex = node.children.indexOfFirst { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
    if (contentStartIndex == -1) return
    val eolElement = node.children.subList(0, contentStartIndex).findLast { it.type == MarkdownTokenTypes.EOL } ?: return
    val codeContentStartOffset = eolElement.endOffset
    val codeContentEndOffset = node.children.findLast { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }?.endOffset ?: return
    val code = content.substring(codeContentStartOffset, codeContentEndOffset).trimIndent()
    val language = node.findChildOfTypeRecursive(MarkdownTokenTypes.FENCE_LANG)?.getTextInNode(content)
    MarkdownCodeBlock(code, colors, language)
}

@Composable
private fun MarkdownCodeBlock(
    code: String,
    colors: MdColors,
    language: String? = null,
) {
    val collapsedLines = 10
    val codeLines = remember(code) { code.lines() }
    var isExpanded by remember(code) { mutableStateOf(codeLines.size <= collapsedLines) }
    val canCollapse = codeLines.size > collapsedLines
    val displayCode = if (isExpanded) code else codeLines.take(collapsedLines).joinToString("\n")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .border(BorderStroke(0.5.dp, colors.outlineVariant), RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!language.isNullOrBlank()) {
                MdText(language, color = colors.onSurfaceVariant, fontSize = 11.sp)
            }
            if (canCollapse) {
                Spacer(Modifier.weight(1f))
                MdText(
                    if (isExpanded) "收起 ▲" else "展开 ▼ (${codeLines.size} 行)",
                    color = colors.primary,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(4.dp),
                )
            }
        }
        MdText(
            displayCode,
            style = LocalMdTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MarkdownTable(
    node: ASTNode,
    content: String,
    colors: MdColors,
) {
    val headerNode = node.children.find { it.type == GFMElementTypes.HEADER }
    val rowNodes = node.children.filter { it.type == GFMElementTypes.ROW }
    val columnCount = headerNode?.children?.count { it.type == GFMTokenTypes.CELL } ?: 0
    if (columnCount == 0) return

    val headerCells = headerNode?.children
        ?.filter { it.type == GFMTokenTypes.CELL }
        ?.map { it.getTextInNode(content).trim() } ?: emptyList()

    val rows = rowNodes.map { rowNode ->
        rowNode.children.filter { it.type == GFMTokenTypes.CELL }.map { it.getTextInNode(content).trim() }
    }

    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(0.5.dp, colors.outlineVariant), RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            headerCells.forEach { cell ->
                MdText(cell, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
            }
        }
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (index % 2 == 0) colors.surface
                        else colors.surfaceVariant.copy(alpha = 0.15f),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                row.forEach { cell ->
                    MdText(cell, modifier = Modifier.weight(1f), maxLines = 2)
                }
            }
        }
    }
}

// ---- 行内 AnnotatedString 构建 ----

private fun AnnotatedString.Builder.appendMarkdownInline(
    node: ASTNode,
    content: String,
    colors: MdColors,
) {
    when {
        node is LeafASTNode -> append(node.getTextInNode(content))

        node.type == MarkdownElementTypes.EMPH -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            node.children.trimSurrounding(MarkdownTokenTypes.EMPH, 1).forEach { appendMarkdownInline(it, content, colors) }
        }

        node.type == MarkdownElementTypes.STRONG -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            node.children.trimSurrounding(MarkdownTokenTypes.EMPH, 2).forEach { appendMarkdownInline(it, content, colors) }
        }

        node.type == GFMElementTypes.STRIKETHROUGH -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
            node.children.trimSurrounding(GFMTokenTypes.TILDE, 2).forEach { appendMarkdownInline(it, content, colors) }
        }

        node.type == MarkdownElementTypes.INLINE_LINK -> {
            val linkDest = node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content) ?: ""
            val linkText = node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_TEXT)?.getTextInNode(content)
                ?.trim { it == '[' || it == ']' } ?: linkDest
            withLink(LinkAnnotation.Url(linkDest)) {
                withStyle(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)) {
                    append(linkText)
                }
            }
        }

        node.type == MarkdownElementTypes.AUTOLINK -> {
            node.children.trimSurrounding(MarkdownTokenTypes.LT, 1).trimSurrounding(MarkdownTokenTypes.GT, 1).forEach { l ->
                val link = l.getTextInNode(content)
                withLink(LinkAnnotation.Url(link)) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(link) }
                }
            }
        }

        node.type == MarkdownElementTypes.CODE_SPAN -> {
            val code = node.getTextInNode(content).trim('`')
            withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 0.9.em, color = colors.primary),
            ) {
                append(' ')
                append(code)
                append(' ')
            }
        }

        node.type == GFMElementTypes.INLINE_MATH -> {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 0.95.em)) {
                append(node.getTextInNode(content))
            }
        }

        node.type == GFMTokenTypes.GFM_AUTOLINK -> {
            val link = node.getTextInNode(content)
            withLink(LinkAnnotation.Url(link)) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(link) }
            }
        }

        else -> node.children.forEach { appendMarkdownInline(it, content, colors) }
    }
}

// ---- 工具 ----

private fun ASTNode.getTextInNode(text: String): String = text.substring(startOffset, endOffset)

private fun ASTNode.findChildOfTypeRecursive(vararg types: IElementType): ASTNode? {
    if (this.type in types) return this
    for (child in children) {
        val result = child.findChildOfTypeRecursive(*types)
        if (result != null) return result
    }
    return null
}

private fun List<ASTNode>.trimSurrounding(type: IElementType, size: Int): List<ASTNode> {
    if (isEmpty() || size <= 0) return this
    var start = 0
    var end = this.size
    var trimmed = 0
    while (start < end && trimmed < size && this[start].type == type) { start++; trimmed++ }
    trimmed = 0
    while (end > start && trimmed < size && this[end - 1].type == type) { end--; trimmed++ }
    return this.subList(start, end)
}
