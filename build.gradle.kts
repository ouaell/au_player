// Root build — MS-002 gates: detekt + ktlint (ktlint auto-wires into check).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// buildUponDefaultConfig is Gradle-side (not a detekt.yml key): keep the full
// default rule set active in every module that applies detekt.
fun applyDetektDefaults(project: Project) {
    project.pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
        project.extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
        }
    }
}
applyDetektDefaults(project)
subprojects { applyDetektDefaults(this) }
