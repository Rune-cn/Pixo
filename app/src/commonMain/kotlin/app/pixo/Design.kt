package app.pixo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全项目共享的排版尺度与页面骨架。
 *
 * 这些值此前散落在 6 个设置页里各写一遍（12dp 卡片边距、20/6dp 标题边距、
 * LazyColumn + nestedScroll 三件套），改一处节奏要改六处。
 */

/** 设置分组卡片的外边距（与 [SectionTitle] 的 20dp 起始对齐）。 */
private val GroupHorizontalMargin = 12.dp
private val GroupBottomMargin = 12.dp

/** [SectionTitle] 的内边距：沿用项目既有节奏（20/6dp），不改视觉。 */
internal val SectionTitleMargin = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp)

/**
 * 设置分区标题。
 *
 * 用 Miuix 的 [SmallTitle]（`subtitle` 字阶 + `onBackgroundVariant`），
 * 不再自造字号/字重 —— 0.9.4 没有公开的 SectionHeader 组件。
 */
@Composable
internal fun SectionTitle(text: String) {
    SmallTitle(text = text, insideMargin = SectionTitleMargin)
}

/**
 * 设置分组：一个 Card 里放若干 Preference 行。
 *
 * Card 自身不插入行间距与分隔线（`InsideMargin = 0.dp`），
 * 行内边距由各 Preference 组件负责。
 */
@Composable
internal fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = GroupHorizontalMargin)
            .padding(bottom = GroupBottomMargin),
        content = content,
    )
}

/**
 * 设置类子页面骨架：毛玻璃顶栏（大字标题随滚动收缩居中）+ 可滚动内容。
 *
 * 内容必须是滚动容器并挂上 `scrollBehavior.nestedScrollConnection`，
 * 否则顶栏收不到滚动事件，大字标题永远停在左上角不居中
 * —— 「标签屏蔽 / 屏蔽人」两个页面此前就是这样，和别的子页面观感不一致。
 */
@Composable
internal fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    PixoSubScreen(
        title = title,
        onBack = onBack,
        scrollBehavior = scrollBehavior,
        actions = actions,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
            verticalArrangement = verticalArrangement,
            overscrollEffect = null,
            content = content,
        )
    }
}

/** 规则编辑页底部的保存按钮（标签屏蔽 / 屏蔽人共用）。 */
@Composable
internal fun RuleSaveButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GroupHorizontalMargin)
            .padding(top = 4.dp, bottom = 16.dp),
    ) { Text(label) }
}

/**
 * 多行规则编辑器（标签屏蔽 / 屏蔽人共用）。
 *
 * @param hint 说明文字（放在卡片上方，解释规则写法）
 * @param value 当前文本，受控
 * @param minHeight 编辑区最小高度，保证空内容时也有足够的输入区域
 */
@Composable
internal fun RuleEditor(
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    minHeight: Dp,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = GroupHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = hint,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Card(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MiuixTheme.textStyles.body2.copy(color = MiuixTheme.colorScheme.onSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .padding(16.dp),
            )
        }
    }
}
