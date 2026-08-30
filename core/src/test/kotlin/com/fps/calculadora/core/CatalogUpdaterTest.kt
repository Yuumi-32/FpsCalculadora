package com.fps.calculadora.core

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O contrato do [CatalogUpdater] é curto e vale mais que qualquer detalhe de
 * rede: **ele nunca lança**.
 *
 * O app funciona offline e só fica melhor com internet. Se uma falha de
 * download conseguir subir uma exceção até a UI, um app que deveria continuar
 * calculando normalmente quebra por causa de um Wi-Fi ruim. Por isso quase
 * todo teste aqui é uma forma diferente de dar errado.
 */
class CatalogUpdaterTest {

    private val validJson = """
        {
          "schema": 1,
          "version": "2.1",
          "updated": "2026-08-30",
          "prices": {
            "currency": "BRL",
            "sampledOn": "2026-08-30",
            "method": "mediana de anúncios de varejo",
            "byId": { "gtx-1050": 620.0 }
          },
          "newGpus": [
            {
              "id": "rtx-5080-super", "group": "RTX 50 (Blackwell)",
              "name": "RTX 5080 Super", "mult": 1.62, "vram": 24.0,
              "gen": "rtx50", "watts": 400
            }
          ]
        }
    """.trimIndent()

    private fun updater(transport: CatalogTransport) =
        CatalogUpdater(base = GameDatabase.default, transport = transport, url = "https://exemplo.test/c.json")

    @Test
    fun `download bom aplica precos e pecas novas`() {
        val u = updater { _, _ -> CatalogResponse.Body(validJson, "\"abc\"") }
        val r = assertIs<CatalogUpdate.Applied>(u.update())

        assertEquals(620.0, r.result.database.gpu("gtx-1050").averagePriceBrl)
        assertEquals("RTX 5080 Super", r.result.newGpus.single().name)
        assertEquals("\"abc\"", r.etag)
    }

    @Test
    fun `sem rede devolve falha em vez de estourar`() {
        val u = updater { _, _ -> throw IOException("host inacessível") }
        val r = assertIs<CatalogUpdate.Failed>(u.update())
        assertEquals(CatalogFailure.NETWORK, r.reason)
    }

    @Test
    fun `json quebrado nao derruba o app`() {
        val u = updater { _, _ -> CatalogResponse.Body("{ isto não é json", null) }
        val r = assertIs<CatalogUpdate.Failed>(u.update())
        assertEquals(CatalogFailure.MALFORMED, r.reason)
    }

    @Test
    fun `campo obrigatorio faltando e malformado, nao aplicado pela metade`() {
        val u = updater { _, _ -> CatalogResponse.Body("""{"version":"2.1","updated":"x"}""", null) }
        val r = assertIs<CatalogUpdate.Failed>(u.update())
        assertEquals(CatalogFailure.MALFORMED, r.reason)
    }

    @Test
    fun `formato de outra versao e distinguido de arquivo corrompido`() {
        // A distinção importa: MALFORMED sugere tentar de novo, INCOMPATIBLE
        // quer dizer "atualize o app" — mensagens diferentes na tela.
        val u = updater { _, _ -> CatalogResponse.Body(validJson.replace("\"schema\": 1", "\"schema\": 7"), null) }
        val r = assertIs<CatalogUpdate.Failed>(u.update())
        assertEquals(CatalogFailure.INCOMPATIBLE, r.reason)
    }

    @Test
    fun `304 nao reprocessa nada`() {
        val u = updater { _, etag ->
            assertEquals("\"anterior\"", etag, "o ETag guardado precisa ir no pedido")
            CatalogResponse.NotModified
        }
        assertIs<CatalogUpdate.UpToDate>(u.update(etag = "\"anterior\""))
    }

    @Test
    fun `primeira vez vai sem etag`() {
        val u = updater { _, etag ->
            assertNull(etag)
            CatalogResponse.Body(validJson, null)
        }
        assertIs<CatalogUpdate.Applied>(u.update())
    }

    @Test
    fun `cache passa pela mesma validacao do download`() {
        // Um arquivo salvo por uma versão anterior do app merece a mesma
        // desconfiança que um recém-baixado.
        val u = updater { _, _ -> throw IOException("não deveria ir à rede") }
        assertIs<CatalogUpdate.Applied>(u.apply(validJson))
        assertIs<CatalogUpdate.Failed>(u.apply("{}"))
    }

    @Test
    fun `resposta gigante e cortada em vez de lida inteira`() {
        val enorme = ByteArray(MAX_CATALOG_BYTES + 1) { 'a'.code.toByte() }
        val erro = kotlin.runCatching { enorme.inputStream().readBoundedText(MAX_CATALOG_BYTES) }
        assertTrue(erro.isFailure)
        assertIs<IOException>(erro.exceptionOrNull())
    }

    @Test
    fun `http puro e recusado antes de abrir conexao`() {
        val erro = kotlin.runCatching {
            HttpCatalogTransport().fetch("http://exemplo.test/c.json", null)
        }
        assertIs<IOException>(erro.exceptionOrNull())
    }
}
