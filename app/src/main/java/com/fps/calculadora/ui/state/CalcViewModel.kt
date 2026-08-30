package com.fps.calculadora.ui.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fps.calculadora.core.Balance
import com.fps.calculadora.core.BuildPreset
import com.fps.calculadora.core.BuildState
import com.fps.calculadora.core.CalcResult
import com.fps.calculadora.core.CompareRow
import com.fps.calculadora.core.DEFAULT_HOURS_PER_DAY
import com.fps.calculadora.core.DEFAULT_TARIFF_BRL
import com.fps.calculadora.core.EnergyEstimate
import com.fps.calculadora.core.FpsCalculator
import com.fps.calculadora.core.GameCompareSummary
import com.fps.calculadora.core.GameDatabase
import com.fps.calculadora.core.GameRankEntry
import com.fps.calculadora.core.GamesSort
import com.fps.calculadora.core.GoalAdvice
import com.fps.calculadora.core.GoalOption
import com.fps.calculadora.core.HistoryEntry
import com.fps.calculadora.core.PerformanceTier
import com.fps.calculadora.core.PsuEstimate
import com.fps.calculadora.core.Resolution
import com.fps.calculadora.core.RtSetting
import com.fps.calculadora.core.UpgradeAdvice
import com.fps.calculadora.core.balance
import com.fps.calculadora.core.buildCode
import com.fps.calculadora.core.byResolution
import com.fps.calculadora.core.compareAllGames
import com.fps.calculadora.core.compareGame
import com.fps.calculadora.core.energyFor
import com.fps.calculadora.core.goalAdvice
import com.fps.calculadora.core.normalize
import com.fps.calculadora.core.parseBuildCode
import com.fps.calculadora.core.rankAllGames
import com.fps.calculadora.core.toBuildState
import com.fps.calculadora.core.upgradeAdvice
import com.fps.calculadora.core.warningsFor
import com.fps.calculadora.core.withNewEntry
import com.fps.calculadora.data.CatalogRepository
import com.fps.calculadora.data.CatalogState
import com.fps.calculadora.data.HistoryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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

/** Modo de comparação da aba Comparar builds — o `cmpMode` do `index.html` (:2630). */
enum class CompareMode { GAME, ALL_GAMES }

/**
 * Guarda o estado da build (compartilhado por todas as abas) e deriva os
 * resultados de cada uma. Não faz conta nenhuma: tudo vem do `:core`. O papel
 * aqui é segurar a escolha do usuário, normalizá-la a cada mudança, lembrar o
 * valor anterior para a seta de variação do gauge, e falar com o histórico
 * local (`DataStore`).
 */
class CalcViewModel(application: Application) : AndroidViewModel(application) {

    private val catalogRepo = CatalogRepository(application)

    /**
     * O catálogo em uso. Começa com o que houver em cache (leitura de disco,
     * rápida) e é trocado quando a rede traz coisa nova.
     */
    var catalog by mutableStateOf(catalogRepo.loadCached())
        private set

    /**
     * A base atual. Deixou de ser constante quando o catálogo remoto entrou:
     * as telas leem por aqui e recompõem sozinhas quando ele troca.
     *
     * A troca é segura no meio do uso porque peça nova só é anexada ao fim da
     * lista — nenhum id que a `state` referencia deixa de existir.
     */
    val db: GameDatabase get() = catalog.database

    private var calculator = FpsCalculator(catalog.database)
    private val historyStore = HistoryStore(application)

    var state by mutableStateOf(db.normalize(defaultState()))
        private set

    var hoursPerDay by mutableIntStateOf(DEFAULT_HOURS_PER_DAY)

    var tariff by mutableDoubleStateOf(DEFAULT_TARIFF_BRL)

    /** Último FPS exibido, para o gauge mostrar de quanto foi o salto. */
    var previousFps by mutableStateOf<Int?>(null)
        private set

    /** Ordenação da aba "Seu PC em todos os jogos" — não persiste entre sessões, igual ao original. */
    var gamesSort by mutableStateOf(GamesSort.FPS)

    /** Modo da aba Comparar builds. */
    var compareMode by mutableStateOf(CompareMode.GAME)

    /** `id` do build do histórico escolhido como B na aba Comparar — `null` até o usuário escolher um. */
    var compareBuildId by mutableStateOf<Long?>(null)

    val historyEntries: Flow<List<HistoryEntry>> = historyStore.entries

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

    fun gamesRanking(): List<GameRankEntry> = calculator.rankAllGames(state, gamesSort)

    fun upgradeAdvice(): UpgradeAdvice = calculator.upgradeAdvice(state)

    fun goalAdvice(target: Int): GoalAdvice = calculator.goalAdvice(state, target)

    /** Aplica uma opção da "Meta de FPS" como a build atual. */
    fun applyGoalOption(option: GoalOption) = applyState(option.state)

    /** Aplica um card de "Builds prontos" — porta o clique do `.pcard` (`index.html:2503`). */
    fun applyBuildPreset(preset: BuildPreset) = applyState(db.normalize(preset.applyTo(state)))

    fun compareGame(buildB: BuildState): List<CompareRow> = calculator.compareGame(state, buildB)

    fun compareAllGames(buildB: BuildState): GameCompareSummary = calculator.compareAllGames(state, buildB)

    /** FPS médio de uma build qualquer — usado pra rotular builds do histórico sem trocar a build atual. */
    fun fpsFor(build: BuildState): Int = calculator.calc(build).avg

    fun exportCurrentBuildCode(): String = db.buildCode(state)

    /** `true` quando o código era válido e a build atual foi trocada por ele. */
    fun importBuildCode(code: String): Boolean {
        val parsed = db.parseBuildCode(code) ?: return false
        applyState(parsed)
        return true
    }

    /**
     * Aplica uma mudança e renormaliza — é o que impede combinações impossíveis
     * (DDR5 numa AM4, RT numa GTX) de sobreviverem a uma troca de peça.
     */
    fun update(transform: (BuildState) -> BuildState) {
        val next = db.normalize(transform(state))
        if (next == state) return
        applyState(next)
    }

    fun clearPreviousFps() {
        previousFps = null
    }

    /**
     * Tenta melhorar o catálogo pela rede — preços mais novos, peças que
     * lançaram.
     *
     * Barato de chamar: o repositório devolve o estado atual sem tocar em
     * socket quando não há conexão ou quando o cache ainda está dentro das 12
     * horas. Por isso a tela pode chamar na abertura sem se preocupar em
     * saber quando é hora.
     *
     * Não expõe carregamento nem erro na UI de propósito. O app calcula FPS
     * sem depender disso; transformar uma falha de rede em algo que o usuário
     * precise ver ou dispensar seria dar à atualização uma importância que ela
     * não tem.
     */
    fun refreshCatalog(force: Boolean = false) {
        viewModelScope.launch {
            val next = catalogRepo.refresh(catalog, force)
            if (next !== catalog) applyCatalog(next)
        }
    }

    private fun applyCatalog(next: CatalogState) {
        catalog = next
        calculator = FpsCalculator(next.database)
        // Renormaliza contra a base nova. A build atual continua válida —
        // peça só é anexada, nunca removida —, mas o normalize é o que faz
        // uma peça recém-chegada passar a ser oferecida nos seletores.
        state = next.database.normalize(state)
    }

    /** Salva a build atual no histórico local — porta o `saveBuild()` (`index.html:2519`). */
    fun saveCurrentBuild() {
        val id = System.currentTimeMillis()
        viewModelScope.launch {
            historyStore.update { it.withNewEntry(state, id) }
        }
    }

    /** Carrega um build salvo como a build atual — porta a ação "Carregar" do card de histórico. */
    fun loadHistoryEntry(entry: HistoryEntry) {
        applyState(db.normalize(entry.state.toBuildState()))
    }

    /** Remove um build do histórico — porta a ação "Excluir" (`index.html:2570`). */
    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            historyStore.update { entries -> entries.filterNot { it.id == id } }
        }
        if (compareBuildId == id) compareBuildId = null
    }

    private fun applyState(next: BuildState) {
        previousFps = calculator.calc(state).avg
        state = next
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
