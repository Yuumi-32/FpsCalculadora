package com.fps.calculadora.core

import java.util.Locale

/**
 * O resumo em texto de uma build — porta 1:1 do `shareText()` do `index.html`
 * (:2768). É o que vai junto do PNG no compartilhamento e o que o botão
 * "Copiar texto" coloca na área de transferência.
 *
 * Mora no `:core` pelo mesmo motivo dos badges: o texto afirma números, e
 * afirmação é regra de produto. A tela só decide onde mostrar o botão.
 */
fun GameDatabase.shareText(state: BuildState, result: CalcResult, psu: PsuEstimate): String {
    val game = game(state.gameId)
    val cpu = cpu(state.cpuId)
    val gpu = gpu(state.gpuId)
    val presetName = preset(state.preset)?.name ?: state.preset

    val upscaler = upscalerName(gpu, state.upscaler)
    val rtOptions = rtOptionsFor(game, gpu)
    val rtName = if (state.rt != RtSetting.OFF && rtOptions.show) {
        rtOptions.options.firstOrNull { it.setting == state.rt }?.name.orEmpty()
    } else {
        ""
    }
    val frameGen = if (result.frameGenMult > 1) {
        "Frame Gen ×" + String.format(Locale.ROOT, "%.2f", result.frameGenMult)
    } else {
        ""
    }

    return listOf(
        "🎮 ${game.name} — ${state.resolution.label} $presetName",
        "${cpu.name} + ${gpu.name}",
        "${mobo(state.moboId).name} · ${constants.ramLabels[state.ram] ?: state.ram}",
        listOf(upscaler, rtName, frameGen).filter { it.isNotEmpty() }.joinToString(" · "),
        "",
        "▶ ${result.avg} FPS médio (${result.avgLow}–${result.avgHigh})",
        "1% low ${result.min} · máximo ${result.max} · monitor ${state.monitorHz} Hz",
        "🔌 Fonte recomendada: ${psu.recommended} W",
        "",
        "— Estimador de FPS",
    ).joinToString("\n")
}
