package com.fps.calculadora.core

import kotlin.math.max

/** Um candidato de GPU e o que ela entregaria no lugar da atual. */
data class GpuUpgrade(
    val gpu: Gpu,
    val result: CalcResult,
    val gainFps: Int,
    val gainPercent: Int,
    /** Fonte recomendada pra essa GPU — pra avisar quando ela pede uma maior que a atual. */
    val psuRecommended: Int,
)

/** Um candidato de CPU e o que ela entregaria no lugar da atual. */
data class CpuUpgrade(
    val cpu: Cpu,
    val result: CalcResult,
    val gainFps: Int,
    val gainPercent: Int,
    /** `true` quando é o mesmo socket da CPU atual — não precisa trocar placa-mãe nem RAM. */
    val dropIn: Boolean,
)

/** O upgrade de RAM sugerido (16→32 GB, ou DDR5 4800→5200), quando existir um. */
data class RamUpgrade(
    val ramKey: String,
    val ramLabel: String,
    val result: CalcResult,
    val gainFps: Int,
    val reason: String,
)

/** Qual peça está segurando o desempenho hoje. */
enum class Bottleneck { CPU, VRAM, GPU }

data class UpgradeAdvice(
    val bottleneck: Bottleneck,
    val verdictTitle: String,
    val verdictBody: String,
    /** `true` quando a seção de CPU deve vir antes da de GPU na tela. */
    val cpuSectionFirst: Boolean,
    val gpuUpgrades: List<GpuUpgrade>,
    val gpuEmptyMessage: String?,
    val cpuUpgrades: List<CpuUpgrade>,
    val cpuEmptyMessage: String?,
    val ramUpgrade: RamUpgrade?,
    val currentAvg: Int,
    val currentPsuRecommended: Int,
    val footer: String,
)

private const val MIN_GAIN_RATIO = 1.05
private const val MAX_UPGRADE_CANDIDATES = 5

/**
 * "O que trocar primeiro?" — porta 1:1 o `renderUpg()` do `index.html`
 * (:2867).
 *
 * Varre o catálogo inteiro de CPUs e GPUs trocando uma peça por vez (o resto
 * da build é renormalizado, então trocar de CPU pode levar junto a placa-mãe
 * e a RAM), filtra quem ganha pelo menos 5% de FPS sobre a build atual, e
 * devolve os 5 melhores candidatos por peça — não é "a próxima da lista", é
 * ranking por ganho real.
 */
fun FpsCalculator.upgradeAdvice(state: BuildState): UpgradeAdvice {
    val cur = calc(state)
    val cpu = db.cpu(state.cpuId)
    val gpu = db.gpu(state.gpuId)
    val psuCur = psu(state).recommended

    fun gain(newAvg: Int) = newAvg - cur.avg
    fun gainPercent(newAvg: Int) = jsRound(gain(newAvg).toDouble() / cur.avg * 100)

    val gpuUpgrades = db.gpus
        .filter { it.id != gpu.id }
        .map { candidate ->
            val s2 = db.normalize(state.copy(gpuId = candidate.id))
            Triple(candidate, s2, calc(s2))
        }
        .filter { (_, _, r) -> r.avg >= cur.avg * MIN_GAIN_RATIO }
        .sortedByDescending { (_, _, r) -> r.avg }
        .take(MAX_UPGRADE_CANDIDATES)
        .map { (candidate, s2, r) ->
            GpuUpgrade(candidate, r, gain(r.avg), gainPercent(r.avg), psu(s2).recommended)
        }

    val cpuUpgrades = db.cpus
        .filter { it.id != cpu.id }
        .map { candidate ->
            val s2 = db.normalize(state.copy(cpuId = candidate.id))
            Pair(candidate, calc(s2))
        }
        .filter { (_, r) -> r.avg >= cur.avg * MIN_GAIN_RATIO }
        .sortedByDescending { (_, r) -> r.avg }
        .take(MAX_UPGRADE_CANDIDATES)
        .map { (candidate, r) ->
            CpuUpgrade(candidate, r, gain(r.avg), gainPercent(r.avg), candidate.socket == cpu.socket)
        }

    val ramUpgrade = ramUpgrade(state, cur.avg)

    val bottleneck = when {
        cur.cpuBottleneck -> Bottleneck.CPU
        cur.vramBottleneck -> Bottleneck.VRAM
        else -> Bottleneck.GPU
    }
    val (verdictTitle, verdictBody) = when (bottleneck) {
        Bottleneck.CPU -> "Seu maior ganho vem da CPU" to
            "${cpu.name} está segurando a ${gpu.name} em ${cur.cpuCap} FPS neste jogo. " +
                "Trocar a CPU destrava até ~${cur.gpuFps} FPS."
        Bottleneck.VRAM -> "VRAM está limitando" to
            "A ${gpu.name} tem ${jsNumber(gpu.vram)} GB — menos do que o jogo pede " +
                "(~${jsNumber(cur.vramNeeded)} GB). Priorize uma GPU com mais VRAM."
        Bottleneck.GPU -> {
            val headroom = max(0, 100 - jsRound(cur.gpuFps.toDouble() / cur.cpuCap * 100))
            "Seu maior ganho vem da GPU" to
                "O sistema está limitado pela ${gpu.name} — a CPU ainda tem folga de $headroom%."
        }
    }

    val gpuEmptyMessage = if (gpuUpgrades.isNotEmpty()) null else if (cur.cpuBottleneck) {
        "GPU não é o gargalo agora — troque a CPU primeiro."
    } else {
        "Nenhuma GPU da base ganha ≥5% neste cenário — você já está no teto."
    }

    val cpuEmptyMessage = if (cpuUpgrades.isNotEmpty()) null else if (cur.cpuBottleneck) {
        "Nenhuma CPU da base ganha ≥5% aqui."
    } else {
        "Trocar CPU quase não ganha aqui — você está limitado pela GPU."
    }

    val footer = "Partindo de ${cur.avg} FPS (${shortCpuName(cpu.name)} + ${gpu.name}). " +
        "Ganhos valem para o jogo e preset atuais — mudar de jogo muda o ranking."

    return UpgradeAdvice(
        bottleneck = bottleneck,
        verdictTitle = verdictTitle,
        verdictBody = verdictBody,
        cpuSectionFirst = cur.cpuBottleneck,
        gpuUpgrades = gpuUpgrades,
        gpuEmptyMessage = gpuEmptyMessage,
        cpuUpgrades = cpuUpgrades,
        cpuEmptyMessage = cpuEmptyMessage,
        ramUpgrade = ramUpgrade,
        currentAvg = cur.avg,
        currentPsuRecommended = psuCur,
        footer = footer,
    )
}

/** 16→32 GB elimina a penalidade de RAM; DDR5 4800→5200 tira a penalidade de clock. Nunca os dois juntos. */
private fun FpsCalculator.ramUpgrade(state: BuildState, curAvg: Int): RamUpgrade? {
    val targetKey = when {
        state.ram.contains("16") -> state.ram.replace("16", "32")
        state.ram.contains("4800") -> state.ram.replace("_4800", "")
        else -> return null
    }
    val label = db.constants.ramLabels[targetKey] ?: return null
    val s2 = db.normalize(state.copy(ram = targetKey))
    val result = calc(s2)
    if (result.avg <= curAvg) return null
    val reason = if (state.ram.contains("16")) {
        "elimina a penalidade de 16 GB"
    } else {
        "DDR5 5200 no lugar de 4800"
    }
    return RamUpgrade(targetKey, label, result, result.avg - curAvg, reason)
}
