package com.fps.calculadora.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Raios `--r-lg / --r-md / --r-sm` do `index.html`. */
object FpsRadius {
    val Lg = 22.dp
    val Md = 14.dp
    val Sm = 10.dp
}

/** Altura da barra de navegação inferior (`--nav-h`). */
val NavBarHeight = 62.dp

val FpsShapes = Shapes(
    extraSmall = RoundedCornerShape(FpsRadius.Sm),
    small = RoundedCornerShape(FpsRadius.Sm),
    medium = RoundedCornerShape(FpsRadius.Md),
    large = RoundedCornerShape(FpsRadius.Lg),
    extraLarge = RoundedCornerShape(FpsRadius.Lg),
)
