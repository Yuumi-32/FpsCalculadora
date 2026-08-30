package com.fps.calculadora.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A fusão do catálogo remoto com a base embutida.
 *
 * O teste que mais importa aqui é o de índice: builds salvas e códigos
 * compartilhados referenciam hardware por posição de array, então uma peça
 * nova inserida no meio da lista converte silenciosamente a máquina guardada
 * de alguém em outra. É um bug que não dá erro em lugar nenhum — só devolve o
 * PC errado.
 */
class RemoteCatalogTest {

    private val base = GameDatabase.default

    private fun catalog(
        schema: Int = CATALOG_SCHEMA,
        prices: Map<String, Double> = emptyMap(),
        newCpus: List<NewCpu> = emptyList(),
        newGpus: List<NewGpu> = emptyList(),
    ) = RemoteCatalog(
        schema = schema,
        version = "2.1",
        updated = "2026-08-30",
        prices = RemotePrices(sampledOn = "2026-08-30", method = "mediana de varejo", byId = prices),
        newCpus = newCpus,
        newGpus = newGpus,
    )

    private val rtx5080 = NewGpu(
        id = "rtx-5080-super", group = "RTX 50 (Blackwell)", name = "RTX 5080 Super",
        mult = 1.62, vram = 24.0, gen = GpuGen.RTX50, watts = 400,
    )

    @Test
    fun `formato desconhecido e recusado inteiro`() {
        // Meio catálogo aplicado é pior que catálogo nenhum: a base ficaria
        // num estado que ninguém projetou.
        val erro = assertFailsWith<CatalogSchemaException> { base.merge(catalog(schema = 99)) }
        assertEquals(99, erro.found)
    }

    @Test
    fun `peca nova entra no fim e nao desloca o que ja existia`() {
        val antes = base.gpus.map { it.id to it.index }
        val r = base.merge(catalog(newGpus = listOf(rtx5080)))

        // Nenhum índice antigo se moveu — é isto que mantém build salva válida.
        for ((id, index) in antes) {
            assertEquals(index, r.database.gpu(id).index, "índice de $id mudou")
        }
        // E a nova ficou no fim, com o índice seguinte.
        val nova = r.database.gpu("rtx-5080-super")
        assertEquals(base.gpus.size, nova.index)
        assertEquals(base.gpus.size + 1, r.database.gpus.size)
    }

    @Test
    fun `peca nova aparece no resultado para a tela poder avisar`() {
        val r = base.merge(catalog(newGpus = listOf(rtx5080)))
        assertTrue(r.hasNewParts)
        assertEquals(1, r.newPartCount)
        assertEquals("RTX 5080 Super", r.newGpus.single().name)
    }

    @Test
    fun `id que ja existe nao entra de novo como novidade`() {
        val duplicada = rtx5080.copy(id = base.gpus.first().id)
        val r = base.merge(catalog(newGpus = listOf(duplicada)))
        assertTrue(r.newGpus.isEmpty())
        assertEquals(base.gpus.size, r.database.gpus.size)
    }

    @Test
    fun `preco chega na peca certa`() {
        val r = base.merge(catalog(prices = mapOf("gtx-1050" to 620.0, "ryzen-5-1600" to 380.0)))
        assertEquals(620.0, r.database.gpu("gtx-1050").averagePriceBrl)
        assertEquals(380.0, r.database.cpu("ryzen-5-1600").averagePriceBrl)
        assertEquals(2, r.pricedParts)
        // Quem não recebeu preço continua sem preço, não com zero.
        assertNull(r.database.gpu("gtx-1060-3gb").averagePriceBrl)
    }

    @Test
    fun `zero a mais no preco e recusado em vez de exibido`() {
        // O erro real de editar tabela de preço à mão. R$ 42.000 numa GTX 1050
        // passaria pelo parser sem reclamar.
        val r = base.merge(catalog(prices = mapOf("gtx-1050" to 620_000.0, "ryzen-5-1600" to -5.0)))
        assertNull(r.database.gpu("gtx-1050").averagePriceBrl)
        assertNull(r.database.cpu("ryzen-5-1600").averagePriceBrl)
        assertEquals(listOf("gtx-1050", "ryzen-5-1600"), r.rejectedPrices)
        assertEquals(0, r.pricedParts)
    }

    @Test
    fun `preco de peca inexistente e reportado em vez de sumir`() {
        // Normalmente é erro de digitação no id. Sumir em silêncio faria o
        // preço "não aparecer" sem ninguém saber por quê.
        val r = base.merge(catalog(prices = mapOf("rtx-9090-ti" to 9_000.0)))
        assertEquals(listOf("rtx-9090-ti"), r.unknownPriceIds)
    }

    @Test
    fun `peca nova ja chega com preco`() {
        val r = base.merge(catalog(
            prices = mapOf("rtx-5080-super" to 9_400.0),
            newGpus = listOf(rtx5080),
        ))
        assertEquals(9_400.0, r.database.gpu("rtx-5080-super").averagePriceBrl)
        assertTrue(r.unknownPriceIds.isEmpty())
    }

    @Test
    fun `catalogo remoto nao mexe no miolo do calculo`() {
        val r = base.merge(catalog(prices = mapOf("gtx-1050" to 620.0), newGpus = listOf(rtx5080)))
        // Jogos, placas-mãe e constantes de cálculo são os mesmos objetos: o
        // arquivo remoto não tem como alterar o resultado de uma estimativa
        // para hardware que já existia.
        assertEquals(base.games, r.database.games)
        assertEquals(base.mobos, r.database.mobos)
        assertEquals(base.constants.presets, r.database.constants.presets)
        assertEquals(base.constants.systemWatts, r.database.constants.systemWatts)
        assertEquals(base.gpu("gtx-1050").mult, r.database.gpu("gtx-1050").mult)
        // Só a versão exibida muda.
        assertEquals("2.1", r.database.meta.version)
    }

    @Test
    fun `base embutida segue intacta depois da fusao`() {
        base.merge(catalog(prices = mapOf("gtx-1050" to 620.0), newGpus = listOf(rtx5080)))
        assertNull(GameDatabase.default.gpu("gtx-1050").averagePriceBrl)
        assertEquals(base.gpus.size, GameDatabase.default.gpus.size)
    }
}
