package com.example.auplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/** §10: 20–28 dp radii for sheets/cards, pill chips. */
val auShapes = Shapes(
    small = RoundedCornerShape(Spacing.s4),
    medium = RoundedCornerShape(Spacing.card),
    large = RoundedCornerShape(Spacing.sheet)
)
