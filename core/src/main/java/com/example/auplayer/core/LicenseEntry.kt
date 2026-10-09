package com.example.auplayer.core

/** Third-party notice entry (MS-051). Screen + CI check enforce coverage. */
data class LicenseEntry(val name: String, val license: String, val url: String)

/**
 * Curated notices (MS-051). Every dependency allowlist alias maps to one
 * entry here (checked by tools/check_notices.sh); native/model/font weights
 * are marked PENDING until their artifacts land (MS-004/MS-046/MS-067).
 */
val THIRD_PARTY_NOTICES: List<LicenseEntry> = listOf(
    LicenseEntry(
        "AndroidX Core",
        "Apache-2.0",
        "https://github.com/androidx/androidx"
    ),
    LicenseEntry(
        "Compose BOM / UI / Material3 / Icons / Foundation",
        "Apache-2.0",
        "https://developer.android.com/jetpack/compose"
    ),
    LicenseEntry(
        "Activity Compose",
        "Apache-2.0",
        "https://github.com/androidx/androidx"
    ),
    LicenseEntry(
        "Lifecycle",
        "Apache-2.0",
        "https://github.com/androidx/androidx"
    ),
    LicenseEntry(
        "Navigation Compose",
        "Apache-2.0",
        "https://github.com/androidx/navigation"
    ),
    LicenseEntry(
        "Room",
        "Apache-2.0",
        "https://github.com/androidx/androidx"
    ),
    LicenseEntry(
        "DataStore",
        "Apache-2.0",
        "https://github.com/androidx/androidx"
    ),
    LicenseEntry(
        "Kotlin Coroutines",
        "Apache-2.0",
        "https://github.com/Kotlin/kotlinx.coroutines"
    ),
    LicenseEntry(
        "Kotlin (stdlib, metadata reader — kapt only)",
        "Apache-2.0",
        "https://github.com/JetBrains/kotlin"
    ),
    LicenseEntry(
        "OkHttp / MockWebServer",
        "Apache-2.0",
        "https://github.com/square/okhttp"
    ),
    LicenseEntry(
        "Coil",
        "Apache-2.0",
        "https://github.com/coil-kt/coil"
    ),
    LicenseEntry(
        "Media3",
        "Apache-2.0",
        "https://github.com/androidx/media"
    ),
    LicenseEntry(
        "Commons Compress",
        "Apache-2.0",
        "https://github.com/apache/commons-compress"
    ),
    LicenseEntry(
        "XZ for Java",
        "Public-Domain",
        "https://tukaani.org/xz/java.html"
    ),
    LicenseEntry("JUnit4", "EPL-1.0", "https://github.com/junit-team/junit4"),
    LicenseEntry("AndroidX Test", "Apache-2.0", "https://github.com/androidx/androidx"),
    LicenseEntry("Anime4K shaders", "MIT", "https://github.com/bloc97/Anime4K"),
    LicenseEntry(
        "mpv / FFmpeg / libass (self-built)",
        "LGPL-2.1 (PENDING artifact)",
        "https://github.com/mpv-player/mpv"
    ),
    LicenseEntry(
        "NCNN",
        "BSD-3-Clause (PENDING artifact)",
        "https://github.com/Tencent/ncnn"
    ),
    LicenseEntry(
        "Real-ESRGAN weights",
        "PENDING verification (MS-046)",
        "https://github.com/xinntao/Real-ESRGAN"
    ),
    LicenseEntry("Bundled fonts", "PENDING (MS-067)", "https://fonts.google.com")
)
