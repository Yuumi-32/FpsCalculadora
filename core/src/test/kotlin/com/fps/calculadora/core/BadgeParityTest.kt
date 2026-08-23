package com.fps.calculadora.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Paridade da `badge()` do `index.html` (:2059) — a faixa de desempenho que
 * decide a cor do gauge e o texto do selo.
 *
 * Mesmo contrato dos outros golden: os vetores em `golden-badge.json` saem da
 * execução do JS original em `tools/gen-golden.mjs`, não de transcrição manual.
 */
class BadgeParityTest {

    @Serializable
    data class BadgeExpected(val label: String, val color: String, val bg: String, val glow: Boolean)

    @Serializable
    data class BadgeCase(val fps: Int, val expected: BadgeExpected)

    @Serializable
    data class BadgeGolden(val cases: List<BadgeCase>)

    private val golden: BadgeGolden by lazy {
        val stream = javaClass.getResourceAsStream("/golden-badge.json")
            ?: fail("vetores ausentes: golden-badge.json — rode `node tools/gen-golden.mjs`")
        val json = Json { ignoreUnknownKeys = true }
        stream.bufferedReader().use { json.decodeFromString<BadgeGolden>(it.readText()) }
    }

    @Test
    fun `badge reproduz os vetores do index html`() {
        val cases = golden.cases
        assertEquals(421, cases.size, "quantidade de vetores mudou")

        for (case in cases) {
            val tier = PerformanceTier.forFps(case.fps)
            val where = "fps=${case.fps}"
            assertEquals(case.expected.label, tier.label, "rótulo divergiu em $where")
            assertEquals("var(--${tier.colorToken})", case.expected.color, "cor divergiu em $where")
            assertEquals("var(--${tier.backgroundToken})", case.expected.bg, "fundo divergiu em $where")
            assertEquals(case.expected.glow, tier.glow, "glow divergiu em $where")
        }
    }

    @Test
    fun `os limiares ficam exatamente onde o JS colocou`() {
        // Redundante com o golden de propósito: se alguém "arredondar" um limiar,
        // este teste nomeia a fronteira quebrada em vez de só apontar um fps solto.
        assertEquals(PerformanceTier.UNPLAYABLE, PerformanceTier.forFps(29))
        assertEquals(PerformanceTier.TOLERABLE, PerformanceTier.forFps(30))
        assertEquals(PerformanceTier.TOLERABLE, PerformanceTier.forFps(59))
        assertEquals(PerformanceTier.GOOD, PerformanceTier.forFps(60))
        assertEquals(PerformanceTier.GOOD, PerformanceTier.forFps(120))
        assertEquals(PerformanceTier.EXCELLENT, PerformanceTier.forFps(121))
        assertEquals(PerformanceTier.EXCELLENT, PerformanceTier.forFps(180))
        assertEquals(PerformanceTier.COMPETITIVE, PerformanceTier.forFps(181))
    }

    @Test
    fun `fracao do monitor satura e porcentagem nao`() {
        assertEquals(0.5, monitorFraction(72, 144))
        assertEquals(1.0, monitorFraction(240, 144), "o arco do gauge não passa do fim")
        assertEquals(167, monitorPercent(240, 144), "mas o 'vs monitor' mostra o excedente")
        assertEquals(100, monitorPercent(144, 144))
        assertEquals(0.0, monitorFraction(60, 0), "monitor inválido não divide por zero")
    }
}
