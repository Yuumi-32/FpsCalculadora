package com.fps.calculadora.core

/** Uma combinação de ajustes gráficos que alcança a meta de FPS escolhida. */
data class GoalOption(
    val state: BuildState,
    val result: CalcResult,
    val presetName: String,
    val upscalerName: String,
    /** Vazio quando essa opção não usa Frame Generation. */
    val frameGenLabel: String,
    /** `true` quando essa opção desliga o RT que estava ligado na build atual. */
    val turnsRtOff: Boolean,
)

data class GoalAdvice(
    val target: Int,
    val currentAvg: Int,
    /** Nenhuma combinação do catálogo bate a meta neste jogo — o teto real do hardware. */
    val bestPossibleAvg: Int?,
    /** A build já bate a meta com a melhor qualidade disponível — nada a melhorar. */
    val alreadyAtBestQuality: Boolean,
    val options: List<GoalOption>,
)

private const val MAX_GOAL_OPTIONS = 3

/**
 * "Meta de FPS" — porta 1:1 o `fillGoal()` do `index.html` (:3214).
 *
 * Varre combinações de preset × upscaler × Frame Generation × RT (o resto da
 * build fica igual) atrás de quem bate o FPS-alvo. Prioriza qualidade visual —
 * preset mais alto primeiro, upscaler mais leve depois, Frame Gen por último —
 * e só sugere opções melhores que a qualidade atual, quando ela já basta.
 */
fun FpsCalculator.goalAdvice(state: BuildState, target: Int): GoalAdvice {
    val cur = calc(state)
    val gpu = db.gpu(state.gpuId)
    val presets = db.constants.presets
    val upscalers = db.upscalersFor(gpu)
    val frameGens = db.frameGenOptionsFor(gpu)
    val rtChoices = if (state.rt != RtSetting.OFF) listOf(state.rt, RtSetting.OFF) else listOf(state.rt)

    data class Scored(val candidate: BuildState, val result: CalcResult, val score: Int)

    val seen = LinkedHashMap<String, Scored>()
    for (preset in presets) {
        for (upscaler in upscalers) {
            for (frameGen in frameGens) {
                for (rt in rtChoices) {
                    val candidate = db.normalize(
                        state.copy(
                            preset = preset.key,
                            upscaler = upscaler.mult,
                            frameGen = frameGen.mult,
                            rt = rt,
                        ),
                    )
                    val key = "${candidate.preset}|${candidate.upscaler}|${candidate.frameGen}|${candidate.rt}"
                    seen.getOrPut(key) {
                        Scored(
                            candidate,
                            calc(candidate),
                            goalScore(presets, candidate.preset, candidate.upscaler, candidate.frameGen, candidate.rt, state.rt),
                        )
                    }
                }
            }
        }
    }

    val all = seen.values.toList()
    val meets = all.filter { it.result.avg >= target }
    val curScore = goalScore(presets, state.preset, state.upscaler, state.frameGen, state.rt, state.rt)
    val curMeets = cur.avg >= target
    val ranked = (if (curMeets) meets.filter { it.score > curScore } else meets)
        .sortedByDescending { it.score }
        .take(MAX_GOAL_OPTIONS)

    val options = ranked.map { scored ->
        val candidate = scored.candidate
        val frameGenOption = frameGens.firstOrNull { it.mult == candidate.frameGen }
        GoalOption(
            state = candidate,
            result = scored.result,
            presetName = presets.first { it.key == candidate.preset }.name,
            upscalerName = db.upscalerName(gpu, candidate.upscaler),
            frameGenLabel = frameGenOption?.takeIf { it.mult > 1.0 }?.name ?: "",
            turnsRtOff = candidate.rt != state.rt,
        )
    }

    return GoalAdvice(
        target = target,
        currentAvg = cur.avg,
        bestPossibleAvg = if (meets.isEmpty()) all.maxByOrNull { it.result.avg }?.result?.avg else null,
        alreadyAtBestQuality = meets.isNotEmpty() && options.isEmpty(),
        options = options,
    )
}

/** Ranking do upscaler pela leveza: nativo/DLAA pesa mais que Performance. */
private fun upscalerRank(mult: Double): Int = when {
    mult <= 0.66 -> 5
    mult <= 0.93 -> 4
    mult <= 1.001 -> 3
    else -> 2
}

/** Pontuação de uma combinação: preset alto > upscaler leve > sem Frame Gen > mesmo RT de hoje. */
private fun goalScore(
    presets: List<Preset>,
    presetKey: String,
    upscalerMult: Double,
    frameGenMult: Double,
    rt: RtSetting,
    referenceRt: RtSetting,
): Int {
    val presetIndex = presets.indexOfFirst { it.key == presetKey }
    val presetRank = if (presetIndex < 0) 0 else presets.size - presetIndex
    val frameGenRank = when {
        frameGenMult <= 1.0 -> 60
        frameGenMult < 2.0 -> 25
        else -> 0
    }
    val rtRank = if (rt == referenceRt) 8 else 0
    return presetRank * 1000 + upscalerRank(upscalerMult) * 100 + frameGenRank + rtRank
}
