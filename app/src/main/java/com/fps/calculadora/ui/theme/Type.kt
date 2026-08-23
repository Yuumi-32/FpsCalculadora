package com.fps.calculadora.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.fps.calculadora.R

/**
 * Duas famílias, como no CSS: `--sans` para texto e `--mono` para número.
 *
 * Todo valor numérico do app (gauge, stats, multiplicadores, watts) é
 * monoespaçado — é o que impede o número de "dançar" enquanto anima.
 */
val RobotoMono = FontFamily(
    Font(R.font.roboto_mono_regular, FontWeight.Normal),
    Font(R.font.roboto_mono_medium, FontWeight.Medium),
    Font(R.font.roboto_mono_bold, FontWeight.Bold),
)

/** `--sans`: a fonte do sistema, igual ao `-apple-system / Roboto` do CSS. */
val FpsSans = FontFamily.Default
