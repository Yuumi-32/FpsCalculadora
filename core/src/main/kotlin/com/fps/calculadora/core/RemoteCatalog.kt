package com.fps.calculadora.core

import kotlinx.serialization.Serializable

/**
 * O arquivo que o app baixa de
 * `https://yuumi-32.github.io/FpsCalculadora/catalogo.json`.
 *
 * Ele existe para resolver os dois jeitos de a base envelhecer dentro de um
 * APK instalado, que são independentes e mudam em ritmos bem diferentes:
 *
 * - **preço** muda toda semana, com dólar e promoção;
 * - **peça nova** aparece a cada poucos meses, quando lança geração.
 *
 * Por isso os dois são campos separados em vez de um catálogo inteiro
 * reescrito: dá para publicar uma revisão de preços sem tocar em [newGpus], e
 * o arquivo continua editável à mão sem virar um despejo de 100 KB.
 *
 * O que **não** vem daqui: multiplicadores das peças já existentes, jogos,
 * placas-mãe e constantes de cálculo. Isso é o miolo do modelo, coberto pelos
 * testes de paridade, e uma troca remota conseguiria mudar o resultado do
 * cálculo sem passar por teste nenhum. Peça nova entra; regra de cálculo não.
 */
@Serializable
data class RemoteCatalog(
    /**
     * Versão do **formato**, não do conteúdo. Um arquivo com schema
     * desconhecido é recusado inteiro em vez de interpretado pela metade —
     * ver [CATALOG_SCHEMA].
     */
    val schema: Int,
    /** Versão do conteúdo, para exibição ("2.1"). */
    val version: String,
    /** Data ISO em que o catálogo foi publicado ("2026-08-30"). */
    val updated: String,
    val prices: RemotePrices = RemotePrices(),
    val newCpus: List<NewCpu> = emptyList(),
    val newGpus: List<NewGpu> = emptyList(),
)

/**
 * Os preços médios, com a procedência junto.
 *
 * [sampledOn] e [method] não são metadados decorativos: são o que permite a
 * tela dizer "média de agosto" em vez de deixar o usuário achar que o número
 * é de hoje. Preço sem data é preço mentiroso.
 */
@Serializable
data class RemotePrices(
    val currency: String = "BRL",
    /** Data ISO da amostragem. */
    val sampledOn: String = "",
    /** Como a média foi tirada, em uma linha, para aparecer na tela. */
    val method: String = "",
    /** `id da peça` → preço médio em reais. */
    val byId: Map<String, Double> = emptyMap(),
)

/** Uma CPU que ainda não existia quando este APK foi publicado. */
@Serializable
data class NewCpu(
    val id: String,
    val group: String,
    val name: String,
    val mult: Double,
    val socket: Socket,
    val watts: Int,
)

/** Uma GPU que ainda não existia quando este APK foi publicado. */
@Serializable
data class NewGpu(
    val id: String,
    val group: String,
    val name: String,
    val mult: Double,
    val vram: Double,
    val gen: GpuGen,
    val watts: Int,
)

/** Formato que este build entende. Ver [RemoteCatalog.schema]. */
const val CATALOG_SCHEMA = 1

/**
 * Teto de plausibilidade para um preço de peça avulsa, em reais.
 *
 * Não é segurança — o arquivo vem do próprio projeto, não de um estranho. É
 * proteção contra o erro que realmente acontece ao editar tabela de preço à
 * mão: o zero a mais. Um "42000" digitado no lugar de "4200" passaria pelo
 * parser sem reclamar e apareceria na tela como um preço plausível de PC
 * inteiro.
 */
const val MAX_PLAUSIBLE_PRICE_BRL = 100_000.0

/** O que a fusão do catálogo remoto com a base embutida produziu. */
data class CatalogMergeResult(
    /** A base já com preços e peças novas aplicados. */
    val database: GameDatabase,
    /**
     * A procedência dos preços, que viaja junto até a tela.
     *
     * Sem `sampledOn` ao lado do número, o usuário lê "≈ R$ 4.200" como o
     * preço de hoje. A data é o que transforma isso em "era isso quando
     * medimos" — e é a diferença entre uma média honesta e uma cotação
     * errada.
     */
    val prices: RemotePrices,
    /** Peças que não existiam na base embutida — o "tem peça nova no mercado". */
    val newCpus: List<Cpu>,
    val newGpus: List<Gpu>,
    /** Quantas peças ficaram com preço. */
    val pricedParts: Int,
    /** Ids cujo preço foi recusado por estar fora da faixa plausível. */
    val rejectedPrices: List<String>,
    /** Ids com preço no arquivo que não existem na base nem entraram como novos. */
    val unknownPriceIds: List<String>,
) {
    val hasNewParts: Boolean get() = newCpus.isNotEmpty() || newGpus.isNotEmpty()
    val newPartCount: Int get() = newCpus.size + newGpus.size
}

/** Erro de catálogo que vale distinguir de "não deu para baixar". */
class CatalogSchemaException(val found: Int) :
    Exception("catálogo em formato desconhecido: schema $found, este app entende $CATALOG_SCHEMA")

/**
 * Aplica [catalog] sobre esta base e devolve uma nova — [GameDatabase] continua
 * imutável, como sempre foi; o que muda é que agora existe mais de uma.
 *
 * As peças novas são **anexadas ao fim** dos arrays, nunca inseridas no meio.
 * Isso não é detalhe de implementação: builds salvos e códigos compartilhados
 * antigos referenciam hardware por índice de array (ver [Cpu.index]), então
 * inserir uma RTX 5080 no meio da lista transformaria silenciosamente a build
 * guardada de alguém em outra máquina.
 */
fun GameDatabase.merge(catalog: RemoteCatalog): CatalogMergeResult {
    if (catalog.schema != CATALOG_SCHEMA) throw CatalogSchemaException(catalog.schema)

    val knownCpuIds = cpus.mapTo(HashSet()) { it.id }
    val knownGpuIds = gpus.mapTo(HashSet()) { it.id }

    // Peça nova com id que já existe é ruído de edição, não peça nova.
    val addedCpus = catalog.newCpus
        .filter { it.id !in knownCpuIds && it.mult > 0 }
        .distinctBy { it.id }
        .mapIndexed { i, c ->
            Cpu(
                id = c.id, index = cpus.size + i, group = c.group, name = c.name,
                mult = c.mult, socket = c.socket, watts = c.watts,
            )
        }
    val addedGpus = catalog.newGpus
        .filter { it.id !in knownGpuIds && it.mult > 0 }
        .distinctBy { it.id }
        .mapIndexed { i, g ->
            Gpu(
                id = g.id, index = gpus.size + i, group = g.group, name = g.name,
                mult = g.mult, vram = g.vram, gen = g.gen, watts = g.watts,
            )
        }

    val rejected = mutableListOf<String>()
    val prices = buildMap {
        for ((id, value) in catalog.prices.byId) {
            if (value <= 0 || value > MAX_PLAUSIBLE_PRICE_BRL) rejected += id else put(id, value)
        }
    }

    val mergedCpus = (cpus + addedCpus).map { it.copy(averagePriceBrl = prices[it.id] ?: it.averagePriceBrl) }
    val mergedGpus = (gpus + addedGpus).map { it.copy(averagePriceBrl = prices[it.id] ?: it.averagePriceBrl) }

    val allIds = mergedCpus.mapTo(HashSet()) { it.id } + mergedGpus.mapTo(HashSet()) { it.id }
    val unknown = prices.keys.filter { it !in allIds }.sorted()

    return CatalogMergeResult(
        database = GameDatabase(
            games = games,
            cpus = mergedCpus,
            gpus = mergedGpus,
            mobos = mobos,
            constants = constants.copy(
                meta = DbMeta(version = catalog.version, updated = catalog.updated),
            ),
        ),
        prices = catalog.prices,
        newCpus = addedCpus,
        newGpus = addedGpus,
        // Contados em separado: `mergedCpus + mergedGpus` colapsaria para
        // List<Any> e o campo sumiria.
        pricedParts = mergedCpus.count { it.averagePriceBrl != null } +
            mergedGpus.count { it.averagePriceBrl != null },
        rejectedPrices = rejected.sorted(),
        unknownPriceIds = unknown,
    )
}
