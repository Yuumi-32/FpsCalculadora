package com.fps.calculadora.core

/**
 * Custo mensal de energia da build. Porta o cálculo que estava dentro do
 * `renderEnergy()` (`index.html:3166`), misturado à montagem do HTML.
 *
 * @param gamingWatts carga típica jogando — não é o pico da fonte.
 * @param kwhPerMonth consumo mensal estimado.
 * @param monthlyCost custo em reais, já multiplicado pela tarifa.
 */
data class EnergyEstimate(
    val gamingWatts: Int,
    val kwhPerMonth: Double,
    val monthlyCost: Double,
)

/**
 * Carga real jogando: a GPU vai a quase 100%, a CPU fica bem abaixo do TDP e o
 * resto do sistema também não roda no máximo.
 *
 * Estes três fatores são **modelo**, não catálogo — por isso vivem aqui e não
 * nos JSON, na mesma lógica que separa a penalidade de VRAM dos dados.
 */
private const val CPU_LOAD_FACTOR = 0.6
private const val SYSTEM_LOAD_FACTOR = 0.8
private const val DAYS_PER_MONTH = 30

/** Padrões da UI quando o usuário ainda não escolheu nada. */
const val DEFAULT_HOURS_PER_DAY = 3
const val DEFAULT_TARIFF_BRL = 0.95

/**
 * Estima o custo mensal de energia de uma build.
 *
 * @param hoursPerDay horas de jogo por dia.
 * @param tariffBrlPerKwh tarifa em R$/kWh — varia muito por distribuidora.
 */
fun GameDatabase.energyFor(
    state: BuildState,
    hoursPerDay: Int = DEFAULT_HOURS_PER_DAY,
    tariffBrlPerKwh: Double = DEFAULT_TARIFF_BRL,
): EnergyEstimate {
    val gpu = gpu(state.gpuId)
    val cpu = cpu(state.cpuId)

    val gamingWatts = jsRound(
        gpu.watts + cpu.watts * CPU_LOAD_FACTOR + constants.systemWatts * SYSTEM_LOAD_FACTOR
    )
    val kwhPerMonth = gamingWatts / 1000.0 * hoursPerDay * DAYS_PER_MONTH
    return EnergyEstimate(
        gamingWatts = gamingWatts,
        kwhPerMonth = kwhPerMonth,
        monthlyCost = kwhPerMonth * tariffBrlPerKwh,
    )
}
