import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

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
        val root = File(script.get()).parentFile.parentFile
        execOps.exec {
            workingDir(root)
            commandLine("sh", script.get(), tag.get())
        }
    }
}

android {
    namespace = "app.auplayer.player"

    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    api(projects.aucore)
    api(projects.enhance)
    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.uiGraphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.materialIcons)

    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}

// Stages pinned native libs (no binaries in git); up-to-date when present.
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