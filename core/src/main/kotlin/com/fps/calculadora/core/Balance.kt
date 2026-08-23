package com.fps.calculadora.core

import kotlin.math.max

/**
 * Quanto de cada peça está sendo usado — o que alimenta as barras "Equilíbrio
 * CPU × GPU". Porta o cálculo que estava dentro do `renderBneck()`
 * (`index.html:2762`), preso à montagem do HTML.
 *
 * A peça que limita fica em 100%; a outra mostra a fração que consegue
 * aproveitar. Sempre pelo menos 1%, para a barra nunca sumir de vez.
 */
data class Balance(
    val gpuLoad: Int,
    val cpuLoad: Int,
    /** `true` quando a CPU é o teto — a GPU sobra. */
    val cpuLimited: Boolean,
) {
    /** Quanto da GPU fica ocioso esperando a CPU. */
    val gpuIdle: Int get() = 100 - gpuLoad

    /** Folga que a CPU ainda tem antes de virar o gargalo. */
    val cpuHeadroom: Int get() = 100 - cpuLoad
}

/**
 * Divide a carga entre CPU e GPU a partir de um resultado já calculado.
 *
 * Não recalcula nada: usa o teto de CPU e o FPS que a GPU entregaria sozinha,
 * ambos já expostos por [CalcResult].
 */
fun CalcResult.balance(): Balance {
    val gpuLoad = if (cpuBottleneck) {
        max(1, jsRound(cpuCap.toDouble() / gpuFps * 100))
    } else {
        100
    }
    val cpuLoad = if (cpuBottleneck) {
        100
    } else {
        max(1, jsRound(gpuFps.toDouble() / cpuCap * 100))
    }
    return Balance(gpuLoad = gpuLoad, cpuLoad = cpuLoad, cpuLimited = cpuBottleneck)
}
