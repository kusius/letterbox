@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import java.util.Properties


val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.googleServices)
    id("ios-scripts")
    id("native-build")
    id("desktop-credentials")
}

sqldelight {
    linkSqlite = true
    databases {
        create("Database") {
            packageName.set("io.kusius.letterbox")
            version = 2
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}

kotlin {
    applyHierarchyTemplate(KotlinHierarchyTemplate.default) {
        common {
            group("mobile") {
                withAndroidTarget()
                group("ios") {
                    withIosArm64()
                    withIosX64()
                    withIosSimulatorArm64()
                }
            }
        }
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
//    TODO: Need to refactor data store because its not ready for wasm yet (see awesome-kmm)
//    wasmJs {
//        moduleName = "composeApp"
//        browser {
//            val rootDirPath = project.rootDir.path
//            val projectDirPath = project.projectDir.path
//            commonWebpackConfig {
//                outputFileName = "composeApp.js"
//                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
//                    static = (static ?: mutableListOf()).apply {
//                        // Serve sources to debug inside browser
//                        add(rootDirPath)
//                        add(projectDirPath)
//                    }
//                }
//            }
//        }
//        binaries.executable()
//    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.time.ExperimentalTime")
            }
        }
        val desktopMain by getting
        val mobileMain by getting
//        val macosMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.compose.backhandler)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.logging)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.resources)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.logging)
            implementation(libs.napier)
            implementation(libs.kotlinx.serialization.cbor)
            implementation(libs.koin)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.compose.navigation)
            implementation(libs.coil)
            implementation(libs.coil.svg)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.store)
            implementation(libs.kottie)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        desktopMain.dependencies {
            implementation(libs.compose.webview)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqlidelight.sqlite)
            implementation(libs.google.api.client)
            implementation(libs.google.oauth.client.jetty)
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }

        mobileMain.dependencies {
            implementation(libs.compose.webview)
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.credentials)
                implementation(libs.androidx.credentials.play)
                implementation(libs.play.services.auth)
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqlidelight.android)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.sqlidelight.native)
                implementation(libs.ktor.client.darwin)
                implementation(libs.snizzors)
            }
        }
    }
}

android {
    namespace = "io.kusius.letterbox"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "io.kusius.letterbox"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        val androidVersionString =
            libs.versions.app.version.android
                .get()
        versionCode = androidVersionString.toVersionCode()
        versionName = "v$androidVersionString"
    }

    signingConfigs {
        create("release") {
            // Load from local.properties (gitignored) or environment variables
            val keystorePath =
                localProperties.getProperty("RELEASE_KEYSTORE_PATH")
                    ?: System.getenv("RELEASE_KEYSTORE_PATH")
            val keystorePassword =
                localProperties.getProperty("KEYSTORE_PASSWORD")
                    ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
            val keyAlias = "letterbox-upload"
            val keyPassword =
                localProperties.getProperty("KEY_PASSWORD")
                    ?: System.getenv("KEY_PASSWORD") ?: ""

            if (keystorePath != null && keystorePath.isNotEmpty()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                logger.warn("Release signing not configured. Set RELEASE_KEYSTORE_PATH in local.properties")
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-DEBUG"
            resValue("string", "app_name", "Letterbox.dev")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            resValue("string", "app_name", "Letterbox")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "io.kusius.letterbox.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.kusius.letterbox"
            packageVersion = "1.0.0"
        }

        buildTypes.release.proguard {
            configurationFiles.from("compose-desktop.pro")
        }

        // KCEF
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}

// KCEF
afterEvaluate {
    tasks.withType<JavaExec> {
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}

buildConfig {
    val secretsProperties =
        Properties().apply {
            val secretsFile = rootProject.file("secrets.properties")
            if (secretsFile.exists()) {
                load(secretsFile.inputStream())
            }
        }

    buildConfigField("CLIENT_ID", expect<String>())
    buildConfigField("CLIENT_SECRET", expect<String>(""))

    sourceSets.named("androidMain") {
        buildConfigField("CLIENT_ID", provider { secretsProperties.getProperty("android_client_id", "") })
    }

    sourceSets.named("iosMain") {
        buildConfigField("CLIENT_ID", provider { secretsProperties.getProperty("ios_client_id", "") })
    }

    sourceSets.named("desktopMain") {
        buildConfigField("CLIENT_ID", provider { secretsProperties.getProperty("desktop_client_id", "") })
        buildConfigField("CLIENT_SECRET", provider { secretsProperties.getProperty("desktop_client_secret", "") })
    }
}
