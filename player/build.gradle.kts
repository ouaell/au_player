plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.example.auplayer.player"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":enhance"))
    implementation(libs.coroutines.core)
    implementation(libs.media3.session)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    testImplementation(libs.junit)
}

/** Stages pinned native libs (no binaries in git); up-to-date when present. */
tasks.register("fetchNativeLibs") {
    outputs.dir("src/main/jniLibs")
    doLast {
        val tag = rootProject.file("native/pins.txt").readLines()
            .first { it.startsWith("NATIVE_RELEASE_TAG=") }
            .substringAfter('=')
        exec {
            commandLine("sh", rootProject.file("tools/fetch_native.sh").absolutePath, tag)
        }
    }
}

tasks.named("preBuild") { dependsOn("fetchNativeLibs") }
