package com.fps.calculadora.core

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O cache é o que faz a rede ser opcional de verdade: sem ele, um aparelho
 * offline voltaria à base congelada do APK toda vez que o app abrisse.
 *
 * Por isso os testes se concentram em não perder o que já foi baixado — em
 * arquivo pela metade, meta corrompido e relógio errado.
 */
class CatalogCacheTest {

    private val dir: File = Files.createTempDirectory("catalogo-cache").toFile()
    private val cache = CatalogCache(dir)

    @AfterTest
    fun limpa() {
        dir.deleteRecursively()
    }

    @Test
    fun `sem cache devolve null em vez de explodir`() {
        assertNull(cache.read())
    }

    @Test
    fun `grava e le de volta`() {
        cache.write("""{"schema":1}""", "\"etag-1\"", fetchedAt = 1_000L)
        val lido = cache.read()!!
        assertEquals("""{"schema":1}""", lido.raw)
        assertEquals("\"etag-1\"", lido.etag)
        assertEquals(1_000L, lido.fetchedAt)
    }

    @Test
    fun `payload fica legivel no disco`() {
        // É o mesmo JSON publicado, não uma string escapada dentro de outro
        // JSON — quem for depurar consegue abrir o arquivo e ler.
        cache.write("""{"schema":1,"version":"2.1"}""", null, 0)
        assertEquals("""{"schema":1,"version":"2.1"}""", File(dir, CatalogCache.PAYLOAD).readText())
    }

    @Test
    fun `meta corrompido nao descarta o catalogo`() {
        cache.write("""{"schema":1}""", "\"etag-1\"", fetchedAt = 5_000L)
        File(dir, CatalogCache.META).writeText("lixo{{{")

        // Perder o ETag custa um download inteiro na próxima; perder o
        // catálogo custa o usuário voltar à base do APK. Só o primeiro é
        // degradação aceitável.
        val lido = cache.read()!!
        assertEquals("""{"schema":1}""", lido.raw)
        assertNull(lido.etag)
    }

    @Test
    fun `limpar apaga os dois arquivos`() {
        cache.write("""{"schema":1}""", "\"e\"", 1L)
        cache.clear()
        assertNull(cache.read())
        assertFalse(File(dir, CatalogCache.PAYLOAD).exists())
        assertFalse(File(dir, CatalogCache.META).exists())
    }

    @Test
    fun `sobrescrever nao deixa tmp para tras`() {
        cache.write("""{"schema":1,"version":"2.0"}""", "\"a\"", 1L)
        cache.write("""{"schema":1,"version":"2.1"}""", "\"b\"", 2L)
        assertEquals("""{"schema":1,"version":"2.1"}""", cache.read()!!.raw)
        assertTrue(dir.listFiles()!!.none { it.name.endsWith(".tmp") }, "sobrou arquivo temporário")
    }

    @Test
    fun `vencimento respeita o intervalo minimo`() {
        val hora = 60L * 60 * 1000
        val c = CachedCatalog(raw = "{}", etag = null, fetchedAt = 100 * hora)

        assertFalse(c.isStale(now = 106 * hora), "6h ainda não venceu")
        assertTrue(c.isStale(now = 112 * hora), "12h venceu")
    }

    @Test
    fun `relogio para tras nao congela a atualizacao`() {
        // Fuso horário ou ajuste manual do relógio deixaria `now - fetchedAt`
        // negativo. Sem este caso, o app pararia de atualizar para sempre.
        val c = CachedCatalog(raw = "{}", etag = null, fetchedAt = 1_000_000L)
        assertTrue(c.isStale(now = 5_000L))
    }

    @Test
    fun `cache sem data conta como vencido`() {
        // fetchedAt = 0 é o meta perdido: melhor tentar de novo que confiar.
        assertTrue(CachedCatalog(raw = "{}", etag = null, fetchedAt = 0).isStale(now = 1L))
    }
}
