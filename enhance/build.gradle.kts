plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "app.auplayer.enhance"
}

dependencies {
    api(projects.aucore)
    implementation(libs.bundles.kotlinx.coroutines)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
