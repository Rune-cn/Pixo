plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    jvm("desktop")

    val composeVersion = "1.12.1"
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
            implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
            implementation("org.jetbrains.compose.animation:animation:$composeVersion")
            implementation("org.jetbrains.compose.ui:ui:$composeVersion")
            implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
            implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
            implementation("top.yukonga.miuix.kmp:miuix-blur:0.9.4")
            implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
            implementation("io.coil-kt.coil3:coil-compose:3.6.3")
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
            // JetBrains Markdown 解析器（GFM），配合自研 Compose 渲染器（MarkdownBlock.kt）
            implementation("org.jetbrains:markdown:0.7.9")
            // 灯箱图片缩放（双指捏合/拖动/双击）：成熟的 telephoto zoomable（KMP，纯手势，不依赖具体图片加载器）
            implementation("me.saket.telephoto:zoomable:0.19.0")
            implementation("com.squareup.okhttp3:okhttp:5.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")
            implementation("org.jetbrains.compose.components:components-resources:1.12.1")
            implementation("org.jetbrains.androidx.navigationevent:navigationevent-compose:1.1.0")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.13.0")
            implementation("androidx.core:core-ktx:1.19.1")
            implementation("androidx.appcompat:appcompat:1.7.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
        }
        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

android {
    namespace = "app.pixo"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.pixo"
        minSdk = 33
        targetSdk = 37
        versionCode = 6
        versionName = "1.0.5"
        // 只保留 zh/en 资源，去掉 androidx 其它语言副本，缩减 APK
        resourceConfigurations += setOf("zh", "en")
    }

    signingConfigs {
        // keystore.properties 与密钥库文件均不入库（见 .gitignore）。
        // 本地有该文件时才填充 release 签名；CI / 无签名环境下创建空配置，
        // 使 debug 构建（自带调试签名）可在无 keystore 时正常配置与出包。
        create("release") {
            val ksFile = rootProject.file("keystore.properties")
            if (ksFile.exists()) {
                val props = ksFile.readLines()
                    .mapNotNull { line -> line.split("=", limit = 2).takeIf { it.size == 2 } }
                    .associate { it[0] to it[1] }
                storeFile = file(props.getValue("storeFile"))
                storePassword = props.getValue("storePassword")
                keyAlias = props.getValue("keyAlias")
                keyPassword = props.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.desktop {
    application {
        mainClass = "app.pixo.MainKt"
        // 走 Windows/macOS/Linux 系统代理：Pixiv 需代理才能直连
        jvmArgs += "-Djava.net.useSystemProxies=true"
    }
}

// 临时预览任务（无头渲染悬浮底栏按压前后）：
// > .\gradlew.bat :app:barPreview --args=build/barfix2
tasks.register<JavaExec>("barPreview") {
    group = "application"
    dependsOn("desktopMainClasses")
    val mainCompilation = kotlin.targets.getByName("desktop").compilations.getByName("main")
    classpath = (mainCompilation.runtimeDependencyFiles ?: project.files())
        .plus(project.files("build/classes/kotlin/desktop/main"))
        .plus(project.files("build/processedResources/desktop/main"))
    mainClass.set("app.pixo.BarPreviewKt")
    workingDir = rootProject.projectDir
}

compose.resources {
    packageOfResClass = "app.pixo.resources"
    publicResClass = true
}
