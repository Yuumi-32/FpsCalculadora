package com.fps.calculadora.core

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Onde o catálogo é publicado. Mesmo GitHub Pages que serve a política de privacidade. */
const val DEFAULT_CATALOG_URL = "https://yuumi-32.github.io/FpsCalculadora/catalogo.json"

/**
 * Teto do corpo da resposta.
 *
 * O catálogo real fica na casa das dezenas de KB. Meio mega é folga larga e,
 * ainda assim, impede que uma resposta inesperada (uma página de erro do CDN,
 * um arquivo trocado) seja lida inteira para dentro da memória do aparelho.
 */
const val MAX_CATALOG_BYTES = 512 * 1024

/** O que voltou do servidor. */
sealed interface CatalogResponse {
    data class Body(val text: String, val etag: String?) : CatalogResponse
    /** HTTP 304: o arquivo não mudou desde o ETag que mandamos. */
    data object NotModified : CatalogResponse
}

/**
 * A saída de rede, isolada atrás de uma interface por um motivo prático: o
 * resto do fluxo de atualização — parse, validação, fusão, recusa de schema —
 * é onde moram os erros, e nada disso precisa de socket para ser testado.
 */
fun interface CatalogTransport {
    /** @throws IOException em qualquer falha de rede ou resposta não-200/304. */
    fun fetch(url: String, etag: String?): CatalogResponse
}

/**
 * A implementação real, sobre [HttpURLConnection] — que é `java.net`, roda
 * igual na JVM e no Android, e evita puxar OkHttp para dentro de um projeto
 * que hoje não tem nenhuma dependência de rede.
 */
class HttpCatalogTransport(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 15_000,
) : CatalogTransport {

    override fun fetch(url: String, etag: String?): CatalogResponse {
        val parsed = URL(url)
        // Cinto e suspensório junto com o network_security_config: aquele
        // proíbe texto claro no app inteiro, este garante que nem por
        // configuração errada o catálogo saia por http://.
        if (!parsed.protocol.equals("https", ignoreCase = true)) {
            throw IOException("catálogo só por https, veio: ${parsed.protocol}")
        }

        val conn = (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            requestMethod = "GET"
            // O padrão do HttpURLConnection já recusa redirecionamento que
            // troca de protocolo, então seguir redirect não abre caminho para
            // um downgrade silencioso para http.
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            // Nada aqui identifica o aparelho ou a pessoa: só o ETag do
            // arquivo que já temos, que é do arquivo, não de quem pede.
            if (!etag.isNullOrBlank()) setRequestProperty("If-None-Match", etag)
        }

        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) return CatalogResponse.NotModified
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            val text = conn.inputStream.readBoundedText(MAX_CATALOG_BYTES)
            return CatalogResponse.Body(text, conn.getHeaderField("ETag"))
        } finally {
            conn.disconnect()
        }
    }
}

/** Lê no máximo [max] bytes, falhando em vez de engolir uma resposta gigante. */
internal fun InputStream.readBoundedText(max: Int): String {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (true) {
        val n = read(buffer)
        if (n < 0) break
        if (out.size() + n > max) throw IOException("catálogo maior que ${max / 1024} KB")
        out.write(buffer, 0, n)
    }
    return out.toString(Charsets.UTF_8.name())
}

/** Por que uma atualização não pegou — o suficiente para a tela dizer algo útil. */
enum class CatalogFailure {
    /** Sem rede, timeout, servidor fora, resposta não-200. */
    NETWORK,
    /** Baixou, mas não é JSON válido ou falta campo obrigatório. */
    MALFORMED,
    /** Baixou e é válido, mas o formato é de uma versão que este app não entende. */
    INCOMPATIBLE,
}

/** O desfecho de uma tentativa de atualização. */
sealed interface CatalogUpdate {
    /** Deu certo: [result] tem a base nova, os preços e as peças que apareceram. */
    data class Applied(
        val result: CatalogMergeResult,
        val etag: String?,
        /** O JSON cru, para o chamador guardar em cache. */
        val raw: String,
    ) : CatalogUpdate

    /** O servidor respondeu 304 — o que está em cache continua valendo. */
    data object UpToDate : CatalogUpdate

    data class Failed(val reason: CatalogFailure, val cause: Throwable? = null) : CatalogUpdate
}

/**
 * Junta transporte, parse e fusão numa operação que **nunca lança**.
 *
 * Essa é a regra que importa aqui: o FPS Calculadora é um app que funciona
 * offline e só fica melhor com rede. Uma falha de download não pode ter
 * qualquer efeito além de a tela continuar mostrando os dados que já tinha —
 * então tudo o que pode dar errado vira um [CatalogUpdate.Failed] devolvido, e
 * não uma exceção subindo até a UI.
 */
class CatalogUpdater(
    private val base: GameDatabase = GameDatabase.default,
    private val transport: CatalogTransport = HttpCatalogTransport(),
    private val url: String = DEFAULT_CATALOG_URL,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** @param etag o do último download bem-sucedido, ou `null` na primeira vez. */
    fun update(etag: String? = null): CatalogUpdate {
        val response = try {
            transport.fetch(url, etag)
        } catch (e: Exception) {
            return CatalogUpdate.Failed(CatalogFailure.NETWORK, e)
        }

        if (response is CatalogResponse.NotModified) return CatalogUpdate.UpToDate
        val body = response as CatalogResponse.Body

        return apply(body.text, body.etag)
    }

    /**
     * Interpreta um JSON de catálogo já em mãos — de download ou de cache.
     *
     * Público porque a leitura do cache no início do app passa exatamente pelo
     * mesmo caminho de validação: um arquivo salvo por uma versão anterior
     * merece a mesma desconfiança que um recém-baixado.
     */
    fun apply(rawJson: String, etag: String? = null): CatalogUpdate = try {
        val catalog = json.decodeFromString<RemoteCatalog>(rawJson)
        CatalogUpdate.Applied(base.merge(catalog), etag, rawJson)
    } catch (e: CatalogSchemaException) {
        CatalogUpdate.Failed(CatalogFailure.INCOMPATIBLE, e)
    } catch (e: Exception) {
        CatalogUpdate.Failed(CatalogFailure.MALFORMED, e)
    }
}
