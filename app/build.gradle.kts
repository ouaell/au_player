plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
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
}
