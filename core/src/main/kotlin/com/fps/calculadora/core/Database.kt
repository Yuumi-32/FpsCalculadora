package com.fps.calculadora.core

import kotlinx.serialization.json.Json

/**
 * Catálogo de hardware e jogos, carregado dos JSON em `resources/data/`.
 *
 * Os JSON são gerados a partir do `index.html` por `tools/extract-data.mjs`.
 * A UI Compose já é a publicada e o HTML ficou só no build de debug, mas ele
 * continua sendo a fonte da verdade dos dados: é lá que se acrescenta hardware,
 * e daqui sai o espelho verificado pelos testes de paridade.
 *
 * [default] é a base que veio no APK. Desde o catálogo remoto ela deixou de ser
 * a única — ver [merge], que produz uma segunda a partir dela.
 */
class GameDatabase(
    val games: List<Game>,
    val cpus: List<Cpu>,
    val gpus: List<Gpu>,
    val mobos: List<Mobo>,
    val constants: Constants,
) {
    private val gamesById = games.associateBy { it.id }
    private val cpusById = cpus.associateBy { it.id }
    private val gpusById = gpus.associateBy { it.id }
    private val mobosById = mobos.associateBy { it.id }
    private val presetsByKey = constants.presets.associateBy { it.key }

    val meta: DbMeta get() = constants.meta

    fun game(id: String): Game = gamesById[id] ?: error("jogo desconhecido: $id")
    fun cpu(id: String): Cpu = cpusById[id] ?: error("CPU desconhecida: $id")
    fun gpu(id: String): Gpu = gpusById[id] ?: error("GPU desconhecida: $id")
    fun mobo(id: String): Mobo = mobosById[id] ?: error("placa-mãe desconhecida: $id")
    fun preset(key: String): Preset? = presetsByKey[key]

    /** Ponte com o formato antigo baseado em índice (builds salvos, códigos compartilhados). */
    fun gameAt(index: Int): Game = games[index]
    fun cpuAt(index: Int): Cpu = cpus[index]
    fun gpuAt(index: Int): Gpu = gpus[index]
    fun moboAt(index: Int): Mobo = mobos[index]

    /** Placas-mãe compatíveis com o socket da CPU, na ordem do catálogo. */
    fun mobosFor(socket: Socket): List<Mobo> = mobos.filter { it.socket == socket }

    /** Upscalers oferecidos pela GPU: FSR nas Radeon, XeSS nas Arc, DLSS nas GeForce (DLAA/DLSS só com RT cores). */
    fun upscalersFor(gpu: Gpu): List<Upscaler> = when {
        gpu.gen.isRadeon -> constants.upscalersAmd
        gpu.gen.isArc -> constants.upscalersIntel
        gpu.gen.hasRtCores -> constants.upscalersNvidia
        else -> constants.upscalersNvidia.filter { !it.needsRtx }
    }

    /** Nome legível do upscaler ativo, ou vazio se o multiplicador não bate com nenhum. */
    fun upscalerName(gpu: Gpu, mult: Double): String =
        upscalersFor(gpu).firstOrNull { it.mult == mult }?.name ?: ""

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private inline fun <reified T> load(file: String): T {
            val stream = GameDatabase::class.java.getResourceAsStream("/data/$file")
                ?: error("recurso não encontrado: /data/$file")
            return stream.bufferedReader().use { json.decodeFromString<T>(it.readText()) }
        }

        /** Instância única — os JSON são imutáveis, então não há motivo pra recarregar. */
        val default: GameDatabase by lazy {
            GameDatabase(
                games = load("games.json"),
                cpus = load("cpus.json"),
                gpus = load("gpus.json"),
                mobos = load("mobos.json"),
                constants = load("constants.json"),
            )
        }
    }
}
