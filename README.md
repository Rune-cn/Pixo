# Pixo

Pixiv 第三方客户端，基于 Compose Multiplatform + Miuix（MIUI/HyperOS 设计语言），一套 UI 代码同时支持 Android 与桌面端。

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> 非官方项目，与 Pixiv 无关。所有作品版权归原作者所有。

## 功能特性

### 浏览

- [x] 首页推荐流
- [x] 排行榜：插画 / 漫画 / 小说（日榜 / 周榜 / 月榜）
- [x] 发现页、推荐页
- [x] 搜索：插画 / 小说 / 用户三标签，用户支持昵称、账号与数字 ID
- [x] 公开 / 私密收藏合并入口，页内 Tab 切换

### 作品

- [x] 插画详情：多页浏览、原图灯箱（双指缩放 / 拖动 / 双击放大还原）
- [x] ugoira 动图播放（zip 解帧轮播，可开启「播放需确认」避免闪烁）
- [x] 列表角标：R-18 标记、多图页数（`×4`）
- [x] 漫画作品浏览（详情 / 排行 / 作者主页分类）
- [x] 小说详情

### 互动

- [x] 评论区：一级评论 + 追评卡片（深色嵌入、查看 / 回复、长按卡片复制文字）
- [x] 收藏（公开 / 私密）
- [x] 关注 / 取消关注（公开 / 私密两种）
- [x] 作者主页：资料卡、作品分类 Tab、头像放大预览、下拉刷新
- [x] 关注列表公开 / 私密 Tab、粉丝列表

### 工具

- [x] 深链：`pixiv://artworks/{id}`、`pixiv://novel/{id}`、`pixiv://user/{id}` 及对应网页链接
- [x] 详情页 / 个人主页「⋮」菜单：浏览器打开、分享、下载原图（多图选择 + 全选 / 取消全选）
- [x] 原图下载：默认 `Pictures/Pixo`，可自选目录（SAF），失败自动回退应用目录
- [x] 「下载」页：浏览已下载图片（读取所选下载目录）
- [x] 设置：「外观 / 数据 / 调试」；数据页支持偏好设置导出 / 导入、清除缓存、下载目录
- [x] 「关于」页：开源地址、隐私政策、许可证、免责声明、崩溃日志
- [x] 界面语言：简体中文 / English；主题：深色 / 浅色 / 跟随系统 / 动态取色
- [x] 悬浮底栏（液态玻璃）与开关动画

## 技术栈

- Kotlin / Compose Multiplatform（commonMain 共享 UI，Android + Desktop）
- UI：`top.yukonga.miuix.kmp:miuix-*:0.9.4`
- 网络：OkHttp 5.5 + Coil 3；灯箱缩放：`me.saket.telephoto:zoomable`
- 双通道架构：App API（Bearer Token）为主，网页 AJAX（Cookie + CSRF）兜底

## 目录结构

```
app/src/commonMain/kotlin/app/pixo/
├── App.kt              应用骨架、路由、设置 / 下载 / 关于页
├── Screens.kt          首页、榜单、搜索、详情、灯箱、评论、作者主页等
├── Ui.kt               页面框架、毛玻璃顶栏、悬浮底栏
├── PixivApi.kt         统一 API 门面（按登录通道选路）
├── AppPixivApi.kt      App API（app-api.pixiv.net）
├── WebPixivApi.kt      网页通道（www.pixiv.net/ajax）
├── PixivAuth.kt        OAuth 登录与 Token 刷新
├── PixivHttp.kt        OkHttp 封装、时间戳签名
├── Platform.kt         平台 expect 声明
└── Str.kt              跨平台文案（Android res / Compose 资源）
```

## 构建

环境要求：JDK 17+。

```bash
.\gradlew.bat :app:assembleRelease        # Android 正式包（app/build/outputs/apk/release/）
.\gradlew.bat :app:assembleDebug          # Debug 包
.\gradlew.bat :app:compileDebugKotlinAndroid  # 快速编译检查
```

### 签名

`keystore.properties` 与密钥库文件均不入库（见 `.gitignore`）。新仓库首次构建请自行生成：

```bash
keytool -genkeypair -v -keystore pixo-keystore.jks -alias Pixo \
  -keyalg RSA -keysize 2048 -validity 36500 -dname "CN=Pixo,O=Pixo"
```

然后在工作区根目录创建 `keystore.properties`：

```properties
storeFile=../pixo-keystore.jks
storePassword=<你的密码>
keyAlias=Pixo
keyPassword=<你的密码>
```

## 登录方式（设置 → 登录）

1. **Token 登录**：粘贴 Pixiv OAuth Refresh Token，应用自动换取并刷新 Access Token。
2. **网页登录**：Android 内嵌登录页，成功后保存会话 Cookie 与用户 ID。
3. **Cookie 粘贴**：桌面端或内嵌页不可用时，浏览器登录后粘贴 Cookie。

## 逆向资料

接口逆向结论（端点、鉴权、分页、反爬与实测记录）见 [pixiv-reverse-engineering](https://github.com/Rune-cn/pixiv-reverse-engineering)。

## 许可证

[MIT](LICENSE)
