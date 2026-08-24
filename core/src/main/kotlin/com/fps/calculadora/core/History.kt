package com.fps.calculadora.core

import kotlinx.serialization.Serializable

/**
 * Um build salvo no histórico local — porta a forma de cada item de
 * `loadHist()`/`saveBuild()` (`index.html:2513-2526`), agora referenciando
 * hardware por id estável em vez de índice (não há formato antigo a
 * preservar aqui: esse histórico nunca existiu no lado nativo).
 */
@Serializable
data class HistoryEntry(
    /** Epoch ms de quando foi salvo — também serve de identificador único. */
    val id: Long,
    val ts: Long,
    val state: PersistedBuildState,
)

/** [BuildState] em forma serializável — enums viram a `key` que já usam nos dados. */
@Serializable
data class PersistedBuildState(
    val gameId: String,
    val cpuId: String,
    val gpuId: String,
    val moboId: String,
    val ram: String,
    val resolution: String,
    val preset: String,
    val rt: String,
    val frameGen: Double = 1.0,
    val upscaler: Double = 1.0,
    val monitorHz: Int = 144,
)

fun BuildState.toPersisted(): PersistedBuildState = PersistedBuildState(
    gameId = gameId, cpuId = cpuId, gpuId = gpuId, moboId = moboId,
    ram = ram, resolution = resolution.key, preset = preset, rt = rt.key,
    frameGen = frameGen, upscaler = upscaler, monitorHz = monitorHz,
)

fun PersistedBuildState.toBuildState(): BuildState = BuildState(
    gameId = gameId, cpuId = cpuId, gpuId = gpuId, moboId = moboId,
    ram = ram, resolution = Resolution.fromKey(resolution), preset = preset,
    rt = RtSetting.fromKey(rt), frameGen = frameGen, upscaler = upscaler, monitorHz = monitorHz,
)

/** Quantos builds o histórico guarda no máximo — porta o `30` do `saveBuild()`. */
const val HISTORY_CAP = 30

/** Insere no topo (mais recente primeiro) e corta no limite — porta o corpo do `saveBuild()`. */
fun List<HistoryEntry>.withNewEntry(state: BuildState, id: Long): List<HistoryEntry> =
    (listOf(HistoryEntry(id = id, ts = id, state = state.toPersisted())) + this).take(HISTORY_CAP)
