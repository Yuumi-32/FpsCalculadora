package com.fps.calculadora.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Contrato da migração: o port Kotlin reproduz a `calc()` do `index.html`
 * número por número.
 *
 * Os vetores em `golden-*.json` são gerados por `tools/gen-golden.mjs`, que
 * executa a implementação JS original num sandbox — ninguém digitou esses
 * valores à mão. Se um FPS mudar, este teste falha e diz exatamente em qual
 * combinação de hardware.
 */
class GoldenParityTest {

    private val db = GameDatabase.default
    private val calculator = FpsCalculator(db)

    /* ── Formato dos vetores ─────────────────────────────────────────── */

    @Serializable
    data class GoldenState(
        val game: Int, val cpu: Int, val gpu: Int, val mobo: Int,
        val ram: String, val res: String, val preset: String,
        val rt: String, val fg: Double, val dlss: Double, val monHz: Int = 144,
    )

    @Serializable
    data class GoldenStep(val t: String, val m: Double? = null, val cap: Boolean = false, val fps: Double)

    @Serializable
    data class GoldenExpected(
        val avg: Int, val min: Int, val max: Int,
        val avgL: Int, val avgH: Int, val minL: Int, val minH: Int, val maxL: Int, val maxH: Int,
        val cBot: Boolean, val vBot: Boolean, val rBot: Boolean,
        val gpuWarn: String, val moboWarn: String,
        val vNeed: Double, val vAvail: Double,
        val fgM: Double, val bFPS: Int, val bMin: Int,
        val cpuCap: Int, val gpuFps: Int,
        val psuMin: Int, val psuRecommended: Int, val psuTotal: Int,
        val steps: List<GoldenStep>,
    )

    @Serializable
    data class GoldenCase(val tag: String, val state: GoldenState, val expected: GoldenExpected)

    @Serializable
    data class GoldenCounts(val games: Int, val cpus: Int, val gpus: Int, val mobos: Int)

    @Serializable
    data class GoldenMeta(val generatedFrom: String, val dbVersion: String, val counts: GoldenCounts)

    @Serializable
    data class GoldenCalcFile(val meta: GoldenMeta, val cases: List<GoldenCase>)

    @Serializable
    data class NormalizeCase(val raw: GoldenState, val normalized: GoldenState)

    @Serializable
    data class GoldenNormalizeFile(val meta: GoldenMeta, val cases: List<NormalizeCase>)

    private val json = Json { ignoreUnknownKeys = true }

    private inline fun <reified T> loadGolden(file: String): T {
        val stream = javaClass.getResourceAsStream("/$file") ?: fail("vetores ausentes: $file — rode `node tools/gen-golden.mjs`")
        return stream.bufferedReader().use { json.decodeFromString<T>(it.readText()) }
    }

    /** O golden referencia hardware por índice (formato antigo); o core, por id. */
    private fun GoldenState.toBuildState() = BuildState(
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

    private fun describe(s: GoldenState) =
        "${db.gameAt(s.game).name} · ${db.cpuAt(s.cpu).name} · ${db.gpuAt(s.gpu).name} · " +
            "${db.moboAt(s.mobo).name} · ${s.ram} · ${s.res} · ${s.preset} · rt=${s.rt} · fg=${s.fg} · dlss=${s.dlss}"

    /** Mesmo arredondamento de 4 casas usado ao gerar os vetores. */
    private fun round4(x: Double) = floor(x * 1e4 + 0.5) / 1e4

    private fun round4(x: Double?) = x?.let { round4(it) }

    /* ── A base carregou o que devia? ────────────────────────────────── */

    @Test
    fun `base de dados bate com a origem`() {
        val file = loadGolden<GoldenCalcFile>("golden-calc.json")
        assertEquals(file.meta.dbVersion, db.meta.version, "versão da base divergente")
        assertEquals(file.meta.counts.games, db.games.size, "quantidade de jogos")
        assertEquals(file.meta.counts.cpus, db.cpus.size, "quantidade de CPUs")
        assertEquals(file.meta.counts.gpus, db.gpus.size, "quantidade de GPUs")
        assertEquals(file.meta.counts.mobos, db.mobos.size, "quantidade de placas-mãe")
    }

    @Test
    fun `ids do catalogo sao unicos e estaveis`() {
        listOf(
            "jogos" to db.games.map { it.id },
            "CPUs" to db.cpus.map { it.id },
            "GPUs" to db.gpus.map { it.id },
            "placas-mãe" to db.mobos.map { it.id },
        ).forEach { (label, ids) ->
            val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }
            assertTrue(duplicates.isEmpty(), "ids duplicados em $label: ${duplicates.keys}")
            assertTrue(ids.none { it.isBlank() }, "id vazio em $label")
        }
        // O índice tem que continuar sendo a posição real: builds salvos dependem disso.
        db.games.forEachIndexed { i, g -> assertEquals(i, g.index, "índice do jogo ${g.name}") }
        db.cpus.forEachIndexed { i, c -> assertEquals(i, c.index, "índice da CPU ${c.name}") }
        db.gpus.forEachIndexed { i, g -> assertEquals(i, g.index, "índice da GPU ${g.name}") }
        db.mobos.forEachIndexed { i, m -> assertEquals(i, m.index, "índice da placa ${m.name}") }
    }

    /* ── Paridade de calc() ──────────────────────────────────────────── */

    @Test
    fun `calc reproduz os vetores do index html`() {
        val file = loadGolden<GoldenCalcFile>("golden-calc.json")
        assertTrue(file.cases.size > 3000, "poucos vetores: ${file.cases.size}")

        val failures = mutableListOf<String>()

        for (case in file.cases) {
            val state = case.state.toBuildState()
            val actual = calculator.calc(state)
            val psu = calculator.psu(state)
            val e = case.expected
            val diffs = mutableListOf<String>()

            fun <T> check(field: String, expected: T, got: T) {
                if (expected != got) diffs += "$field: esperado=$expected obtido=$got"
            }

            check("avg", e.avg, actual.avg)
            check("min", e.min, actual.min)
            check("max", e.max, actual.max)
            check("avgL", e.avgL, actual.avgLow)
            check("avgH", e.avgH, actual.avgHigh)
            check("minL", e.minL, actual.minLow)
            check("minH", e.minH, actual.minHigh)
            check("maxL", e.maxL, actual.maxLow)
            check("maxH", e.maxH, actual.maxHigh)
            check("cBot", e.cBot, actual.cpuBottleneck)
            check("vBot", e.vBot, actual.vramBottleneck)
            check("rBot", e.rBot, actual.ramBottleneck)
            check("gpuWarn", e.gpuWarn, actual.gpuWarning)
            check("moboWarn", e.moboWarn, actual.moboWarning)
            check("vNeed", e.vNeed, actual.vramNeeded)
            check("vAvail", e.vAvail, actual.vramAvailable)
            check("fgM", e.fgM, actual.frameGenMult)
            check("bFPS", e.bFPS, actual.baseFps)
            check("bMin", e.bMin, actual.baseMin)
            check("cpuCap", e.cpuCap, actual.cpuCap)
            check("gpuFps", e.gpuFps, actual.gpuFps)
            check("psu.min", e.psuMin, psu.min)
            check("psu.recommended", e.psuRecommended, psu.recommended)
            check("psu.total", e.psuTotal, psu.total)

            // Os passos pegam divergência na ORDEM das multiplicações, que o
            // resultado arredondado esconderia.
            check("steps.size", e.steps.size, actual.steps.size)
            if (e.steps.size == actual.steps.size) {
                e.steps.forEachIndexed { i, expectedStep ->
                    val step = actual.steps[i]
                    check("steps[$i].titulo", expectedStep.t, step.title)
                    check("steps[$i].mult", expectedStep.m, round4(step.mult))
                    check("steps[$i].cap", expectedStep.cap, step.isCap)
                    check("steps[$i].fps", expectedStep.fps, round4(step.fps))
                }
            }

            if (diffs.isNotEmpty()) {
                failures += "[${case.tag}] ${describe(case.state)}\n    " + diffs.joinToString("\n    ")
            }
        }

        if (failures.isNotEmpty()) {
            fail(
                "${failures.size} de ${file.cases.size} casos divergiram do index.html:\n\n" +
                    failures.take(15).joinToString("\n\n") +
                    if (failures.size > 15) "\n\n… e mais ${failures.size - 15}." else ""
            )
        }
    }

    /* ── Paridade de normalize() ─────────────────────────────────────── */

    @Test
    fun `normalize reproduz os vetores do index html`() {
        val file = loadGolden<GoldenNormalizeFile>("golden-normalize.json")
        val failures = mutableListOf<String>()

        for (case in file.cases) {
            val actual = db.normalize(case.raw.toBuildState())
            val expected = case.normalized.toBuildState()
            if (actual != expected) {
                val diffs = buildList {
                    if (actual.ram != expected.ram) add("ram: esperado=${expected.ram} obtido=${actual.ram}")
                    if (actual.moboId != expected.moboId) add("mobo: esperado=${expected.moboId} obtido=${actual.moboId}")
                    if (actual.rt != expected.rt) add("rt: esperado=${expected.rt} obtido=${actual.rt}")
                    if (actual.frameGen != expected.frameGen) add("fg: esperado=${expected.frameGen} obtido=${actual.frameGen}")
                    if (actual.upscaler != expected.upscaler) add("dlss: esperado=${expected.upscaler} obtido=${actual.upscaler}")
                }
                failures += "${describe(case.raw)}\n    " + diffs.joinToString("\n    ")
            }
        }

        if (failures.isNotEmpty()) {
            fail(
                "${failures.size} de ${file.cases.size} normalizações divergiram:\n\n" +
                    failures.take(15).joinToString("\n\n")
            )
        }
    }

    /* ── Invariantes que o golden não cobre ──────────────────────────── */

    @Test
    fun `normalize e idempotente`() {
        val file = loadGolden<GoldenNormalizeFile>("golden-normalize.json")
        for (case in file.cases.take(300)) {
            val once = db.normalize(case.raw.toBuildState())
            assertEquals(once, db.normalize(once), "normalize mudou na segunda passada: ${describe(case.raw)}")
        }
    }

    @Test
    fun `todo build pronto do onboarding e valido`() {
        for ((key, preset) in db.constants.buildPresets) {
            val state = BuildState(
                gameId = db.games.first().id,
                cpuId = preset.cpu, gpuId = preset.gpu, moboId = preset.mobo,
                ram = preset.ram,
                resolution = Resolution.fromKey(preset.res),
                preset = preset.preset,
                rt = RtSetting.fromKey(preset.rt),
                frameGen = preset.fg, upscaler = preset.dlss,
            )
            // Os ids têm que apontar para o mesmo hardware que os índices antigos.
            assertEquals(db.cpuAt(preset.cpuIndex).id, preset.cpu, "cpu do build $key")
            assertEquals(db.gpuAt(preset.gpuIndex).id, preset.gpu, "gpu do build $key")
            assertEquals(db.moboAt(preset.moboIndex).id, preset.mobo, "mobo do build $key")
            assertEquals(db.cpu(preset.cpu).socket, db.mobo(preset.mobo).socket, "socket incompatível no build $key")
            assertTrue(calculator.calc(state).avg > 0, "build $key não produziu FPS")
        }
    }
}
