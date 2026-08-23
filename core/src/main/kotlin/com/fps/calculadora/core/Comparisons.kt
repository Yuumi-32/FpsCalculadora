package com.fps.calculadora.core

/**
 * A mesma build nas três resoluções — alimenta o card "Nas três resoluções"
 * (`renderResCmp()`, `index.html:3145`).
 *
 * Normaliza cada variação antes de calcular, como o original faz. Trocar de
 * resolução não invalida nenhuma escolha hoje, mas manter a normalização deixa
 * o comportamento igual se isso mudar.
 */
fun FpsCalculator.byResolution(state: BuildState): Map<Resolution, CalcResult> =
    Resolution.entries.associateWith { resolution ->
        calc(db.normalize(state.copy(resolution = resolution)))
    }
