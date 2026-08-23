package com.fps.calculadora.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em

/**
 * Mapeia os tokens do app para os papéis do Material 3, para que componentes
 * prontos (ModalBottomSheet, Snackbar, NavigationBar) já nasçam na paleta certa
 * em vez de aparecerem roxos.
 */
private val FpsColorScheme = darkColorScheme(
    primary = FpsColors.Acc,
    onPrimary = FpsColors.Bg0,
    primaryContainer = FpsColors.AccDeep,
    onPrimaryContainer = FpsColors.Tx1,
    secondary = FpsColors.Tx2,
    onSecondary = FpsColors.Bg0,
    background = FpsColors.Bg0,
    onBackground = FpsColors.Tx1,
    surface = FpsColors.Bg1,
    onSurface = FpsColors.Tx1,
    surfaceVariant = FpsColors.Bg2,
    onSurfaceVariant = FpsColors.Tx2,
    surfaceContainer = FpsColors.Bg1,
    surfaceContainerHigh = FpsColors.Bg2,
    surfaceContainerHighest = FpsColors.Bg3,
    outline = FpsColors.Line2,
    outlineVariant = FpsColors.Line,
    error = FpsColors.Bad,
    onError = FpsColors.Bg0,
    scrim = Color.Black.copy(alpha = 0.62f),
)

private val FpsTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = FpsSans),
        displayMedium = base.displayMedium.copy(fontFamily = FpsSans),
        displaySmall = base.displaySmall.copy(fontFamily = FpsSans),
        headlineLarge = base.headlineLarge.copy(fontFamily = FpsSans),
        headlineMedium = base.headlineMedium.copy(fontFamily = FpsSans),
        headlineSmall = base.headlineSmall.copy(fontFamily = FpsSans),
        titleLarge = base.titleLarge.copy(fontFamily = FpsSans),
        titleMedium = base.titleMedium.copy(fontFamily = FpsSans),
        titleSmall = base.titleSmall.copy(fontFamily = FpsSans),
        bodyLarge = base.bodyLarge.copy(fontFamily = FpsSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = FpsSans),
        bodySmall = base.bodySmall.copy(fontFamily = FpsSans),
        labelLarge = base.labelLarge.copy(fontFamily = FpsSans),
        labelMedium = base.labelMedium.copy(fontFamily = FpsSans),
        labelSmall = base.labelSmall.copy(fontFamily = FpsSans),
    )
}

/** Estilo dos números — `--mono`, 700, com o mesmo aperto de letra do CSS. */
val MonoNumber = TextStyle(
    fontFamily = RobotoMono,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.03).em,
)

@Composable
fun FpsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FpsColorScheme,
        typography = FpsTypography,
        shapes = FpsShapes,
        content = content,
    )
}

/**
 * Fundo do app: o `radial-gradient` laranja no topo sobre o `--bg0`
 * (`index.html:53-56`).
 *
 * O CSS usa uma elipse (120% de largura por 46% de altura ancorada em 50% / -6%);
 * o `Brush.radialGradient` do Compose só faz círculo, então aproximamos pelo raio
 * horizontal. Como o gradiente é um brilho de 10% de opacidade que some em 60%,
 * a diferença não é perceptível.
 */
@Composable
fun FpsBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(FpsColors.Bg0)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colorStops = arrayOf(0f to FpsColors.HeroGlow, 0.6f to Color.Transparent),
                        center = Offset(size.width / 2f, -0.06f * size.height),
                        radius = size.width * 0.6f,
                    )
                )
            }
    ) { content() }
}
