package com.fps.calculadora.core

import kotlin.math.floor
import kotlin.math.min

/**
 * `Math.round` do JavaScript arredonda .5 para +infinito; `kotlin.math.round`
 * arredonda para longe do zero. Os FPS são sempre positivos, mas replicar a
 * definição exata elimina a classe inteira de divergência.
 */
internal fun jsRound(x: Double): Int = floor(x + 0.5).toInt()

/**
 * Faixa de desempenho de um resultado — o que colore o gauge e escreve o badge.
 * Porta 1:1 da `badge()` do `index.html` (:2059), com vetores próprios em
 * `golden-badge.json`.
 *
 * Mora aqui, e não na UI, porque os limiares são regra de produto: mudar o que
 * conta como "Bom" muda o que o app afirma, não como ele desenha.
 */
enum class PerformanceTier(
    val label: String,
    /** Token de cor do design system, sem o prefixo: `ok`, `warn`, `bad`. */
    val colorToken: String,
    val glow: Boolean,
) {
    UNPLAYABLE("Injogável", "bad", false),
    TOLERABLE("Tolerável", "warn", false),
    GOOD("Bom", "ok", false),
    EXCELLENT("Excelente", "ok", true),
    COMPETITIVE("★ Competitivo", "ok", true);

    /** Token do fundo do badge — sempre a variante `-bg` da cor. */
    val backgroundToken: String get() = "$colorToken-bg"

    companion object {
        fun forFps(fps: Int): PerformanceTier = when {
            fps < 30 -> UNPLAYABLE
            fps < 60 -> TOLERABLE
            fps <= 120 -> GOOD
            fps <= 180 -> EXCELLENT
            else -> COMPETITIVE
        }
    }
}

/**
 * Quanto do monitor a build preenche, de 0 a 1 — a fração do arco do gauge e a
 * largura das barras. Satura em 1: passar do refresh não estica mais nada.
 */
fun monitorFraction(fps: Int, monitorHz: Int): Double =
    if (monitorHz <= 0) 0.0 else min(fps.toDouble() / monitorHz, 1.0)

/** O "vs monitor" das estatísticas — **não** satura, 240 fps num 144 Hz dá 167%. */
fun monitorPercent(fps: Int, monitorHz: Int): Int =
    if (monitorHz <= 0) 0 else jsRound(fps.toDouble() / monitorHz * 100)
