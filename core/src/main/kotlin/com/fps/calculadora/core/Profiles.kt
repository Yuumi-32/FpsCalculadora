package com.fps.calculadora.core

import kotlinx.serialization.Serializable

/**
 * Perfis de PC — porta o bloco `fps:profiles:v1` do `index.html` (:3371).
 *
 * Um perfil é "o PC do qual estamos falando agora": guarda a build inteira mais
 * as duas entradas da conta de energia. O perfil ativo é também **onde a build
 * atual mora** — mexer num seletor grava no perfil ativo, que é o que faz o app
 * reabrir onde parou. Não existe estado "solto" fora de um perfil, exatamente
 * como no original (`persistState()` chama `syncProfile()`).
 *
 * A lógica vive no `:core`, e não no `DataStore`, porque é regra: qual perfil
 * fica ativo depois de excluir o ativo, o que acontece ao excluir o último,
 * quanto de nome cabe. Persistir é detalhe de plataforma; decidir isso não é.
 */
@Serializable
data class PcProfile(
    /** Epoch ms de quando foi criado — também identifica o perfil. */
    val id: Long,
    val name: String,
    val state: PersistedBuildState,
    val hoursPerDay: Int = DEFAULT_HOURS_PER_DAY,
    val tariff: Double = DEFAULT_TARIFF_BRL,
)

/**
 * A lista de perfis e qual está ativo. Nunca é válida vazia — ver [repaired],
 * que é por onde todo dado vindo do disco passa.
 */
@Serializable
data class ProfileBook(
    val list: List<PcProfile>,
    val activeId: Long,
) {
    /**
     * O perfil ativo. Cai no primeiro da lista quando o `activeId` não existe,
     * em vez de estourar — quem vem do disco já passou pelo [repaired], mas um
     * `copy()` mal feito no meio do caminho não deve derrubar o app.
     */
    val active: PcProfile get() = list.firstOrNull { it.id == activeId } ?: list.first()

    companion object {
        /** Nome do perfil que existe desde a primeira abertura. */
        const val FIRST_PROFILE_NAME = "Meu PC"

        /** O `maxlength="24"` do campo de renomear (`index.html:3423`). */
        const val NAME_MAX_LENGTH = 24

        /** Livro de um perfil só, com a build que o app abriu. */
        fun of(state: BuildState, id: Long): ProfileBook = ProfileBook(
            list = listOf(PcProfile(id = id, name = FIRST_PROFILE_NAME, state = state.toPersisted())),
            activeId = id,
        )
    }
}

/**
 * Devolve um livro utilizável a partir de qualquer coisa que tenha vindo do
 * disco: lista vazia vira o perfil inicial, `activeId` órfão cai no primeiro.
 * Porta as duas guardas do `loadProfiles()` (`index.html:3377-3381`).
 */
fun ProfileBook?.repaired(fallbackState: BuildState, fallbackId: Long): ProfileBook {
    if (this == null || list.isEmpty()) return ProfileBook.of(fallbackState, fallbackId)
    if (list.none { it.id == activeId }) return copy(activeId = list.first().id)
    return this
}

/** Grava a build atual no perfil ativo — o `syncProfile()` do original. */
fun ProfileBook.withActive(transform: (PcProfile) -> PcProfile): ProfileBook =
    copy(list = list.map { if (it.id == activeId) transform(it) else it })

/** Troca de perfil. Sem efeito quando o id não existe mais. */
fun ProfileBook.switchedTo(id: Long): ProfileBook =
    if (list.none { it.id == id }) this else copy(activeId = id)

/**
 * Acrescenta um perfil copiando o ativo e já o deixa ativo — o "+ Novo perfil
 * (cópia do atual)". O nome segue a contagem da lista, como no original.
 */
fun ProfileBook.withNewProfile(id: Long): ProfileBook {
    val copy = active.copy(id = id, name = "PC ${list.size + 1}")
    return copy(list = list + copy, activeId = id)
}

/** Renomeia. Nome em branco é ignorado; o excedente é cortado em [ProfileBook.NAME_MAX_LENGTH]. */
fun ProfileBook.renamed(id: Long, name: String): ProfileBook {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return this
    val short = trimmed.take(ProfileBook.NAME_MAX_LENGTH)
    return copy(list = list.map { if (it.id == id) it.copy(name = short) else it })
}

/**
 * Exclui um perfil. O último não pode ser excluído (o original nem oferece a
 * lixeira nesse caso); excluir o ativo promove o primeiro que sobrar, e é isso
 * que troca a build na tela.
 */
fun ProfileBook.without(id: Long): ProfileBook {
    val rest = list.filterNot { it.id == id }
    if (rest.isEmpty()) return this
    return ProfileBook(list = rest, activeId = if (activeId == id) rest.first().id else activeId)
}
