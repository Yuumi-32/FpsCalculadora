package com.fps.calculadora.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

private const val CODE_PREFIX = "FPS1."
private val CODE_PATTERN = Regex("FPS1\\.([A-Za-z0-9+/=]+)")
private val codeJson = Json { ignoreUnknownKeys = true }
private val VALID_RESOLUTION_KEYS = setOf("1080p", "1440p", "4k")

/**
 * O payload do código de texto — mesmas chaves curtas do `buildCode()`
 * original (`index.html:3278`). Hardware vai por **nome**, não por índice:
 * um código compartilhado não pode quebrar se o catálogo for reordenado.
 * Todo campo tem default pra nunca falhar a decodificação por causa de um
 * campo ausente — um código inválido vira `null` só depois, ao resolver o
 * hardware contra o catálogo local.
 */
@Serializable
private data class BuildCodePayload(
    @SerialName("g") val game: String = "",
    @SerialName("c") val cpu: String = "",
    @SerialName("p") val gpu: String = "",
    @SerialName("m") val mobo: String = "",
    @SerialName("r") val ram: String = "",
    @SerialName("e") val res: String = "",
    @SerialName("q") val preset: String = "",
    @SerialName("t") val rt: String = "",
    @SerialName("f") val fg: Double = 1.0,
    @SerialName("d") val dlss: Double = 1.0,
    @SerialName("h") val hz: Int = 144,
)

/**
 * Gera o código de texto pra compartilhar um build — porta 1:1 o
 * `buildCode()` do `index.html` (:3278). Mesmo idioma de "base64 seguro pra
 * UTF-8" do `btoa(unescape(encodeURIComponent(...)))` original.
 */
fun GameDatabase.buildCode(state: BuildState): String {
    val payload = BuildCodePayload(
        game = game(state.gameId).name,
        cpu = cpu(state.cpuId).name,
        gpu = gpu(state.gpuId).name,
        mobo = mobo(state.moboId).name,
        ram = state.ram,
        res = state.resolution.key,
        preset = state.preset,
        rt = state.rt.key,
        fg = state.frameGen,
        dlss = state.upscaler,
        hz = state.monitorHz,
    )
    val json = codeJson.encodeToString(payload)
    val base64 = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
    return CODE_PREFIX + base64
}

/**
 * Decodifica um código gerado por [buildCode]. `null` quando o texto não tem
 * um código reconhecível, o JSON está corrompido, ou o jogo/CPU/GPU citado
 * não existe (mais) no catálogo local — porta o `parseCode()`
 * (`index.html:3283`), inclusive o regex tolerante (o código pode vir colado
 * junto de outro texto) e os defaults de campos inválidos.
 */
fun GameDatabase.parseBuildCode(code: String): BuildState? {
    val match = CODE_PATTERN.find(code.trim()) ?: return null
    val payload = try {
        val json = String(Base64.getDecoder().decode(match.groupValues[1]), Charsets.UTF_8)
        codeJson.decodeFromString<BuildCodePayload>(json)
    } catch (e: Exception) {
        return null
    }

    val foundGame = games.firstOrNull { it.name == payload.game } ?: return null
    val foundCpu = cpus.firstOrNull { it.name == payload.cpu } ?: return null
    val foundGpu = gpus.firstOrNull { it.name == payload.gpu } ?: return null
    val foundMobo = mobos.firstOrNull { it.name == payload.mobo } ?: moboAt(0)

    val resolution = if (payload.res in VALID_RESOLUTION_KEYS) {
        Resolution.fromKey(payload.res)
    } else {
        Resolution.QHD
    }
    val preset = if (constants.presets.any { it.key == payload.preset }) payload.preset else "ultra"
    val rt = RtSetting.entries.firstOrNull { it.key == payload.rt } ?: RtSetting.OFF

    return normalize(
        BuildState(
            gameId = foundGame.id,
            cpuId = foundCpu.id,
            gpuId = foundGpu.id,
            moboId = foundMobo.id,
            ram = payload.ram,
            resolution = resolution,
            preset = preset,
            rt = rt,
            frameGen = payload.fg,
            upscaler = payload.dlss,
            monitorHz = if (payload.hz > 0) payload.hz else 144,
        )
    )
}
