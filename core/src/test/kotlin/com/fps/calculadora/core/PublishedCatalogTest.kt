package com.fps.calculadora.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * O arquivo que está publicado em `docs/catalogo.json` é válido para **esta**
 * versão do app?
 *
 * É a única falha do catálogo remoto que ninguém percebe: o app trata arquivo
 * ruim caindo de volta para a base embutida, em silêncio e por projeto. Quer
 * dizer que publicar um catálogo quebrado não gera erro, não gera relatório e
 * não gera reclamação — só faz o recurso parar de funcionar para todo mundo,
 * sem aviso.
 *
 * Este teste é o aviso. Ele roda com `./gradlew :core:test`, antes de o
 * arquivo chegar ao GitHub Pages.
 */
class PublishedCatalogTest {

    private val arquivo = File("../docs/catalogo.json")

    @Test
    fun `o catalogo publicado existe onde o app procura`() {
        assertTrue(
            arquivo.isFile,
            "docs/catalogo.json não encontrado — rode `node tools/gen-catalogo.mjs`",
        )
    }

    @Test
    fun `o catalogo publicado e aplicavel por esta versao`() {
        val update = CatalogUpdater().apply(arquivo.readText())

        // A mensagem importa: quem quebrar o arquivo precisa saber o que
        // aconteceu sem ter de ir depurar o parser.
        if (update is CatalogUpdate.Failed) {
            val detalhe = update.cause?.message ?: "sem detalhe"
            when (update.reason) {
                CatalogFailure.INCOMPATIBLE ->
                    error("catalogo.json usa um schema que este app não entende: $detalhe")
                CatalogFailure.MALFORMED ->
                    error("catalogo.json está malformado: $detalhe")
                CatalogFailure.NETWORK ->
                    error("inesperado: falha de rede lendo arquivo local")
            }
        }

        val aplicado = assertIs<CatalogUpdate.Applied>(update)

        // Preço com id que não existe é typo silencioso: o app ignora a
        // entrada e o preço simplesmente não aparece, sem ninguém saber por
        // quê.
        assertTrue(
            aplicado.result.unknownPriceIds.isEmpty(),
            "preços para ids inexistentes (typo?): ${aplicado.result.unknownPriceIds}",
        )
        // Preço fora da faixa plausível é quase sempre um zero a mais.
        assertTrue(
            aplicado.result.rejectedPrices.isEmpty(),
            "preços fora da faixa plausível: ${aplicado.result.rejectedPrices}",
        )
    }

    @Test
    fun `catalogo com preco declara quando foi levantado`() {
        val aplicado = CatalogUpdater().apply(arquivo.readText())
        if (aplicado !is CatalogUpdate.Applied) return

        // Preço sem data é preço mentiroso: a tela mostraria "≈ R$ 4.200" sem
        // nada indicando de quando é. Enquanto não houver preço nenhum, não há
        // o que datar.
        if (aplicado.result.prices.byId.isNotEmpty()) {
            assertTrue(
                aplicado.result.prices.sampledOn.isNotBlank(),
                "há preços em docs/catalogo.json mas prices.sampledOn está vazio — " +
                    "a data aparece na tela junto do valor",
            )
        }
    }
}
