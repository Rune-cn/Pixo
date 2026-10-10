# miuix 无内置混淆规则：运行时依赖反射/元数据的包必须全量保留（否则 R8 裁剪导致闪退）。
# icons 只按静态引用保留，交给 R8 裁掉未用图标，显著降体积。
-keep class top.yukonga.miuix.kmp.basic.** { *; }
-keep class top.yukonga.miuix.kmp.blur.** { *; }
-keep class top.yukonga.miuix.kmp.preference.** { *; }
-keep class top.yukonga.miuix.kmp.theme.** { *; }
-keep class top.yukonga.miuix.kmp.overlay.** { *; }
-keep class top.yukonga.miuix.kmp.utils.** { *; }
-keep class top.yukonga.miuix.kmp.shader.** { *; }
-keep class top.yukonga.miuix.kmp.squircle.** { *; }
-keep class top.yukonga.miuix.kmp.animation.** { *; }

# mikepenz Markdown 渲染器 + JetBrains Markdown 解析器：依赖反射/ServiceLoader，
# R8 混淆/裁剪会破坏解析导致渲染失败（显示原始 Markdown 源码）。
-keep class com.mikepenz.** { *; }
-keep class org.intellij.markdown.** { *; }
