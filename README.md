# Pixo

Pixiv 第三方客户端，基于 Compose Multiplatform + Miuix（MIUI/HyperOS 设计语言），一套 UI 代码同时支持 **Android** 与 **桌面端**。

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Desktop-4ea1e8)
[![Download](https://img.shields.io/github/v/release/Rune-cn/Pixo?label=Latest%20Release)](https://github.com/Rune-cn/Pixo/releases/latest)

> 非官方项目，与 Pixiv 无关。所有作品版权归原作者所有。

## 📥 下载

进入 [Releases](https://github.com/Rune-cn/Pixo/releases/latest) 下载最新版：

- **`pixo-vX.Y.Z-release.apk`**：签名正式包，可直接覆盖安装
- **`pixo-vX.Y.Z-debug.apk`**：调试包（约 40MB，含调试信息）

> Release 由 GitHub Actions 在每次推送 main 时**自动构建并发布**，只保留当前最新版本（旧版本自动清理）。

## 功能特性

### 浏览

- [x] 首页推荐流、**关注新作**（已关注画师最新作品，分页加载）
- [x] 排行榜：插画 / 漫画 / 小说（日榜 / 周榜 / 月榜）
- [x] 发现页、推荐页
- [x] 搜索：插画 / 小说 / 用户三标签，用户支持昵称、账号与数字 ID
- [x] 公开 / 私密收藏合并入口，页内 Tab 切换
- [x] **浏览历史**：本地记录看过的插画 / 小说，从设置页进入，点击直达详情

### 作品

- [x] 插画详情：多页浏览、原图灯箱（双指缩放 / 拖动 / 双击放大还原）
- [x] **相关作品**：详情页标签下方横向推荐，可继续跳转详情
- [x] ugoira 动图播放（zip 解帧轮播，可开启「播放需确认」避免闪烁）
- [x] 列表角标：R-18 标记、多图页数（`×4`）
- [x] 漫画作品浏览（详情 / 排行 / 作者主页分类）
- [x] 小说详情

### 互动

- [x] 评论区：一级评论 + 追评（展开状态保持，滚动不收起）；**评论表情章**完整显示
- [x] 收藏（公开 / 私密）
- [x] 关注 / 取消关注（公开 / 私密两种）
- [x] 作者主页：资料卡、作品分类 Tab、头像放大预览、下拉刷新（返回列表不重复加载）
- [x] 关注列表公开 / 私密 Tab、粉丝列表

### 内容过滤与标签

- [x] **标签屏蔽**：设置页「屏蔽」分区，正则规则按行配置，命中作品任一标签即从列表过滤
- [x] **标签官方翻译**：详情页标签旁显示 Pixiv 官方译名（`translation.en`）
- [x] **长按标签**：弹出「复制 / 屏蔽」选择菜单，屏蔽自动加入黑名单

### 工具

- [x] 深链：`pixiv://artworks/{id}`、`pixiv://novel/{id}`、`pixiv://user/{id}` 及对应网页链接
- [x] 「⋮」菜单：浏览器打开（系统浏览器，按 Android 规范）、分享、下载原图（多图选择 + 全选 / 取消全选）
- [x] 原图下载：默认 `Pictures/Pixo`，**可自选目录**（SAF，目录显示为可读路径）
- [x] 「下载」页：浏览已下载图片（读取所选下载目录）
- [x] 设置：「外观 / 内容 / 数据 / 调试」；数据页支持偏好导出 / 导入、**WebDAV 云备份 / 恢复**、清除缓存、下载目录
- [x] 「关于」页：开源地址、隐私政策、许可证、免责声明
- [x] 界面语言：简体中文 / English；主题：深色 / 浅色 / 跟随系统 / **动态取色**（Material You）
- [x] 悬浮底栏（液态玻璃）与开关动画、顶栏大字标题随滚动收缩（Miuix 规范）

## 技术栈

- Kotlin / Compose Multiplatform（commonMain 共享 UI，Android + Desktop）
- UI：`top.yukonga.miuix.kmp:miuix-*:0.9.4`
- 网络：OkHttp 5.5 + Coil 3；灯箱缩放：`me.saket.telephoto:zoomable`
- 双通道架构：App API（Bearer Token）为主，网页 AJAX（Cookie + CSRF）兜底
- 评论 / 追评 / 相关作品 / 关注新作优先网页通道（App API 对部分端点不可靠）

## 目录结构

```
app/src/commonMain/kotlin/app/pixo/
├── App.kt              应用骨架、路由、设置 / 下载 / 关于 / 历史页
├── Screens.kt          首页、榜单、搜索、详情、灯箱、评论、作者主页、关注新作等
├── Ui.kt               页面框架、毛玻璃顶栏、悬浮底栏
├── FloatingBottomBar.kt / LiquidGlass.kt  悬浮底栏与液态玻璃材质
├── MarkdownBlock.kt    Markdown 文档渲染
├── WebDavApi.kt        WebDAV 设置备份（Basic 认证）
├── PixivApi.kt         统一 API 门面（按登录通道选路）
├── AppPixivApi.kt      App API（app-api.pixiv.net）
├── WebPixivApi.kt      网页通道（www.pixiv.net/ajax）
├── PixivAuth.kt        OAuth 登录与 Token 刷新
├── PixivHttp.kt        OkHttp 封装、时间戳签名
├── Platform.kt         平台 expect 声明
└── Str.kt              跨平台文案（Android res / Compose 资源）
```

## 构建与发布

**CI 自动发布**（推荐）：推送 `main` 后 .github/workflows/build.yml 自动：
1. `assembleDebug` / 编译闸门 / `assembleRelease`（R8 + 签名，密钥走仓库 Secrets）
2. 构建成功自动创建/覆盖 GitHub Release（`v<versionName>`），上传 release + debug 两个 APK
3. 自动清理历史版本，只保留当前

**本地构建**：环境要求 JDK 17+。

```bash
./gradlew :app:assembleRelease        # Android 正式包（app/build/outputs/apk/release/）
./gradlew :app:assembleDebug          # Debug 包
./gradlew :app:compileDebugKotlinAndroid  # 快速编译检查
```

### 签名（本地 / 新仓库）

`keystore.properties` 与密钥库文件均不入库（见 `.gitignore`）：

```bash
keytool -genkeypair -v -keystore pixo-keystore.jks -alias Pixo \
  -keyalg RSA -keysize 2048 -validity 36500 -dname "CN=Pixo,O=Pixo"
```

```properties
storeFile=../pixo-keystore.jks
storePassword=<你的密码>
keyAlias=Pixo
keyPassword=<你的密码>
```

CI 签名配置（GitHub Secrets）：`PIXO_KEYSTORE_BASE64` / `PIXO_KEYSTORE_PASSWORD` / `PIXO_KEY_ALIAS` / `PIXO_KEY_PASSWORD`。

## 登录方式（设置 → 登录）

**网页登录**：Android 内嵌登录页，登录成功后自动保存会话 Cookie 与用户 ID，即可浏览 / 评论 / 收藏 / 关注等全部功能。

## 许可证

[GNU GPL v3](LICENSE)
