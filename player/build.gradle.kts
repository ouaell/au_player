plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

/** Stages pinned native libs (CC-safe: no project captures in the action). */
abstract class FetchNativeLibs : DefaultTask() {
    @get:Inject
    abstract val execOps: ExecOperations

    @get:Input
    abstract val tag: Property<String>

    @get:Input
    abstract val script: Property<String>

    @TaskAction
    fun run() {
        // Repo root derived from the script path (never capture project at execution).
        val root = java.io.File(script.get()).parentFile.parentFile
        execOps.exec {
            workingDir(root)
            commandLine("sh", script.get(), tag.get())
        }
    }
}

android {
    namespace = "com.example.auplayer.player"
    compileSdk = 36
    ndkVersion = "30.0.16248370"
    defaultConfig { minSdk = 26 }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
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
tasks.register<FetchNativeLibs>("fetchNativeLibs") {
    tag.set(
        rootProject.file("native/pins.txt").readLines()
            .first { it.startsWith("NATIVE_RELEASE_TAG=") }
            .substringAfter('=')
    )
    script.set(rootProject.file("tools/fetch_native.sh").absolutePath)
    outputs.dir("src/main/jniLibs")
}

tasks.named("preBuild") { dependsOn("fetchNativeLibs") }
