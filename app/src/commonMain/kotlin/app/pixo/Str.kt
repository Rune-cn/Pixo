package app.pixo

import androidx.compose.runtime.Composable

/**
 * 跨平台字符串访问。
 * Android 实际实现从 resources.arsc（R.string）读取，桌面端从 Compose 资源读取，
 * 保证 Android 上全部文案随系统/应用语言通过标准资源机制切换。
 */
internal expect object Str {
    @Composable fun nav_home(): String
    @Composable fun nav_settings(): String
    @Composable fun theme_follow(): String
    @Composable fun theme_dark(): String
    @Composable fun theme_light(): String
    @Composable fun language_follow(): String
    @Composable fun language_zh(): String
    @Composable fun language_en(): String
    @Composable fun settings_login(): String
    @Composable fun settings_title(): String
    @Composable fun settings_general(): String
    @Composable fun settings_appearance(): String
    @Composable fun cookie_label(): String
    @Composable fun settings_language(): String
    @Composable fun settings_theme(): String
    @Composable fun theme_mode(): String
    @Composable fun predictive_back(): String
    @Composable fun dynamic_color(): String
    @Composable fun dynamic_color_summary(): String
    @Composable fun floating_bar(): String
    @Composable fun floating_bar_summary(): String
    @Composable fun liquid_glass(): String
    @Composable fun liquid_glass_summary(): String
    @Composable fun export_login_info(): String
    @Composable fun export_login_summary(): String
    @Composable fun export_copied(): String
    @Composable fun export_login_dialog_title(): String
    @Composable fun predictive_back_summary(): String
    @Composable fun settings_debug(): String
    @Composable fun debug_log(): String
    @Composable fun debug_log_summary(): String
    @Composable fun view_logs(): String
    @Composable fun refresh_pull(): String
    @Composable fun refresh_release(): String
    @Composable fun refresh_loading(): String
    @Composable fun refresh_done(): String
    @Composable fun load_more(): String
    @Composable fun no_more(): String
    @Composable fun home_title(): String
    @Composable fun need_cookie(): String
    @Composable fun need_cookie_hint(): String
    @Composable fun go_settings(): String
    @Composable fun daily(): String
    @Composable fun weekly(): String
    @Composable fun monthly(): String
    @Composable fun search(): String
    @Composable fun bookmark_public_label(): String
    @Composable fun bookmark_private_label(): String
    @Composable fun novel_ranking(): String
    @Composable fun illust_ranking(): String
    @Composable fun ranking(): String
    @Composable fun illust(): String
    @Composable fun novel(): String
    @Composable fun search_pixiv(): String
    @Composable fun discover(): String
    @Composable fun recommended(): String
    @Composable fun search_hint(): String
    @Composable fun comments(): String
    @Composable fun author_prefix(): String
    @Composable fun anonymous(): String
    @Composable fun no_comments(): String
    @Composable fun no_works(): String
    @Composable fun close_comment(): String
    @Composable fun novel_detail(): String
    @Composable fun bookmark_public(): String
    @Composable fun bookmark_private(): String
    @Composable fun retry(): String
    @Composable fun copied(tag: String): String
    @Composable fun load_failed(message: String): String
    @Composable fun bookmarked_public(): String
    @Composable fun bookmarked_private(): String
    @Composable fun token_login(): String
    @Composable fun token_login_hint(): String
    @Composable fun refresh_token_label(): String
    @Composable fun login_button(): String
    @Composable fun logout_button(): String
    @Composable fun cancel(): String
    @Composable fun login_status(user: String): String
    @Composable fun not_logged_in(): String
    @Composable fun clear_logs(): String
    @Composable fun comment_hint(): String
    @Composable fun comment_send(): String
    @Composable fun comment_posted(): String
    @Composable fun comment_post_failed(message: String): String
    @Composable fun unbookmark(): String
    @Composable fun bookmark_removed(): String
    @Composable fun works_title(): String
    @Composable fun author_page(): String
    @Composable fun user_stats(illusts: Int, novels: Int): String
    @Composable fun avatar_desc(): String
    @Composable fun following(): String
    @Composable fun follower(): String
    @Composable fun empty_following(): String
    @Composable fun empty_follower(): String
    @Composable fun web_login(): String
    @Composable fun web_login_hint(): String
    @Composable fun web_login_success(): String
    @Composable fun web_login_open_browser(): String
    @Composable fun open_in_browser(): String
    @Composable fun more_actions(): String
    @Composable fun animated_badge(): String
    @Composable fun web_login_browser_hint(): String
    @Composable fun web_login_detected(): String
    @Composable fun web_login_confirm(): String
    @Composable fun cookie_paste_hint(): String
    @Composable fun cookie_save(): String
    @Composable fun cookie_saved(): String
    @Composable fun crash_log(): String
    @Composable fun clear_crash_log(): String
    @Composable fun crash_none(): String
    @Composable fun copy_all(): String
    @Composable fun copy_select(): String
    @Composable fun copy_title(): String
    @Composable fun manga(): String
    @Composable fun replies(): String
    @Composable fun view_replies(): String
    @Composable fun view_all_replies(): String
    @Composable fun collapse_replies(): String
    @Composable fun load_more_replies(): String
    @Composable fun no_replies(): String
    @Composable fun reply_action(): String
    @Composable fun copy_account(): String
    @Composable fun copy_user_id(): String
    @Composable fun user_id_label(): String
    @Composable fun share(): String
    @Composable fun download_original(): String
    @Composable fun download(): String
    @Composable fun select_all(): String
    @Composable fun download_folder(): String
    @Composable fun download_folder_summary(): String
    @Composable fun choose_folder(): String
    @Composable fun download_done(count: Int): String
    @Composable fun download_failed(): String
    @Composable fun download_selected_hint(): String
    @Composable fun user(): String
}
