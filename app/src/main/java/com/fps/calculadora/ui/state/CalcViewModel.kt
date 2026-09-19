package com.fps.calculadora.ui.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.fps.calculadora.core.ProfileBook
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
import com.fps.calculadora.core.normalizeOrNull
import com.fps.calculadora.core.parseBuildCode
import com.fps.calculadora.core.rankAllGames
import com.fps.calculadora.core.renamed
import com.fps.calculadora.core.repaired
import com.fps.calculadora.core.shareText
import com.fps.calculadora.core.switchedTo
import com.fps.calculadora.core.toPersisted
import com.fps.calculadora.core.upgradeAdvice
import com.fps.calculadora.core.warningsFor
import com.fps.calculadora.core.withActive
import com.fps.calculadora.core.withNewEntry
import com.fps.calculadora.core.withNewProfile
import com.fps.calculadora.core.without
import com.fps.calculadora.data.CatalogRepository
import com.fps.calculadora.data.CatalogState
import com.fps.calculadora.data.HistoryStore
import com.fps.calculadora.data.PreferencesStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
    private val preferencesStore = PreferencesStore(application)

    var state by mutableStateOf(db.normalize(defaultState()))
        private set

    var hoursPerDay by mutableIntStateOf(DEFAULT_HOURS_PER_DAY)

    var tariff by mutableDoubleStateOf(DEFAULT_TARIFF_BRL)

    /**
     * Os perfis de PC. O ativo é onde a build atual mora — ver [profiles], que
     * é o que a tela lê, e [syncedBook], que é o que vai pro disco.
     */
    private var book by mutableStateOf(ProfileBook.of(state, 0L))

    /**
     * `false` até o disco responder. Segura a splash (ver `ComposeMainActivity`)
     * e, principalmente, impede que o padrão seja gravado por cima do que estava
     * salvo caso a leitura demore.
     */
    var restored by mutableStateOf(false)
        private set

    /** `true` na primeira abertura, enquanto as boas-vindas não forem dispensadas. */
    var onboardingPending by mutableStateOf(false)
        private set

    /**
     * Os perfis com o ativo já refletindo o que está na tela — é assim que a
     * folha "Perfis de PC" mostra a build de agora embaixo do nome.
     */
    val profiles: ProfileBook get() = syncedBook()

    val activeProfileName: String get() = book.active.name

    init {
        viewModelScope.launch { restore() }
        // Grava sozinho a cada mudança, com uma pausa antes: sem ela o arquivo
        // seria reescrito a cada passo do slider de horas. O `restored` garante
        // que nada seja gravado antes de sabermos o que já havia lá.
        viewModelScope.launch {
            snapshotFlow { if (restored) syncedBook() else null }
                .filterNotNull()
                // `collectLatest` + `delay` é o debounce: cada valor novo
                // cancela a espera do anterior, e só o último chega ao disco.
                .collectLatest { book ->
                    delay(SAVE_DEBOUNCE_MS)
                    preferencesStore.saveProfiles(book)
                }
        }
    }

    /**
     * Devolve o app ao estado da última sessão. Qualquer falha de leitura vira
     * "primeira abertura": perder o perfil salvo é ruim, não abrir é pior.
     */
    private suspend fun restore() {
        val stored = try {
            preferencesStore.preferences.first()
        } catch (e: Exception) {
            null
        }
        val restoredBook = stored?.profiles.repaired(state, System.currentTimeMillis())
        book = restoredBook
        // Direto, sem `applyState`: reabrir o app não é uma mudança de build, e
        // a seta de variação do gauge não deve aparecer na abertura.
        restoredBook.active.let { profile ->
            db.normalizeOrNull(profile.state)?.let { state = it }
            hoursPerDay = profile.hoursPerDay
            tariff = profile.tariff
        }
        onboardingPending = stored?.onboarded == false
        restored = true
    }

    private fun syncedBook(): ProfileBook = book.withActive {
        it.copy(state = state.toPersisted(), hoursPerDay = hoursPerDay, tariff = tariff)
    }

    /** Aplica o perfil escolhido — troca build, horas e tarifa de uma vez. */
    private fun activate(next: ProfileBook) {
        book = next
        val profile = next.active
        db.normalizeOrNull(profile.state)?.let { applyState(it) }
        hoursPerDay = profile.hoursPerDay
        tariff = profile.tariff
    }

    /** Guarda o que está na tela no perfil atual antes de sair dele. */
    fun switchProfile(id: Long) {
        if (id == book.activeId) return
        activate(syncedBook().switchedTo(id))
    }

    /** "+ Novo perfil (cópia do atual)" — o novo já entra ativo. */
    fun newProfile() {
        book = syncedBook().withNewProfile(System.currentTimeMillis())
    }

    fun renameProfile(id: Long, name: String) {
        book = syncedBook().renamed(id, name)
    }

    /**
     * Exclui um perfil. Excluir o ativo promove o primeiro que sobrar e troca a
     * build na tela; o último perfil não é excluído (a folha nem oferece).
     */
    fun deleteProfile(id: Long) {
        val next = syncedBook().without(id)
        if (next.activeId != book.activeId) activate(next) else book = next
    }

    /** Dispensa as boas-vindas — o `dismissOnboard()` do original (`index.html:3135`). */
    fun dismissOnboarding() {
        onboardingPending = false
        viewModelScope.launch { preferencesStore.setOnboarded(true) }
    }

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

    /** O resumo em texto da build atual — o que acompanha o PNG e o botão "Copiar texto". */
    fun currentShareText(): String = db.shareText(state, calculator.calc(state), calculator.psu(state))

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

    /**
     * A build de uma entrada do histórico, normalizada contra a base atual —
     * `null` quando ela cita peça que o catálogo não conhece (mais).
     *
     * O histórico é o único dado do app que atravessa uma troca de catálogo
     * sem passar pelo `normalize`: as entradas vêm do disco com os ids de
     * quando foram salvas. Ler por aqui é o que impede um id órfão de derrubar
     * a tela dentro de `db.game()`/`db.cpu()`, que estouram em id desconhecido.
     */
    fun historyBuild(entry: HistoryEntry): BuildState? = db.normalizeOrNull(entry.state)

    /**
     * Carrega um build salvo como a build atual — porta a ação "Carregar" do
     * card de histórico. Devolve `false` sem mexer em nada quando a build não
     * é mais legível (ver [historyBuild]).
     */
    fun loadHistoryEntry(entry: HistoryEntry): Boolean {
        val build = historyBuild(entry) ?: return false
        applyState(build)
        return true
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
        /** Quanto esperar antes de gravar no disco, em ms — ver o `init`. */
        const val SAVE_DEBOUNCE_MS = 400L

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
