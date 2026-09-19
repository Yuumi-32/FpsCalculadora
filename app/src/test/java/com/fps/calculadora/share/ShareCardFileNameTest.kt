package com.fps.calculadora.share

import com.fps.calculadora.core.BuildState
import com.fps.calculadora.core.GameDatabase
import com.fps.calculadora.core.Resolution
import com.fps.calculadora.core.RtSetting
import com.fps.calculadora.core.normalize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * O nome do PNG do compartilhamento vira nome de arquivo em dois lugares
 * hostis: o `DISPLAY_NAME` do MediaStore e o cache que o FileProvider publica.
 * Barra, dois-pontos ou acento em qualquer um dos dois é uma gravação que falha
 * no aparelho da pessoa e não aqui — daí este arquivo.
 *
 * Varre a base inteira de propósito: o nome sai do nome comercial da peça, e a
 * peça que quebra a regra é sempre a que ainda não foi cadastrada.
 */
class ShareCardFileNameTest {

    private val db = GameDatabase.default

    private fun nameFor(cpuIndex: Int = 0, gpuIndex: Int = 0): String = shareCardFileName(
        db,
        db.normalize(
            BuildState(
                gameId = db.gameAt(0).id,
                cpuId = db.cpuAt(cpuIndex).id,
                gpuId = db.gpuAt(gpuIndex).id,
                moboId = db.moboAt(0).id,
                ram = "ddr4_16",
                resolution = Resolution.FHD,
                preset = "ultra",
                rt = RtSetting.OFF,
            )
        ),
    )

    @Test
    fun `nenhuma peça da base produz nome fora do seguro`() {
        val nomes = db.gpus.indices.map { nameFor(gpuIndex = it) } +
            db.cpus.indices.map { nameFor(cpuIndex = it) }

        for (name in nomes) {
            assertTrue(SEGURO.matches(name), "nome fora do formato seguro: $name")
        }
    }

    @Test
    fun `builds diferentes geram nomes diferentes`() {
        assertNotEquals(nameFor(gpuIndex = 0), nameFor(gpuIndex = 1))
        assertNotEquals(nameFor(cpuIndex = 0), nameFor(cpuIndex = 1))
    }

    @Test
    fun `a mesma build gera sempre o mesmo nome`() {
        assertEquals(nameFor(cpuIndex = 2, gpuIndex = 3), nameFor(cpuIndex = 2, gpuIndex = 3))
    }

    private companion object {
        /** Minúsculas, dígitos e hífen simples — nada que precise de escape. */
        val SEGURO = Regex("""fps-estimado-[a-z0-9]+(-[a-z0-9]+)*\.png""")
    }
}
