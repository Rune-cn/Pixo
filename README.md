# Pixo — Pixiv 客户端（Compose Multiplatform + Miuix）

Pixo 是一个面向 Android 的 Pixiv 客户端，采用 Compose Multiplatform 编写，UI 使用 **Miuix**（MIUI/HyperOS 设计语言）实现液态玻璃悬浮底栏、毛玻璃顶栏、MIUI 卡片与动效。

- 语言：Kotlin / Compose Multiplatform（commonMain 一套 UI 代码，Android + Desktop 双端）
- UI 库：`top.yukonga.miuix.kmp:miuix-*:0.9.4`（锁定版本）
- 网络：OkHttp 5.5 + Coil 3（图片加载）
- 参考实现：InstallerX-Revived、legado-with-MD3（本地 `../InstallerX-Revived`、`../legado-with-MD3`）

## 功能

- 首页推荐 / 排行榜（插画、漫画、小说）/ 发现 / 搜索
- 作品详情：插画分页与原图灯箱（**支持双指捏合缩放、缩放后拖动、双击放大/还原**）
- 收藏：公开 / 私密收藏、收藏列表分页（`max_bookmark_id` 游标）
- 评论：一级评论列表、**追评查看与回复**、长按卡片复制文字
- 作者主页：资料卡、关注 / 粉丝列表、作品按【插画 / 漫画 / 小说】分类统计、作品列表分页
- 用户 ID 长按复制：主页 `@账号` 长按 → 复制 @账号 / 复制数字用户 ID / 选择复制
- 液态玻璃悬浮底栏、动态取色（壁纸）、深色 / 浅色 / 跟随系统、预见式返回、中英双语

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
- 评论/追评写操作：App API `POST /v1/{kind}/comment/add`（回复加 `parent_comment_id`）；
  网页 `POST /rpc/post_comment.php`。
- 评论列表无 Cookie 时匿名请求返回 400，App 内会提示先去登录。

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

- 新增文案要同步 **4 处**：`Str.kt` expect + `Str.android.kt` + `Str.desktop.kt` + 资源
  （`androidMain/res/values[-zh]` 与 `commonMain/composeResources/values[-zh]`）。
- UI 组件优先用 Miuix（Card / Button / TextField / OverlayDialog / PullToRefresh ...），
  需要更多 Miuix 用法看本机 skill：`.codex/skills/miuix-design/`。
- 分页缓存统一走 `FeedCache`（内存级），详情/评论避免重复请求。
- Miuix 是实验性库，升级版本前先在参考仓库验证。

## 已知限制

- 追评的「回复」在 Token 通道已验证；网页（Cookie）通道的 `parent_comment_id` 按旧 RPC 约定实现，未在线上实测。
- 粉丝数依赖网页会话 Cookie；纯 Token 模式显示「—」（粉丝列表本身可用）。
- 网页端作者作品分页依赖 `profile/all` 全量 ID（作品多时首屏较慢）。
- 桌面端登录用浏览器 + 粘贴 Cookie 流程。
