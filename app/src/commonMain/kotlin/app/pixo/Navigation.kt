package app.pixo

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

@Serializable
internal object Home


@Serializable
internal object AppSettings


@Serializable
internal object AppearanceSettings


@Serializable
internal object ContentSettings


@Serializable
internal object TagBlockSettings

@Serializable
internal object AuthorBlockSettings


@Serializable
internal object DataSettings


@Serializable
internal object Downloads


@Serializable
internal object History


@Serializable
internal object Follow


@Serializable
internal object About


@Serializable
internal object DebugSettings


@Serializable
internal object LogViewer


@Serializable
internal object CrashLog


@Serializable
internal object WebLogin


@Serializable
internal data class User(val userId: Long)


@Serializable
internal data class Follows(val userId: Long, val kind: String)


@Serializable
internal data class Ranking(val mode: String, val content: String = "illust")


@Serializable
internal data class Search(val query: String = "")


@Serializable
internal data class NovelRanking(val mode: String)


@Serializable
internal data class Bookmarks(val restrict: String)


@Serializable
internal data class Detail(val workId: Long, val kind: String)


@Serializable
internal data class Comments(val workId: Long, val kind: String)


/** 深链目标：由 `pixiv://artworks/{id}`、`pixiv://user/{id}` 或对应网页链接解析而来。 */
data class PixivDeepLink(val workId: Long = 0, val kind: String = "", val userId: Long = 0)


@Serializable
internal object Discover


@Serializable
internal object Recommended


/**
 * 子页面统一过渡：miuix 的 push/pop（500ms 弹簧曲线）。
 *
 * 25 个路由原本各自重复 4 行过渡参数，改一处动画要改 25 处；
 * 现在只有这里一处定义。
 */
internal inline fun <reified T : Any> NavGraphBuilder.pixoScreen(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable<T>(
    enterTransition = { miuixPushEnter() },
    exitTransition = { miuixPushExit() },
    popEnterTransition = { miuixPopEnter() },
    popExitTransition = { miuixPopExit() },
    content = content,
)


/** 标签根页的淡入淡出过渡（与子页面的左右滑动区分）。 */
internal inline fun <reified T : Any> NavGraphBuilder.pixoScreenFade(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable<T>(
    enterTransition = { miuixFadeEnter() },
    exitTransition = { miuixFadeExit() },
    popEnterTransition = { miuixFadeEnter() },
    popExitTransition = { miuixFadeExit() },
    content = content,
)


internal val themeModes = listOf(ColorSchemeMode.System, ColorSchemeMode.Dark, ColorSchemeMode.Light)


/**
 * 从偏好恢复主题模式（存的是 [ColorSchemeMode.name]，未知值回退「跟随系统」）。
 *
 * 之前只有 `rememberSaveable`，进程重启后主题会静默退回 System —— 现在与其它外观项一致持久化。
 */
internal fun savedThemeMode(): ColorSchemeMode =
    themeModes.firstOrNull { it.name == AppPrefs.themeMode } ?: ColorSchemeMode.System
