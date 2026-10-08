@file:Suppress("MagicNumber") // The §10 type scale values are the spec itself.
package com.example.auplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// One variable sans (OFL); the bundled font file lands in MS-067.
private val auSans = FontFamily.Default

/** §10 sizes 12/14/16/20/28/36. Names/values tunable, scale fixed. */
val auType = Typography(
    labelSmall = TextStyle(fontFamily = auSans, fontSize = 12.sp),
    labelMedium = TextStyle(fontFamily = auSans, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = auSans, fontSize = 16.sp),
    titleMedium = TextStyle(fontFamily = auSans, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = auSans, fontSize = 28.sp),
    displaySmall = TextStyle(fontFamily = auSans, fontSize = 36.sp)
)
