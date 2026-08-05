import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// 签名来源优先级：环境变量（CI: GitHub Actions Secrets）> 本地 local.properties（已被 .gitignore 忽略，不进版本库）
// 未配置任何签名时，debug/release 降级为系统默认 debug 签名，保证本地与 CI 构建均不中断。
val keystoreProperties = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) {
        load(propsFile.inputStream())
    }
}
fun signingProp(envName: String, localName: String): String? =
    System.getenv(envName) ?: keystoreProperties.getProperty(localName)

val signStoreFile = signingProp("KEYSTORE_FILE", "APP_STORE_FILE")
val signStorePassword = signingProp("KEYSTORE_PASSWORD", "APP_STORE_PASSWORD")
val signKeyAlias = signingProp("KEY_ALIAS", "APP_KEY_ALIAS")
val signKeyPassword = signingProp("KEY_PASSWORD", "APP_KEY_PASSWORD")

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "zhiqiu.app.mk"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "zhiqiu.app.mk"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        // 统一签名：debug 与 release 均使用同一正式签名（key.jks）
        // 签名信息缺失（如 CI 未配 Secrets）时降级为系统默认 debug 签名，保证构建不中断。
        create("appSign") {
            if (signStoreFile != null && signStorePassword != null && signKeyAlias != null && signKeyPassword != null) {
                storeFile = rootProject.file(signStoreFile)
                storePassword = signStorePassword
                keyAlias = signKeyAlias
                keyPassword = signKeyPassword
            } else {
                initWith(getByName("debug"))
            }
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("appSign")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("appSign")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}