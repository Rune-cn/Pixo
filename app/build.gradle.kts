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
            implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
            implementation("io.coil-kt.coil3:coil-compose:3.6.3")
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
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
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val props = rootProject.file("keystore.properties").readLines()
                .map { it.split("=", limit = 2) }
                .associate { it[0] to it[1] }
            storeFile = file(props.getValue("storeFile"))
            storePassword = props.getValue("storePassword")
            keyAlias = props.getValue("keyAlias")
            keyPassword = props.getValue("keyPassword")
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

compose.resources {
    packageOfResClass = "app.pixo.resources"
    publicResClass = true
}
