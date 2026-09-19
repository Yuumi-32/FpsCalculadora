package com.fps.calculadora.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * O que este arquivo protege é a promessa de que **o app sempre abre e sempre
 * tem um perfil ativo**. O perfil ativo é onde a build atual mora: um livro sem
 * perfis, ou com um `activeId` que não existe, não é um estado ruim — é uma
 * tela em branco e um crash na abertura.
 *
 * Por isso os testes se concentram nos caminhos estranhos (disco corrompido,
 * exclusão do ativo, exclusão do último) e não no caminho feliz.
 */
class ProfilesTest {

    private val db = GameDatabase.default

    private fun buildState(gpuIndex: Int = 45) = db.normalize(
        BuildState(
            gameId = db.gameAt(2).id,
            cpuId = db.cpuAt(16).id,
            gpuId = db.gpuAt(gpuIndex).id,
            moboId = db.moboAt(3).id,
            ram = "ddr4_32",
            resolution = Resolution.QHD,
            preset = "ultra",
            rt = RtSetting.OFF,
        )
    )

    @Test
    fun `livro nulo ou vazio vira o perfil inicial`() {
        val state = buildState()

        val doNada = (null as ProfileBook?).repaired(state, 7L)
        assertEquals(1, doNada.list.size)
        assertEquals(ProfileBook.FIRST_PROFILE_NAME, doNada.active.name)
        assertEquals(7L, doNada.activeId)
        assertEquals(state.toPersisted(), doNada.active.state)

        val doVazio = ProfileBook(emptyList(), 99L).repaired(state, 7L)
        assertEquals(1, doVazio.list.size)
        assertEquals(7L, doVazio.activeId)
    }

    @Test
    fun `activeId orfao cai no primeiro perfil em vez de estourar`() {
        val book = ProfileBook.of(buildState(), 1L).withNewProfile(2L).copy(activeId = 999L)

        val fixed = book.repaired(buildState(), 3L)

        assertEquals(1L, fixed.activeId)
        assertEquals(2, fixed.list.size)
    }

    @Test
    fun `livro valido passa intacto pelo conserto`() {
        val book = ProfileBook.of(buildState(), 1L)
        assertSame(book, book.repaired(buildState(), 2L))
    }

    @Test
    fun `perfil novo e copia do atual e ja entra ativo`() {
        val original = ProfileBook.of(buildState(gpuIndex = 45), 1L)

        val book = original.withNewProfile(2L)

        assertEquals(2, book.list.size)
        assertEquals(2L, book.activeId)
        assertEquals("PC 2", book.active.name)
        assertEquals(original.active.state, book.active.state)
    }

    @Test
    fun `excluir o ativo promove o primeiro que sobrar`() {
        val book = ProfileBook.of(buildState(), 1L).withNewProfile(2L).withNewProfile(3L)
        assertEquals(3L, book.activeId)

        val after = book.without(3L)

        assertEquals(1L, after.activeId)
        assertEquals(listOf(1L, 2L), after.list.map { it.id })
    }

    @Test
    fun `excluir um inativo nao mexe em quem esta ativo`() {
        val book = ProfileBook.of(buildState(), 1L).withNewProfile(2L)

        val after = book.without(1L)

        assertEquals(2L, after.activeId)
        assertEquals(listOf(2L), after.list.map { it.id })
    }

    @Test
    fun `o ultimo perfil nao e excluido`() {
        val book = ProfileBook.of(buildState(), 1L)

        val after = book.without(1L)

        assertEquals(1, after.list.size)
        assertEquals(1L, after.activeId)
    }

    @Test
    fun `renomear corta no limite e ignora nome em branco`() {
        val book = ProfileBook.of(buildState(), 1L)

        val longo = book.renamed(1L, "  " + "x".repeat(40) + "  ")
        assertEquals(ProfileBook.NAME_MAX_LENGTH, longo.active.name.length)

        val branco = book.renamed(1L, "   ")
        assertEquals(ProfileBook.FIRST_PROFILE_NAME, branco.active.name)
    }

    @Test
    fun `trocar para um id inexistente nao troca nada`() {
        val book = ProfileBook.of(buildState(), 1L)
        assertEquals(1L, book.switchedTo(404L).activeId)
    }

    @Test
    fun `withActive so mexe no perfil ativo`() {
        val book = ProfileBook.of(buildState(gpuIndex = 45), 1L).withNewProfile(2L)
        val outra = buildState(gpuIndex = 38)

        val after = book.withActive { it.copy(state = outra.toPersisted(), hoursPerDay = 8) }

        assertEquals(outra.toPersisted(), after.active.state)
        assertEquals(8, after.active.hoursPerDay)
        val inativo = after.list.first { it.id == 1L }
        assertEquals(DEFAULT_HOURS_PER_DAY, inativo.hoursPerDay)
    }

    @Test
    fun `build salva que referencia peca desconhecida devolve nulo em vez de estourar`() {
        val ok = buildState().toPersisted()
        assertEquals(db.normalize(ok.toBuildState()), db.normalizeOrNull(ok))

        assertNull(db.normalizeOrNull(ok.copy(gpuId = "gpu-que-nunca-existiu")))
        assertNull(db.normalizeOrNull(ok.copy(resolution = "8k")))
    }
}
