package com.fps.calculadora.core

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Um passo da cadeia de multiplicação, na ordem em que foi aplicado. */
data class Step(
    val title: String,
    val detail: String,
    /** Multiplicador aplicado, ou `null` quando o passo é um valor absoluto. */
    val mult: Double?,
    /** `true` quando o passo é o teto de CPU, que substitui o valor em vez de multiplicar. */
    val isCap: Boolean = false,
    val fps: Double,
)

data class PsuEstimate(val min: Int, val recommended: Int, val total: Int)

data class CalcResult(
    val avg: Int,
    /** 1% low — os frames mais lentos, o que se sente como engasgo. */
    val min: Int,
    val max: Int,
    val avgLow: Int, val avgHigh: Int,
    val minLow: Int, val minHigh: Int,
    val maxLow: Int, val maxHigh: Int,
    val cpuBottleneck: Boolean,
    val vramBottleneck: Boolean,
    val ramBottleneck: Boolean,
    val gpuWarning: String,
    val moboWarning: String,
    val vramNeeded: Double,
    val vramAvailable: Double,
    val frameGenMult: Double,
    /** FPS antes do Frame Generation — os frames "reais", que definem a latência. */
    val baseFps: Int,
    val baseMin: Int,
    val cpuCap: Int,
    /** O que a GPU entregaria sozinha, antes do teto de CPU. */
    val gpuFps: Int,
    val steps: List<Step>,
)

/**
 * Estimador de FPS — porta 1:1 da `calc()` do `index.html`.
 *
 * Os testes em `GoldenParityTest` comparam cada número e cada passo contra
 * vetores gerados da implementação JS original, então qualquer divergência de
 * ordem de operação ou arredondamento falha o build.
 */
class FpsCalculator(val db: GameDatabase = GameDatabase.default) {

    fun calc(state: BuildState): CalcResult {
        val game = db.game(state.gameId)
        val cpu = db.cpu(state.cpuId)
        val gpu = db.gpu(state.gpuId)
        val mobo = db.mobo(state.moboId)
        val steps = mutableListOf<Step>()

        val noRt = !gpu.gen.hasRtCores
        val canRr = gpu.gen.hasRayReconstruction
        val isRtx2030 = gpu.gen == GpuGen.RTX20 || gpu.gen == GpuGen.RTX30
        val radeon = gpu.gen.isRadeon

        // O modo de RT pedido nem sempre é o que roda: o hardware pode não dar conta.
        val rt = when {
            game.rtMode == RtMode.NONE -> RtSetting.OFF
            noRt && game.rtMode != RtMode.LUMEN -> RtSetting.OFF
            noRt && game.rtMode == RtMode.LUMEN && state.rt == RtSetting.FULL -> RtSetting.STANDARD
            !canRr && game.rtMode == RtMode.PATH_TRACING && state.rt == RtSetting.FULL -> RtSetting.STANDARD
            else -> state.rt
        }

        var gpuWarning = when {
            state.rt != RtSetting.OFF && noRt && game.rtMode != RtMode.NONE ->
                "${gpu.name} não tem RT cores — usando rasterização pura."
            game.rtMode == RtMode.PATH_TRACING && state.rt == RtSetting.FULL && (isRtx2030 || radeon) ->
                "${gpu.name} não suporta Path Tracing completo — usando Ray Tracing padrão."
            else -> ""
        }
        if (state.frameGen > 1 && !gpu.gen.hasFrameGen) {
            gpuWarning = (if (gpuWarning.isNotEmpty()) "$gpuWarning " else "") +
                "Frame Generation não suportado nessa geração."
        }

        var base = game.referenceFps(state.resolution, rt)
        steps += Step(
            title = "Benchmark base — ${game.name}",
            detail = "${state.resolution.label} · ${game.rtLabelFor(rt)} · referência RTX 5070",
            mult = null, fps = base,
        )

        base *= gpu.mult
        steps += Step(gpu.name, "desempenho relativo da GPU", gpu.mult, fps = base)

        // A placa-mãe (VRM) só afeta o quanto a CPU sustenta boost — não tem
        // ligação física com o lado GPU do cálculo. O efeito dela entra
        // depois, no teto de CPU, não aqui.
        val moboWarning = if (mobo.mult < 0.98 && cpu.mult >= 1.00) {
            "${mobo.name} pode limitar o boost do ${cpu.name} por VRM insuficiente."
        } else ""

        // Software Lumen não usa os RT cores, então não paga o pedágio da arquitetura.
        val softwareLumen = game.rtMode == RtMode.LUMEN && rt == RtSetting.STANDARD
        if (rt != RtSetting.OFF && !softwareLumen) {
            db.constants.rtEfficiencyByGen[gpu.gen]?.let { efficiency ->
                base *= efficiency
                steps += Step("Eficiência de RT da arquitetura", gpu.group, efficiency, fps = base)
            }
        }

        val preset = db.preset(state.preset)
        val presetMult = preset?.mult ?: 1.0
        base *= presetMult
        steps += Step("Preset ${preset?.name ?: state.preset}", "qualidade gráfica", presetMult, fps = base)

        val upscalerMult = if (state.upscaler != 0.0) state.upscaler else 1.0
        base *= upscalerMult
        steps += Step(
            db.upscalerName(gpu, upscalerMult).ifEmpty { "Upscaler" },
            if (radeon) "upscaling FSR" else "upscaling DLSS",
            upscalerMult, fps = base,
        )

        val vramNeeded = game.vramNeeded(state.resolution) +
            (db.constants.vramByPreset[state.preset] ?: 0.0) +
            vramForRt(game.rtMode, rt)
        val vramAvailable = gpu.vram
        val vramBottleneck = vramNeeded > vramAvailable
        if (vramBottleneck) {
            // Estourar a VRAM custa caro, mas satura: no pior caso sobra 45% do desempenho.
            val penalty = max(1 - min((vramNeeded - vramAvailable) / vramAvailable, 0.55), 0.45)
            base *= penalty
            val needLabel = jsRound(vramNeeded * 10) / 10.0
            steps += Step(
                "VRAM insuficiente",
                "precisa ~$needLabel GB · tem $vramAvailable GB",
                penalty, fps = base,
            )
        }

        val gpuFps = base // lado GPU, antes do teto de CPU

        val cpuCap = (game.cpuCap ?: Game.DEFAULT_CPU_CAP) * cpu.mult * mobo.mult
        val cpuBottleneck = base > cpuCap
        if (cpuBottleneck) {
            base = cpuCap
            val capDetail = if (mobo.mult != 1.0) {
                "limite de frames que a CPU prepara · ${mobo.name} reduz o boost sustentado"
            } else {
                "limite de frames que a CPU prepara"
            }
            steps += Step(
                "Teto de CPU — ${cpu.name}",
                capDetail,
                mult = null, isCap = true, fps = base,
            )
        }

        val ramBottleneck = state.ram.contains("16")
        if (ramBottleneck) {
            val penalty = if (game.heavy) 0.92 else 0.97
            base *= penalty
            steps += Step("RAM 16 GB", "recomendado: 32 GB", penalty, fps = base)
        }
        if (state.ram.contains("4800")) {
            base *= DDR5_4800_PENALTY
            steps += Step("DDR5 4800 MT/s", "abaixo do padrão 5200", DDR5_4800_PENALTY, fps = base)
        }

        val frameGenMult = if (state.frameGen != 0.0) state.frameGen else 1.0
        val baseFps = base
        base *= frameGenMult
        if (frameGenMult > 1) {
            steps += Step(
                "Frame Generation ×" + String.format(Locale.ROOT, "%.2f", frameGenMult),
                "quadros gerados por IA", frameGenMult, fps = base,
            )
        }

        // Engasgo por VRAM ou RAM afunda o 1% low muito mais do que a média.
        val stutter = vramBottleneck || ramBottleneck
        val minMult = if (stutter) 0.60 else 0.76
        val minLowMult = if (stutter) 0.52 else 0.72
        val minHighMult = if (stutter) 0.66 else 0.80

        // O Frame Generation interpola quadros extras — infla a média/máximo
        // que a tela mostra, mas não elimina o engasgo do pipeline de
        // renderização real. O 1% low usa baseFps (antes do FG) para não
        // fingir uma suavidade que a interpolação de quadros não entrega.
        return CalcResult(
            avg = jsRound(base),
            min = jsRound(baseFps * minMult),
            max = jsRound(base * MAX_MULT),
            avgLow = jsRound(base * 0.95), avgHigh = jsRound(base * 1.05),
            minLow = jsRound(baseFps * minLowMult), minHigh = jsRound(baseFps * minHighMult),
            maxLow = jsRound(base * 1.18), maxHigh = jsRound(base * 1.30),
            cpuBottleneck = cpuBottleneck,
            vramBottleneck = vramBottleneck,
            ramBottleneck = ramBottleneck,
            gpuWarning = gpuWarning,
            moboWarning = moboWarning,
            vramNeeded = jsRound(vramNeeded * 10) / 10.0,
            vramAvailable = vramAvailable,
            frameGenMult = frameGenMult,
            baseFps = jsRound(baseFps),
            baseMin = jsRound(baseFps * minMult),
            cpuCap = jsRound(cpuCap),
            gpuFps = jsRound(gpuFps),
            steps = steps,
        )
    }

    /** VRAM extra em GB cobrada pelo modo de ray tracing efetivamente ativo. */
    fun vramForRt(mode: RtMode, rt: RtSetting): Double = when {
        rt == RtSetting.OFF -> 0.0
        mode == RtMode.PATH_TRACING && rt == RtSetting.FULL -> 2.0
        mode == RtMode.PATH_TRACING && rt == RtSetting.STANDARD -> 1.0
        mode == RtMode.RAY_TRACING && rt == RtSetting.STANDARD -> 1.0
        mode == RtMode.LUMEN && rt == RtSetting.FULL -> 1.0
        mode == RtMode.LUMEN && rt == RtSetting.STANDARD -> 0.5
        else -> 0.0
    }

    /** Fonte mínima e recomendada, arredondadas para cima no múltiplo de 50 W. */
    fun psu(state: BuildState): PsuEstimate {
        val total = db.gpu(state.gpuId).watts + db.cpu(state.cpuId).watts + db.constants.systemWatts
        return PsuEstimate(
            min = ceil(total / PSU_STEP).toInt() * PSU_STEP.toInt(),
            recommended = ceil(total * PSU_HEADROOM / PSU_STEP).toInt() * PSU_STEP.toInt(),
            total = total,
        )
    }

    private companion object {
        const val MAX_MULT = 1.24
        const val DDR5_4800_PENALTY = 0.975
        const val PSU_STEP = 50.0

        /** 35% de folga sobre o consumo estimado, prática comum de dimensionamento. */
        const val PSU_HEADROOM = 1.35
    }
}
