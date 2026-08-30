package com.fps.calculadora.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.fps.calculadora.core.CatalogCache
import com.fps.calculadora.core.CatalogFailure
import com.fps.calculadora.core.CatalogUpdate
import com.fps.calculadora.core.CatalogUpdater
import com.fps.calculadora.core.GameDatabase
import com.fps.calculadora.core.RemotePrices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** De onde veio a base que está na tela agora. */
enum class CatalogSource {
    /** A que veio dentro do APK. */
    BUNDLED,

    /** Um download anterior, lido do disco. */
    CACHED,

    /** Baixada agora. */
    NETWORK,
}

/** O catálogo em uso e o que a tela precisa saber sobre ele. */
data class CatalogState(
    val database: GameDatabase,
    val source: CatalogSource,
    /** Data ISO de publicação do catálogo, quando não é o embutido. */
    val updatedOn: String? = null,
    val prices: RemotePrices? = null,
    /** Peças que apareceram desde a versão instalada — o "tem peça nova". */
    val newPartNames: List<String> = emptyList(),
    /** Por que a última tentativa falhou, se falhou. `null` quando deu certo. */
    val lastFailure: CatalogFailure? = null,
) {
    val hasPrices: Boolean get() = prices?.byId?.isNotEmpty() == true
}

/**
 * Une cache, rede e base embutida numa fonte só para a UI.
 *
 * A ordem de preferência é sempre a mesma — rede, cache, embutido —, e a queda
 * de um nível para o outro é silenciosa por projeto: o FPS Calculadora é um app
 * offline que fica melhor com internet, não um app online que tolera ficar sem.
 * Nenhuma falha daqui pode impedir alguém de calcular FPS.
 *
 * O único efeito visível de uma falha é a tela continuar mostrando o que já
 * mostrava, e [CatalogState.lastFailure] ficar preenchido para quem quiser
 * explicar isso em algum canto discreto.
 */
class CatalogRepository(private val context: Context) {

    private val cache = CatalogCache(File(context.filesDir, CACHE_DIR))
    private val updater = CatalogUpdater()

    /**
     * O que dá para mostrar imediatamente, sem tocar na rede.
     *
     * Roda na abertura do app: lê o disco e devolve o último catálogo bom. Se
     * o cache estiver corrompido — gravação interrompida, versão anterior com
     * outro formato — cai para a base embutida e apaga o arquivo ruim, em vez
     * de insistir num estado quebrado a cada abertura.
     */
    fun loadCached(): CatalogState {
        val cached = cache.read() ?: return bundled()
        return when (val applied = updater.apply(cached.raw, cached.etag)) {
            is CatalogUpdate.Applied -> applied.toState(CatalogSource.CACHED)
            is CatalogUpdate.Failed -> {
                Log.w(TAG, "cache inutilizável (${applied.reason}), voltando à base do APK")
                cache.clear()
                bundled().copy(lastFailure = applied.reason)
            }
            CatalogUpdate.UpToDate -> bundled()
        }
    }

    /**
     * Tenta melhorar [current] com uma ida à rede.
     *
     * Devolve o próprio [current] quando não há o que fazer: sem conexão, ou
     * cache ainda dentro das 12 horas. Assim a chamada é barata de repetir e a
     * tela não precisa saber quando é hora de atualizar.
     */
    suspend fun refresh(current: CatalogState, force: Boolean = false): CatalogState =
        withContext(Dispatchers.IO) {
            val cached = cache.read()
            if (!force && cached != null && !cached.isStale(System.currentTimeMillis())) return@withContext current

            // Perguntar ao sistema antes de abrir socket evita um timeout de
            // 10 segundos no modo avião só para descobrir o óbvio.
            if (!isOnline()) return@withContext current.copy(lastFailure = CatalogFailure.NETWORK)

            when (val update = updater.update(etag = cached?.etag)) {
                is CatalogUpdate.Applied -> {
                    runCatching { cache.write(update.raw, update.etag, System.currentTimeMillis()) }
                        .onFailure { Log.w(TAG, "catálogo baixado mas não guardado", it) }
                    update.toState(CatalogSource.NETWORK)
                }

                // 304: o arquivo não mudou. Renova a data do cache para não
                // perguntar de novo daqui a pouco.
                CatalogUpdate.UpToDate -> {
                    if (cached != null) {
                        runCatching { cache.write(cached.raw, cached.etag, System.currentTimeMillis()) }
                    }
                    current
                }

                is CatalogUpdate.Failed -> {
                    Log.i(TAG, "catálogo não atualizado: ${update.reason}")
                    current.copy(lastFailure = update.reason)
                }
            }
        }

    private fun bundled() = CatalogState(database = GameDatabase.default, source = CatalogSource.BUNDLED)

    private fun CatalogUpdate.Applied.toState(source: CatalogSource) = CatalogState(
        database = result.database,
        source = source,
        updatedOn = result.database.meta.updated,
        prices = result.prices,
        newPartNames = result.newCpus.map { it.name } + result.newGpus.map { it.name },
        lastFailure = null,
    )

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object {
        const val TAG = "FpsCatalogo"
        const val CACHE_DIR = "catalogo"
    }
}
