package com.fps.calculadora.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Resolução de tela. A `key` é a mesma usada nas tabelas JSON. */
enum class Resolution(val key: String, val label: String) {
    @SerialName("1080p") FHD("1080p", "1080P"),
    @SerialName("2560x1080") UWFHD("2560x1080", "2560×1080"),
    @SerialName("1440p") QHD("1440p", "1440P"),
    @SerialName("3440x1440") UWQHD("3440x1440", "3440×1440"),
    @SerialName("4k") UHD("4k", "4K");

    companion object {
        fun fromKey(key: String): Resolution =
            entries.firstOrNull { it.key == key } ?: error("resolução desconhecida: $key")
    }
}

/** Como o jogo implementa iluminação por ray tracing. */
@Serializable
enum class RtMode {
    /** Path tracing / ray reconstruction disponível. */ @SerialName("pt") PATH_TRACING,
    /** Ray tracing convencional. */ @SerialName("rt") RAY_TRACING,
    /** Unreal Engine Lumen (tem caminho por software). */ @SerialName("lumen") LUMEN,
    /** Jogo sem RT algum. */ @SerialName("none") NONE,
}

/** O que o usuário escolheu no seletor de RT. */
enum class RtSetting(val key: String) {
    /** Path tracing + Ray Reconstruction, ou Hardware Lumen. */ FULL("rr"),
    /** Ray tracing padrão, ou Software Lumen. */ STANDARD("rt"),
    OFF("off");

    companion object {
        fun fromKey(key: String): RtSetting =
            entries.firstOrNull { it.key == key } ?: error("modo de RT desconhecido: $key")
    }
}

/** Geração da GPU — determina RT cores, Frame Generation e upscaler. */
@Serializable
enum class GpuGen {
    @SerialName("gtx10") GTX10, @SerialName("gtx16") GTX16,
    @SerialName("rtx20") RTX20, @SerialName("rtx30") RTX30,
    @SerialName("rtx40") RTX40, @SerialName("rtx50") RTX50,
    @SerialName("rdna2") RDNA2, @SerialName("rdna3") RDNA3, @SerialName("rdna4") RDNA4,
    /** Arc A-Series. */ @SerialName("arca") ARCA,
    /** Arc B-Series — primeira com XeSS Frame Generation. */ @SerialName("arcb") ARCB;

    val isRadeon: Boolean get() = this == RDNA2 || this == RDNA3 || this == RDNA4
    val isArc: Boolean get() = this == ARCA || this == ARCB

    /** Pascal e Turing-sem-RT não têm núcleos de ray tracing. */
    val hasRtCores: Boolean get() = this != GTX10 && this != GTX16

    /** Path Tracing completo com Ray Reconstruction: só Ada e Blackwell. */
    val hasRayReconstruction: Boolean get() = this == RTX40 || this == RTX50

    val hasFrameGen: Boolean get() = this == RTX40 || this == RTX50 || this == RDNA3 || this == RDNA4 || this == ARCB

    /** Multi Frame Generation 4×, exclusivo da série RTX 50. */
    val hasMultiFrameGen: Boolean get() = this == RTX50
}

@Serializable
enum class Socket {
    @SerialName("AM4") AM4, @SerialName("AM5") AM5,
    @SerialName("LGA1200") LGA1200,
    @SerialName("LGA1700") LGA1700, @SerialName("LGA1851") LGA1851;

    /** LGA1700 é a única plataforma que aceita DDR4 e DDR5. */
    val supportsDdr4: Boolean get() = this == AM4 || this == LGA1200 || this == LGA1700
    val supportsDdr5: Boolean get() = this == AM5 || this == LGA1700 || this == LGA1851
}

@Serializable
data class Game(
    val id: String,
    /** Posição no array original do index.html — mantida para builds salvos antigos. */
    val index: Int,
    val group: String,
    val name: String,
    val rtMode: RtMode,
    /** Rótulo do modo RT completo, ex. "Path Tracing + RR". */
    val rrLabel: String? = null,
    val rtLabel: String? = null,
    /** Jogo pesado de streaming/CPU — sofre mais com 16 GB de RAM. */
    val heavy: Boolean = false,
    /** Teto de frames que a CPU de referência entrega; ausente = [DEFAULT_CPU_CAP]. */
    val cpuCap: Double? = null,
    /** FPS de referência (RTX 5070) por resolução e modo de RT. */
    val reference: Map<String, Map<String, Double>>,
    /** VRAM necessária em GB por resolução. */
    val vram: Map<String, Double>,
) {
    fun referenceFps(res: Resolution, rt: RtSetting): Double =
        reference[res.key]?.get(rt.key) ?: reference[res.key]?.get(RtSetting.OFF.key) ?: FALLBACK_FPS

    fun vramNeeded(res: Resolution): Double = vram[res.key] ?: FALLBACK_VRAM

    fun rtLabelFor(rt: RtSetting): String = when (rt) {
        RtSetting.FULL -> rrLabel ?: "RT completo"
        RtSetting.STANDARD -> rtLabel ?: "Ray Tracing"
        RtSetting.OFF -> "sem RT"
    }

    companion object {
        const val DEFAULT_CPU_CAP = 155.0
        const val FALLBACK_FPS = 100.0
        const val FALLBACK_VRAM = 8.0
    }
}

@Serializable
data class Cpu(
    val id: String, val index: Int, val group: String, val name: String,
    /** Desempenho relativo ao Ryzen 7 5700X de referência (1.00). */
    val mult: Double,
    val socket: Socket,
    val watts: Int,
)

@Serializable
data class Gpu(
    val id: String, val index: Int, val group: String, val name: String,
    /** Desempenho relativo à RTX 5070 de referência (1.00). */
    val mult: Double,
    /** VRAM em GB. */
    val vram: Double,
    val gen: GpuGen,
    val watts: Int,
)

@Serializable
data class Mobo(
    val id: String, val index: Int, val group: String, val name: String,
    val socket: Socket,
    /** Quanto o VRM da placa sustenta o boost da CPU (1.00 = sem perda). */
    val mult: Double,
)

@Serializable
data class Preset(val key: String, val name: String, val mult: Double)

@Serializable
data class Upscaler(
    val key: String, val name: String, val mult: Double,
    /** Só DLSS: exige RT cores. */
    val needsRtx: Boolean = false,
)

@Serializable
data class DbMeta(val version: String, val updated: String)

@Serializable
data class HzMarker(@SerialName("v") val value: Int, @SerialName("l") val label: String)

@Serializable
data class BuildPreset(
    val cpu: String, val gpu: String, val mobo: String,
    val cpuIndex: Int, val gpuIndex: Int, val moboIndex: Int,
    val ram: String, val res: String, val preset: String,
    val rt: String, val dlss: Double, val fg: Double,
) {
    /** Aplica o preset sobre um estado — jogo e monitor ficam como estavam (`Object.assign(st, p)` do JS). */
    fun applyTo(current: BuildState): BuildState = current.copy(
        cpuId = cpu, gpuId = gpu, moboId = mobo,
        ram = ram, resolution = Resolution.fromKey(res), preset = preset,
        rt = RtSetting.fromKey(rt), frameGen = fg, upscaler = dlss,
    )
}

@Serializable
data class Constants(
    val meta: DbMeta,
    /** Consumo do resto do sistema (placa-mãe, SSD, coolers) em watts. */
    val systemWatts: Int,
    val presets: List<Preset>,
    val upscalersNvidia: List<Upscaler>,
    val upscalersAmd: List<Upscaler>,
    val upscalersIntel: List<Upscaler>,
    val rtEfficiencyByGen: Map<GpuGen, Double>,
    /** Ajuste de VRAM por preset, relativo ao Ultra. */
    val vramByPreset: Map<String, Double>,
    val ramLabels: Map<String, String>,
    val hzMarkers: Map<String, List<HzMarker>>,
    val buildPresets: Map<String, BuildPreset>,
)

/**
 * Entradas de uma estimativa. Referencia hardware por **id estável**, não por
 * índice de array — inserir uma GPU no meio da lista não invalida mais builds
 * salvos nem códigos compartilhados.
 */
data class BuildState(
    val gameId: String,
    val cpuId: String,
    val gpuId: String,
    val moboId: String,
    /** Chave em [Constants.ramLabels], ex. "ddr4_32". */
    val ram: String,
    val resolution: Resolution,
    /** Chave em [Constants.presets], ex. "ultra". */
    val preset: String,
    val rt: RtSetting,
    /** Multiplicador de Frame Generation: 1.0, 1.78 (2×) ou 3.15 (4× MFG). */
    val frameGen: Double = 1.0,
    /** Multiplicador do upscaler (DLSS/FSR). */
    val upscaler: Double = 1.0,
    val monitorHz: Int = 144,
)
