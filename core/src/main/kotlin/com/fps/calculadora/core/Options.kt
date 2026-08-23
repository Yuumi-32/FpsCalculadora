package com.fps.calculadora.core

/**
 * Opções que dependem do hardware escolhido, e a normalização que conserta
 * combinações impossíveis (RT numa GTX 1060, DDR5 numa AM4, Frame Gen numa
 * Pascal). Porta 1:1 de `ramOptionsFor` / `rtOptionsFor` / `fgOptionsFor` /
 * `dlssOptionsFor` / `normalizeS` do `index.html`.
 */

data class RamOption(val key: String, val name: String, val group: String)

data class RtOptions(
    /** `false` quando o jogo não tem RT algum — o seletor some da tela. */
    val show: Boolean,
    val label: String,
    val options: List<RtOption>,
)

data class RtOption(val setting: RtSetting, val name: String)

data class FrameGenOption(val mult: Double, val name: String)

/** Memórias compatíveis com o socket, na ordem do catálogo. */
fun GameDatabase.ramOptionsFor(socket: Socket): List<RamOption> {
    val labels = constants.ramLabels
    val ddr4Group = "DDR4 (${socket.name})"
    val ddr4 = labels.filterKeys { it.startsWith("ddr4_") }
        .map { (key, name) -> RamOption(key, name, ddr4Group) }
    val ddr5 = labels.filterKeys { it.startsWith("ddr5_") }
        .map { (key, name) ->
            val group = if (key.contains("4800")) "DDR5 mínimo — 4800 MT/s" else "DDR5 padrão — 5200 MT/s"
            RamOption(key, name, group)
        }
    return when {
        !socket.supportsDdr5 -> ddr4
        !socket.supportsDdr4 -> ddr5
        else -> ddr4 + ddr5
    }
}

/** Modos de RT que este jogo oferece nesta GPU. */
fun GameDatabase.rtOptionsFor(game: Game, gpu: Gpu): RtOptions {
    if (game.rtMode == RtMode.NONE) return RtOptions(show = false, label = "Ray Tracing", options = emptyList())

    val hasRt = gpu.gen.hasRtCores
    val options = mutableListOf<RtOption>()
    var label = "Ray Tracing"

    when (game.rtMode) {
        RtMode.PATH_TRACING -> {
            if (gpu.gen.hasRayReconstruction) {
                options += RtOption(RtSetting.FULL, game.rrLabel ?: "Path Tracing + RR")
            }
            if (hasRt) options += RtOption(RtSetting.STANDARD, game.rtLabel ?: "Ray Tracing")
            options += RtOption(RtSetting.OFF, "Desativado")
        }
        RtMode.RAY_TRACING -> {
            if (hasRt) options += RtOption(RtSetting.STANDARD, game.rtLabel ?: "Ray Tracing Ligado")
            options += RtOption(RtSetting.OFF, "RT Desativado")
        }
        RtMode.LUMEN -> {
            // Lumen é o único que roda sem RT cores, pelo caminho de software.
            label = "Lumen"
            if (hasRt) options += RtOption(RtSetting.FULL, "Hardware Lumen")
            options += RtOption(RtSetting.STANDARD, "Software Lumen")
            options += RtOption(RtSetting.OFF, "Lumen Off")
        }
        RtMode.NONE -> Unit
    }
    return RtOptions(show = true, label = label, options = options)
}

fun GameDatabase.frameGenOptionsFor(gpu: Gpu): List<FrameGenOption> = buildList {
    add(FrameGenOption(1.0, "Desativado"))
    if (gpu.gen.hasFrameGen) {
        add(FrameGenOption(1.78, if (gpu.gen.isRadeon) "FSR 3 Frame Gen 2×" else "2× Frame Gen"))
    }
    if (gpu.gen.hasMultiFrameGen) add(FrameGenOption(3.15, "4× MFG (RTX 50)"))
}

/**
 * Corrige um estado para a combinação válida mais próxima. É o que permite
 * trocar de CPU AM5 para AM4 sem ficar com DDR5 numa placa que não aceita.
 */
fun GameDatabase.normalize(state: BuildState): BuildState {
    val cpu = cpu(state.cpuId)
    val gpu = gpu(state.gpuId)
    val game = game(state.gameId)
    var result = state

    if (ramOptionsFor(cpu.socket).none { it.key == result.ram }) {
        result = result.copy(ram = if (cpu.socket == Socket.AM4) "ddr4_32" else "ddr5_32")
    }

    val compatibleMobos = mobosFor(cpu.socket)
    if (compatibleMobos.none { it.id == result.moboId }) {
        result = result.copy(moboId = compatibleMobos.first().id)
    }

    val rtOptions = rtOptionsFor(game, gpu)
    result = when {
        !rtOptions.show -> result.copy(rt = RtSetting.OFF)
        rtOptions.options.none { it.setting == result.rt } ->
            result.copy(rt = rtOptions.options.first().setting)
        else -> result
    }

    if (frameGenOptionsFor(gpu).none { it.mult == result.frameGen }) {
        result = result.copy(frameGen = 1.0)
    }

    if (upscalersFor(gpu).none { it.mult == result.upscaler }) {
        // Sem RT cores sobra só o modo nativo (0.60); com RT cores, o balanceado (1.00).
        result = result.copy(upscaler = if (gpu.gen.hasRtCores) 1.0 else 0.60)
    }

    return result
}
