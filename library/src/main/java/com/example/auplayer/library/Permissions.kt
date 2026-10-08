package com.example.auplayer.library

import android.Manifest

/**
 * Media permission mapping (MS-020). ContentResolver/MediaStore/SAF bodies
 * need a device; the mapping + states are pure and JVM-tested. Android 14
 * partial access is surfaced as [MediaAccess.PARTIAL].
 */
enum class MediaAccess {
    GRANTED,
    DENIED,
    PARTIAL
}

fun mediaPermissionFor(sdkInt: Int): String {
    return if (sdkInt >= LEVEL_TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

/** Android 14+ can grant partial photo/video access. */
fun supportsPartialAccess(sdkInt: Int): Boolean = sdkInt >= LEVEL_UPSIDE_DOWN_CAKE

private const val LEVEL_TIRAMISU = 33
private const val LEVEL_UPSIDE_DOWN_CAKE = 34
