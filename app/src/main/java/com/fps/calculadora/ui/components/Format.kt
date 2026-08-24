package com.fps.calculadora.ui.components

import com.fps.calculadora.core.BuildState
import com.fps.calculadora.core.GameDatabase
import com.fps.calculadora.core.shortCpuName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** `DD/MM HH:mm`, sem ano — porta o `fmtDate()` do `index.html` (:2527). */
fun formatHistoryDate(epochMs: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(epochMs))

/** `${shortCPU} · ${gpu} · ${resolução}` — porta o `buildHwLine()` (`index.html:2603`). */
fun buildHwLine(db: GameDatabase, state: BuildState): String {
    val cpu = db.cpu(state.cpuId)
    val gpu = db.gpu(state.gpuId)
    return "${shortCpuName(cpu.name)} · ${gpu.name} · ${state.resolution.key.uppercase()}"
}
