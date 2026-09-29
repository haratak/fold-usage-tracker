import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

val keystorePath = System.getenv("KEYSTORE_FILE")
val keystoreFile = keystorePath?.let(::File)?.takeIf { it.isFile }
val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
val keyAliasEnv = System.getenv("KEY_ALIAS")
val keyPasswordEnv = System.getenv("KEY_PASSWORD")
val releaseSigningReady = keystoreFile != null &&
    !keystorePassword.isNullOrEmpty() &&
    !keyAliasEnv.isNullOrEmpty() &&
    !keyPasswordEnv.isNullOrEmpty()

if (keystoreFile != null && !releaseSigningReady) {
    throw GradleException(
        "KEYSTORE_FILE is set but KEYSTORE_PASSWORD, KEY_ALIAS, or KEY_PASSWORD is missing.",
    )
}

android {
    namespace = "io.github.haratak.foldusage"
    buildToolsVersion = "37.0.0"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "io.github.haratak.foldusage"
        minSdk = 31
        targetSdk = 37
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = resolveVersionName()
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = keystoreFile
                storePassword = keystorePassword
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
                storeType = "pkcs12"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (releaseSigningReady) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.core.ktx)
    implementation(libs.room.runtime)
    implementation(libs.coroutines.android)
    ksp(libs.room.compiler)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}

fun resolveVersionName(): String {
    val refType = System.getenv("GITHUB_REF_TYPE")
    val refName = System.getenv("GITHUB_REF_NAME")
    return if (refType == "tag" && !refName.isNullOrBlank()) {
        refName.removePrefix("v")
    } else {
        "1.0.0"
    }
}
