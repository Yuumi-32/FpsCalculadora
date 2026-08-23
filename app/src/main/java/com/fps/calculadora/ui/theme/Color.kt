package com.fps.calculadora.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de cor transcritos 1:1 do `:root` do `index.html` (linhas 19-42).
 *
 * Enquanto a UI web existir, ela é a referência visual: mudou um token lá,
 * muda aqui. Tema dark-only — o app nunca teve modo claro.
 */
object FpsColors {
    // Fundo → cards → controles
    val Bg0 = Color(0xFF0D0C0A)
    val Bg1 = Color(0xFF161511)
    val Bg2 = Color(0xFF1E1D18)
    val Bg3 = Color(0xFF27261F)

    // Divisórias: branco-creme com alfa baixo, não um cinza sólido
    private val LineBase = Color(0xFFF4F0E4)
    val Line = LineBase.copy(alpha = 0.07f)
    val Line2 = LineBase.copy(alpha = 0.14f)

    // Texto primário / secundário / terciário
    val Tx1 = Color(0xFFF2F0E6)
    val Tx2 = Color(0xFFB3B0A2)
    val Tx3 = Color(0xFF7D7A70)

    // Acento laranja
    val Acc = Color(0xFFE8825A)
    val AccDeep = Color(0xFFC26033)
    val AccSoft = Acc.copy(alpha = 0.14f)

    // Badges de desempenho
    val Ok = Color(0xFFA9D562)
    val OkBg = Color(0xFF96C846).copy(alpha = 0.13f)
    val Bad = Color(0xFFF48A8A)
    val BadBg = Color(0xFFEB6464).copy(alpha = 0.13f)
    val Warn = Color(0xFFF0A836)
    val WarnBg = Color(0xFFF0A836).copy(alpha = 0.12f)
    val Info = Color(0xFF6CB3EF)
    val InfoBg = Color(0xFF5A9BE6).copy(alpha = 0.13f)

    /** Brilho laranja no topo do fundo (`index.html:53-56`). */
    val HeroGlow = Acc.copy(alpha = 0.10f)
}
