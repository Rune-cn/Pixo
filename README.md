# Pixo — Pixiv 客户端（Compose Multiplatform + Miuix）

Pixo 是一个面向 Android 的 Pixiv 客户端，采用 Compose Multiplatform 编写，UI 使用 **Miuix**（MIUI/HyperOS 设计语言）实现液态玻璃悬浮底栏、毛玻璃顶栏、MIUI 卡片与动效。

- 语言：Kotlin / Compose Multiplatform（commonMain 一套 UI 代码，Android + Desktop 双端）
- UI 库：`top.yukonga.miuix.kmp:miuix-*:0.9.4`（锁定版本）
- 网络：OkHttp 5.5 + Coil 3（图片加载）+ `me.saket.telephoto:zoomable`（灯箱缩放）
- 参考实现：InstallerX-Revived、legado-with-MD3（本地 `../InstallerX-Revived`、`../legado-with-MD3`）

## 功能

- 首页推荐 / 排行榜（**插画、漫画、小说**三个入口）/ 发现 / 搜索（**插画 / 小说 / 用户**三标签，用户支持昵称、账号与数字 ID）
- 作品详情：插画分页与原图灯箱（**双指捏合缩放、缩放后拖动、双击放大/还原**，由 telephoto zoomable 提供）
- ugoira 动图（Pixiv 的 “GIF”）：详情页与灯箱播放，列表缩略图带「动图」角标（下载 zip → 解压帧 → 按帧延时轮播）；
 可在「外观」设置开启**动图播放需确认**（默认开启，先显示静态图与播放按钮，避免自动轮播闪烁）
- 详情页顶栏「⋮」菜单：在浏览器中打开原作品页、**分享**、**下载原图**（多图弹选择对话框，带「全选」，按 `{id}_p{n}.jpg` 命名存入下载目录）
- 下载选择对话框的「全选」按钮在全选后变为「取消全选」
- 个人主页顶栏「⋮」菜单：在浏览器中打开、分享
- 详情页作者行以卡片展示，点击进入作者主页
- 深链：`pixiv://artworks/{id}`、`pixiv://novel/{id}`、`pixiv://user/{id}` 及对应 `https://www.pixiv.net/...` 链接直达作品详情 / 用户主页
- 收藏：公开 / 私密收藏合并为单个「收藏」入口，页内 Tab 切换（`max_bookmark_id` 游标分页）；收藏结果以底部提示条反馈
- 评论：一级评论列表（下拉刷新）、**追评查看与回复**（追评卡片嵌在一级卡片内、回复按钮下方，深色区分）、回复按钮唤起输入框、长按卡片复制文字
- 作者主页（下拉刷新）：资料卡（**关注统计**，官方无粉丝数字段故已移除）、**关注 / 取消关注（公开 / 私密两种分级）**、
  作品按【插画 / 漫画 / 小说】分类统计并可切换 Tab 浏览、作品列表分页、**头像点击放大预览**
- 列表预览角标：插画 / 小说卡片显示 **R-18** 标记，多图插画右下角显示页数（如 `×4`）
- 用户 ID 长按复制：主页 `@账号` 长按 → 复制 @账号 / 复制数字用户 ID / 选择复制
- 设置：「外观 / **数据** / 调试」分类；**数据页**含**导出 / 导入设置**（JSON 备份）、**下载目录**（Android 用系统目录选择器 SAF，桌面用文件夹选择）、**清除缓存**；外观页含悬浮底栏开关切换**淡入淡出 + 底部滑入滑出动画**、动态取色（壁纸）、深色 / 浅色 / 跟随系统、预见式返回、中英双语
- 权限：App 启动时动态申请存储 / 读取权限（API 33+ 为媒体读取）

## 目录结构

```
app/src/commonMain/kotlin/app/pixo/
├── App.kt              应用骨架：顶栏、路由、底部悬浮栏、首页/账号宿主页
├── Screens.kt          全部页面：榜单/发现/搜索/详情/灯箱/评论区/作者主页/关注粉丝页
├── LiquidGlass.kt      液态玻璃（AGSL lens/vibrancy/InnerShadow 三件套）
├── Ui.kt               页面框架、毛玻璃顶栏、悬浮底栏、AI 平台适配
├── Models.kt           数据模型（PixivWork / PixivUser / PixivComment ...）
├── PixivApi.kt         统一门面：按登录通道自动选路
├── AppPixivApi.kt      App API（app-api.pixiv.net，Bearer Token）
├── WebPixivApi.kt      网页通道（www.pixiv.net/ajax，Cookie + CSRF）
├── PixivAuth.kt        OAuth（refresh_token / PKCE 登录、token 刷新）
├── PixivHttp.kt        OkHttp 封装、请求头、时间戳签名、Json 辅助
├── Platform.kt         平台 expect（SystemBackHandler 等）
└── Str.kt              跨平台字符串（Android 走 res，桌面走 Compose 资源）
```

## 双通道架构

| 通道 | 鉴权 | 适合 | 覆盖范围 |
|---|---|---|---|
| App API | `Authorization: Bearer` + 时间戳签名 | 登录后（推荐、收藏、评论写操作、列表分页） | `/v1/*`、`/v2/*` |
| 网页 ajax | 会话 Cookie + `x-csrf-token` | 评论列表、追评、网页登录会话 | `/ajax/*`、RPC |

选路规则集中在 `PixivApi.kt`：配置了 Refresh Token 即走 App API，缺失时回退网页通道。

关键接口（实测结论，详见 `../pixiv-reverse-engineering`）：

- 作品统计：App API `/v1/user/detail` 的 `profile.total_illusts / total_manga / total_novels`；
  网页端 `/ajax/user/{id}/profile/all`（`illusts` 含漫画，`manga` 为子集，插画数 = illusts − manga）。
- 粉丝数：App API **没有**该字段；网页端 `/ajax/user/{id}/followers`（**复数**，单数路径恒 404）自带 `total`，
  需要会话 Cookie。仅配 Token 未配 Cookie 时粉丝数显示「—」。
- 评论列表：`GET /ajax/{kind}s/comments/roots?{kind}_id=&offset=&limit=`（无 App API 端点）。
- 追评：App API `/v1/{kind}/comment/replies?{kind}_id=&comment_id=`（有游标）；
  网页 `/ajax/{kind}s/comments/replies?comment_id=&page=`（page 从 1 开始）。
- 关注：App API `POST /v1/user/follow/add`（`restrict=public|private`）、`POST /v1/user/follow/delete`；
  网页 `POST /bookmark_add.php`（`mode=add&type=user&user_id=&tag=&restrict=0|1&format=json`，需 CSRF）、
  取消关注 `POST /rpc_group_setting.php`（`mode=del&type=bookuser&id=`）。
- 用户搜索：`GET /ajax/search/users?nick={词}&p={页}&i=0`（Referer 必须为 `/search/users`，**不能带 `s_mode`**，每页 10 条）；
  纯数字 ID 引擎查不到时回退 `/ajax/user/{id}/...` 直达抓取。
- 评论/追评写操作：App API `POST /v1/{kind}/comment/add`（回复加 `parent_comment_id`）；
  网页 `POST /rpc/post_comment.php`。
- 评论列表无 Cookie 时匿名请求返回 400，App 内会提示先去登录。
- 漫画排行：`GET /ranking.php?mode=daily&content=manga&format=json`（与插画同结构，`illust_type=1`）。
- 作者漫画分页：App API `/v1/user/illusts?type=manga`；网页端从 `works/latest` 按 `illustType==1` 过滤。

## 下载与存储

- 下载原图默认写入系统媒体库 `Pictures/Pixo`（API 29+ 无需存储权限）；写失败时自动回退到应用专属外部目录，保证不丢文件。
- 设置里可自选下载目录（SAF）；若持久化权限失效会自动回退媒体库 / 应用目录并记录日志（应用内「崩溃与日志」可查）。
- 多图作品顶栏下载时弹选择对话框，默认全选；批量下载只统计成功数。
- Android「在浏览器中打开」会先解析 Intent，避开应用自身注册的 pixiv 网页深链，直接交给系统浏览器，不会再次打开自己。

## 登录方式（设置 → 登录）

1. **Token 登录**：粘贴 Pixiv OAuth Refresh Token，App 自动换取 Access Token 并刷新。
2. **网页登录**：内嵌登录页（Android），成功后会保存会话 Cookie 与用户 ID。
3. **Cookie 粘贴**：桌面端或内嵌页不可用时，浏览器登录后粘贴 Cookie（`PHPSESSID=...`）。

> 调试登录信息（本机开发用）保存在工作区根目录 `../pixo-debug-login.json`，**不入库、不提交**。
> 仓库内 `keystore.properties` 也不得提交真实密码。

## 构建

```bash
# 需要：JDK 17+，首次构建自动下载 Gradle
.\gradlew.bat :app:assembleRelease       # Android 正式包（输出 app/build/outputs/apk/release/）
.\gradlew.bat :app:assembleDebug         # Debug 包
.\gradlew.bat :app:compileDebugKotlinAndroid   # 快速编译检查
```

签名：`app/build.gradle.kts` 的 release 签名读取 `keystore.properties`（引用 `local-keystore.jks`，均不入库）。

## 开发约定

- 新增文案要同步 **8 处**：`Str.kt` expect + `Str.android.kt` + `Str.desktop.kt` + 资源
  （`androidMain/res/values`、`values-en`、`values-zh` 与 `commonMain/composeResources/values`、`values-zh`）。
- UI 组件优先用 Miuix（Card / Button / TextField / OverlayDialog / PullToRefresh ...），
  需要更多 Miuix 用法看本机 skill：`.codex/skills/miuix-design/`。
- 分页缓存统一走 `FeedCache`（内存级），详情/评论避免重复请求。
- Miuix 是实验性库，升级版本前先在参考仓库验证。

## 已知限制

- 追评的「回复」在 Token 通道已验证；网页（Cookie）通道的 `parent_comment_id` 按旧 RPC 约定实现，未在线上实测。
- 粉丝数依赖网页会话 Cookie；纯 Token 模式显示「—」（粉丝列表本身可用）。
- 网页端作者作品分页依赖 `profile/all` 全量 ID（作品多时首屏较慢）。
- 桌面端登录用浏览器 + 粘贴 Cookie 流程。
