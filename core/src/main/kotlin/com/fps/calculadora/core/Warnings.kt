package com.fps.calculadora.core

/**
 * Os avisos mostrados abaixo do resultado, na ordem do `renderAll()`
 * (`index.html:2279`).
 *
 * Quais aparecem e em que ordem é regra, não estilo — por isso mora aqui. A UI
 * só decide como desenhar a caixinha amarela.
 */
fun GameDatabase.warningsFor(state: BuildState, result: CalcResult): List<String> = buildList {
    val game = game(state.gameId)
    val cpu = cpu(state.cpuId)

    if (result.cpuBottleneck) {
        add("CPU: ${cpu.name} está limitando — teto estimado de ${result.cpuCap} FPS")
    }
    if (result.vramBottleneck) {
        add(
            "VRAM: jogo requer ~${jsNumber(result.vramNeeded)} GB, " +
                "GPU possui ${jsNumber(result.vramAvailable)} GB — penalidade aplicada"
        )
    }
    if (result.ramBottleneck) {
        add(
            if (game.heavy) {
                "RAM: 16 GB pode causar gagueira em \"${game.name}\" (recomendado: 32 GB)"
            } else {
                "RAM: 16 GB aplica penalidade leve (recomendado: 32 GB)"
            }
        )
    }
    if (result.gpuWarning.isNotEmpty()) add("GPU: ${result.gpuWarning}")
    if (result.moboWarning.isNotEmpty()) add("Placa-mãe: ${result.moboWarning}")
}

/**
 * Formata um número como o JavaScript faria ao concatenar numa string: sem
 * casa decimal quando o valor é inteiro.
 *
 * Sem isto, "GPU possui 8 GB" viraria "GPU possui 8.0 GB" — o tipo de
 * divergência que passa despercebida até alguém comparar as duas telas.
 */
internal fun jsNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
