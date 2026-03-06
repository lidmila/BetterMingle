package com.bettermingle.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val BetterMingleShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object CornerRadius {
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 14.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 28.dp
    val pill = 100.dp

    val button = pill
    val card = lg
    val cardLarge = xl
    val input = md
    val chip = pill
    val badge = xs
    val modal = xl
}
