plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.example.auplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.auplayer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-ms001"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { buildConfig = true }

    flavorDimensions += "store"
    productFlavors {
        create("direct") {
            dimension = "store"
            buildConfigField("boolean", "WEB_SOURCE_ENABLED", "true")
        }
        create("play") {
            dimension = "store"
            buildConfigField("boolean", "WEB_SOURCE_ENABLED", "false")
        }
    }

    buildTypes {
        debug {
            // CI emulator needs x86_64 (§2, §17.2). Release stays arm-only.
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        }
        release {
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Debug key unless release secrets exist (docs/signing.md, owner H8).
            signingConfig = signingConfigs.getByName(
                if (System.getenv("AU_KEYSTORE_PATH").isNullOrBlank()) "debug" else "ciRelease"
            )
        }
        create("smoke") {
            // Minified like release (same R8 rules under test in MS-063) plus
            // x86_64 so the artifact installs on the CI emulator. The real
            // release stays arm-only for size (§2).
            initWith(getByName("release"))
            // Library modules have no `smoke` type: consume their release.
            matchingFallbacks += listOf("release")
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    signingConfigs {
        create("ciRelease") {
            val keystorePath = System.getenv("AU_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("AU_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("AU_KEY_ALIAS")
                keyPassword = System.getenv("AU_KEY_PASSWORD")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":player"))
    implementation(project(":enhance"))
    implementation(project(":reader"))
    implementation(project(":websource"))
    implementation(project(":library"))
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.lifecycle.runtime)
    implementation(libs.navigation.compose)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.datastore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.uiautomator)
}
