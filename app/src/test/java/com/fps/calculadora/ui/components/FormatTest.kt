package com.fps.calculadora.ui.components

import com.fps.calculadora.core.BuildState
import com.fps.calculadora.core.GameDatabase
import com.fps.calculadora.core.Resolution
import com.fps.calculadora.core.RtSetting
import com.fps.calculadora.core.normalize
import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * As duas funções de texto que o `:app` tem fora do Compose. Parecem bobas até
 * lembrar que as duas escrevem em card de histórico de largura fixa: linha que
 * cresce sem aviso vira reticências em cima do nome da placa.
 */
class FormatTest {

    private val db = GameDatabase.default

    private fun state(gpuIndex: Int) = db.normalize(
        BuildState(
            gameId = db.gameAt(0).id,
            cpuId = db.cpuAt(0).id,
            gpuId = db.gpuAt(gpuIndex).id,
            moboId = db.moboAt(0).id,
            ram = "ddr4_16",
            resolution = Resolution.FHD,
            preset = "ultra",
            rt = RtSetting.OFF,
        )
    )

    @Test
    fun `a data do histórico sai em DD-MM HH-mm, sem ano`() {
        // Meia-noite de 5 de março, no fuso de quem estiver rodando — é o que a
        // formatação usa, e comparar com um instante fixo em UTC daria um dia de
        // diferença em metade do planeta.
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 5, 21, 7, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertEquals("05/03 21:07", formatHistoryDate(cal.timeInMillis))
    }

    @Test
    fun `a linha de hardware tem as três partes separadas por ponto medio`() {
        val line = buildHwLine(db, state(0))
        val parts = line.split(" · ")

        assertEquals(3, parts.size, "linha fora do formato: $line")
        assertTrue(parts.all { it.isNotBlank() }, "parte vazia em: $line")
        assertEquals(Resolution.FHD.label, parts[2])
    }

    @Test
    fun `nenhuma peça da base estoura a linha de hardware`() {
        // 58 caracteres é o que cabe em 10.5sp na largura do card num celular
        // de 360dp. Peça nova com nome comprido aparece aqui antes de aparecer
        // cortada no aparelho.
        for (index in db.gpus.indices) {
            val line = buildHwLine(db, state(index))
            assertTrue(line.length <= 58, "linha comprida demais (${line.length}): $line")
        }
    }
}
