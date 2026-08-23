package com.fps.calculadora.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fps.calculadora.core.Balance
import com.fps.calculadora.core.BuildState
import com.fps.calculadora.core.CalcResult
import com.fps.calculadora.core.DEFAULT_HOURS_PER_DAY
import com.fps.calculadora.core.DEFAULT_TARIFF_BRL
import com.fps.calculadora.core.EnergyEstimate
import com.fps.calculadora.core.FpsCalculator
import com.fps.calculadora.core.GameDatabase
import com.fps.calculadora.core.PerformanceTier
import com.fps.calculadora.core.PsuEstimate
import com.fps.calculadora.core.Resolution
import com.fps.calculadora.core.RtSetting
import com.fps.calculadora.core.balance
import com.fps.calculadora.core.byResolution
import com.fps.calculadora.core.energyFor
import com.fps.calculadora.core.normalize
import com.fps.calculadora.core.warningsFor

/** Tudo que a tela Calcular mostra para um estado — calculado de uma vez só. */
data class CalcSnapshot(
    val state: BuildState,
    val result: CalcResult,
    val tier: PerformanceTier,
    val balance: Balance,
    val psu: PsuEstimate,
    val energy: EnergyEstimate,
    val warnings: List<String>,
    val byResolution: Map<Resolution, CalcResult>,
)

/**
 * Guarda o estado da build e deriva o resultado.
 *
 * Não faz conta nenhuma: tudo vem do `:core`. O papel aqui é segurar a escolha
 * do usuário, normalizá-la a cada mudança e lembrar o valor anterior para a
 * seta de variação do gauge.
 */
class CalcViewModel : ViewModel() {

    val db: GameDatabase = GameDatabase.default
    private val calculator = FpsCalculator(db)

    var state by mutableStateOf(db.normalize(defaultState()))
        private set

    var hoursPerDay by mutableIntStateOf(DEFAULT_HOURS_PER_DAY)

    var tariff by mutableDoubleStateOf(DEFAULT_TARIFF_BRL)

    /** Último FPS exibido, para o gauge mostrar de quanto foi o salto. */
    var previousFps by mutableStateOf<Int?>(null)
        private set

    val snapshot: CalcSnapshot
        get() {
            val result = calculator.calc(state)
            return CalcSnapshot(
                state = state,
                result = result,
                tier = PerformanceTier.forFps(result.avg),
                balance = result.balance(),
                psu = calculator.psu(state),
                energy = db.energyFor(state, hoursPerDay, tariff),
                warnings = db.warningsFor(state, result),
                byResolution = calculator.byResolution(state),
            )
        }

    /**
     * Aplica uma mudança e renormaliza — é o que impede combinações impossíveis
     * (DDR5 numa AM4, RT numa GTX) de sobreviverem a uma troca de peça.
     */
    fun update(transform: (BuildState) -> BuildState) {
        val before = calculator.calc(state).avg
        val next = db.normalize(transform(state))
        if (next == state) return
        previousFps = before
        state = next
    }

    fun clearPreviousFps() {
        previousFps = null
    }

    private companion object {
        /** O mesmo estado inicial do `index.html:1822`, resolvido por índice. */
        fun defaultState(): BuildState {
            val db = GameDatabase.default
            return BuildState(
                gameId = db.gameAt(2).id,
                cpuId = db.cpuAt(16).id,
                gpuId = db.gpuAt(45).id,
                moboId = db.moboAt(3).id,
                ram = "ddr4_32",
                resolution = Resolution.QHD,
                preset = "ultra",
                rt = RtSetting.OFF,
                frameGen = 1.0,
                upscaler = 1.0,
                monitorHz = 144,
            )
        }
    }
}
