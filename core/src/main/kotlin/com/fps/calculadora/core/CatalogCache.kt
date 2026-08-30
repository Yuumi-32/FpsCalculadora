package com.fps.calculadora.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * O catálogo baixado, guardado no armazenamento privado do app.
 *
 * O cache não é otimização de banda — é o que faz a rede ser opcional de
 * verdade. Sem ele, um aparelho offline voltaria à base congelada do APK toda
 * vez que abrisse o app, e o download da semana passada teria sido inútil. Com
 * ele, o app abre com o último catálogo conhecido e a rede vira só a tentativa
 * de melhorar isso.
 *
 * São dois arquivos em vez de um envelope só: o payload fica legível para
 * depuração (`catalogo.json` é o mesmo JSON publicado) em vez de virar uma
 * string escapada dentro de outro JSON.
 */
class CatalogCache(private val dir: File) {

    private val payload get() = File(dir, PAYLOAD)
    private val meta get() = File(dir, META)
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Meta(val etag: String? = null, val fetchedAt: Long = 0)

    /** O que está guardado, ou `null` se não há cache utilizável. */
    fun read(): CachedCatalog? {
        val raw = runCatching { payload.takeIf { it.isFile }?.readText() }.getOrNull() ?: return null
        // Meta corrompido não invalida o payload: sem ETag a gente só refaz o
        // download inteiro na próxima, o que é degradação aceitável.
        val m = runCatching { json.decodeFromString<Meta>(meta.readText()) }.getOrNull() ?: Meta()
        return CachedCatalog(raw = raw, etag = m.etag, fetchedAt = m.fetchedAt)
    }

    /**
     * Grava o catálogo recém-baixado.
     *
     * Escreve num `.tmp` e renomeia porque o processo pode morrer no meio — o
     * Android encerra app em segundo plano quando quer. Um `catalogo.json`
     * truncado pela metade seria lido como corrompido na próxima abertura e
     * jogaria o usuário de volta na base do APK sem motivo.
     */
    fun write(raw: String, etag: String?, fetchedAt: Long) {
        dir.mkdirs()
        writeAtomically(payload, raw)
        writeAtomically(meta, json.encodeToString(Meta(etag, fetchedAt)))
    }

    /** Some com o cache — o "limpar dados" do app passa por aqui. */
    fun clear() {
        payload.delete()
        meta.delete()
    }

    private fun writeAtomically(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(target)) {
            // Alguns sistemas de arquivos recusam rename por cima de arquivo
            // existente; a segunda tentativa cobre esse caso.
            target.delete()
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw java.io.IOException("não foi possível gravar ${target.name}")
            }
        }
    }

    companion object {
        const val PAYLOAD = "catalogo.json"
        const val META = "catalogo.meta.json"

        /**
         * Intervalo mínimo entre duas idas à rede: 12 horas.
         *
         * Preço de peça não muda a ponto de justificar consultar a cada
         * abertura, e o app é aberto muitas vezes seguidas por quem está
         * comparando build. Com o GET condicional por ETag, a maioria dessas
         * idas voltaria 304 de qualquer jeito — mas gastando bateria e dados
         * de alguém para descobrir isso.
         */
        const val DEFAULT_MIN_REFRESH_MS = 12L * 60 * 60 * 1000
    }
}

/** Catálogo em cache, com a procedência necessária para decidir se vale rebaixar. */
data class CachedCatalog(
    val raw: String,
    val etag: String?,
    /** Epoch millis do download. `0` quando o meta se perdeu. */
    val fetchedAt: Long,
) {
    /** Já passou tempo suficiente para valer a pena tentar de novo? */
    fun isStale(now: Long, minIntervalMs: Long = CatalogCache.DEFAULT_MIN_REFRESH_MS): Boolean =
        // Relógio para trás (fuso, ajuste manual) não pode congelar a
        // atualização para sempre: futuro impossível conta como vencido.
        fetchedAt <= 0 || now < fetchedAt || now - fetchedAt >= minIntervalMs
}
