package com.fps.calculadora.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Paridade dos dois cálculos que estavam presos dentro de funções de DOM:
 * o equilíbrio CPU × GPU (`renderBneck` :2762) e o custo de energia
 * (`renderEnergy` :3166).
 *
 * Diferente da `calc()` e da `badge()`, essas expressões **não podem ser
 * chamadas** do sandbox — elas montam HTML. Então `gen-golden.mjs` reproduz as
 * fórmulas e as avalia sobre valores reais vindos da `calc()` do JS. O que este
 * teste garante é a aritmética (arredondamento, ordem, saturação em 1%), sobre
 * 497 combinações de hardware de verdade.
 */
class DerivedParityTest {

    private val db = GameDatabase.default
    private val calculator = FpsCalculator(db)

    @Serializable
    data class DerivedState(
        val game: Int, val cpu: Int, val gpu: Int, val mobo: Int,
        val ram: String, val res: String, val preset: String,
        val rt: String, val fg: Double, val dlss: Double, val monHz: Int = 144,
    )

    @Serializable
    data class DerivedExpected(
        val gpuLoad: Int, val cpuLoad: Int, val cpuLimited: Boolean,
        val warnings: List<String>,
        val gamingWatts: Int, val kwhPerMonth: Double, val monthlyCost: Double,
    )

    @Serializable
    data class DerivedCase(val tag: String, val state: DerivedState, val expected: DerivedExpected)

    @Serializable
    data class DerivedGolden(val cases: List<DerivedCase>)

    private val golden: DerivedGolden by lazy {
        val stream = javaClass.getResourceAsStream("/golden-derived.json")
            ?: fail("vetores ausentes: golden-derived.json — rode `node tools/gen-golden.mjs`")
        val json = Json { ignoreUnknownKeys = true }
        stream.bufferedReader().use { json.decodeFromString<DerivedGolden>(it.readText()) }
    }

    private fun DerivedState.toBuildState() = BuildState(
        gameId = db.gameAt(game).id,
        cpuId = db.cpuAt(cpu).id,
        gpuId = db.gpuAt(gpu).id,
        moboId = db.moboAt(mobo).id,
        ram = ram,
        resolution = Resolution.fromKey(res),
        preset = preset,
        rt = RtSetting.fromKey(rt),
        frameGen = fg,
        upscaler = dlss,
        monitorHz = monHz,
    )

    private fun round4(x: Double) = floor(x * 1e4 + 0.5) / 1e4

    @Test
    fun `equilibrio CPU x GPU reproduz os vetores do index html`() {
        for (case in golden.cases) {
            val state = case.state.toBuildState()
            val balance = calculator.calc(state).balance()
            val where = "${case.tag} — ${db.cpu(state.cpuId).name} + ${db.gpu(state.gpuId).name}"

            assertEquals(case.expected.cpuLimited, balance.cpuLimited, "quem limita divergiu em $where")
            assertEquals(case.expected.gpuLoad, balance.gpuLoad, "carga da GPU divergiu em $where")
            assertEquals(case.expected.cpuLoad, balance.cpuLoad, "carga da CPU divergiu em $where")
        }
    }

    @Test
    fun `custo de energia reproduz os vetores do index html`() {
        for (case in golden.cases) {
            val state = case.state.toBuildState()
            val energy = db.energyFor(state)
            val where = "${case.tag} — ${db.cpu(state.cpuId).name} + ${db.gpu(state.gpuId).name}"

            assertEquals(case.expected.gamingWatts, energy.gamingWatts, "watts jogando divergiu em $where")
            assertEquals(case.expected.kwhPerMonth, round4(energy.kwhPerMonth), "kWh/mês divergiu em $where")
            assertEquals(case.expected.monthlyCost, round4(energy.monthlyCost), "custo divergiu em $where")
        }
    }

    @Test
    fun `os avisos reproduzem os vetores do index html`() {
        var total = 0
        for (case in golden.cases) {
            val state = case.state.toBuildState()
            val warnings = db.warningsFor(state, calculator.calc(state))
            val where = "${case.tag} — ${db.cpu(state.cpuId).name} + ${db.gpu(state.gpuId).name}"

            // Compara a lista inteira: pega texto, ordem e avisos a mais ou a menos.
            assertEquals(case.expected.warnings, warnings, "avisos divergiram em $where")
            total += warnings.size
        }
        assertTrue(total > 400, "amostra fraca demais: só $total avisos gerados")
    }

    @Test
    fun `numeros inteiros nao ganham casa decimal`() {
        // O JS concatena `8` onde o Kotlin escreveria `8.0`. Este é o guarda-corpo.
        assertEquals("8", jsNumber(8.0))
        assertEquals("12.5", jsNumber(12.5))
        assertEquals("0", jsNumber(0.0))
    }

    @Test
    fun `a carga nunca zera nem passa de 100`() {
        for (case in golden.cases) {
            val balance = calculator.calc(case.state.toBuildState()).balance()
            assertTrue(balance.gpuLoad in 1..100, "carga de GPU fora da faixa: ${balance.gpuLoad}")
            assertTrue(balance.cpuLoad in 1..100, "carga de CPU fora da faixa: ${balance.cpuLoad}")
            // Uma das duas é sempre a que limita, e limita em 100%.
            assertTrue(
                balance.gpuLoad == 100 || balance.cpuLoad == 100,
                "nenhuma das peças aparece como limitante",
            )
        }
    }

    @Test
    fun `o catalogo nao tem hardware sem watts`() {
        // A `renderEnergy()` do JS tem fallbacks `|| 200` e `|| 80` que nunca
        // disparam. O port não os carrega — este teste é o que sustenta isso.
        assertTrue(db.gpus.none { it.watts <= 0 }, "GPU sem watts no catálogo")
        assertTrue(db.cpus.none { it.watts <= 0 }, "CPU sem watts no catálogo")
    }

    @Test
    fun `comparacao por resolucao cobre as tres e respeita o estado`() {
        val state = BuildState(
            gameId = db.games.first().id,
            cpuId = db.cpus.first().id,
            gpuId = db.gpus.first().id,
            moboId = db.mobos.first().id,
            ram = "ddr4_32",
            resolution = Resolution.FHD,
            preset = "ultra",
            rt = RtSetting.OFF,
        )
        val byRes = calculator.byResolution(db.normalize(state))

        assertEquals(Resolution.entries.toSet(), byRes.keys, "faltou resolução na comparação")
        assertEquals(
            calculator.calc(db.normalize(state)).avg,
            byRes.getValue(Resolution.FHD).avg,
            "a resolução atual tem que bater com o cálculo principal",
        )
        assertTrue(
            byRes.getValue(Resolution.FHD).avg >= byRes.getValue(Resolution.UHD).avg,
            "1080p não pode render menos que 4K na mesma build",
        )
    }
}
