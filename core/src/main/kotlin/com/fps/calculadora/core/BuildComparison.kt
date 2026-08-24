package com.fps.calculadora.core

import kotlin.math.max

/**
 * Uma linha comparativa entre a build A (atual) e a build B (salva) — porta o
 * `cmpRow()` do `index.html` (:2607).
 *
 * @param higherIsBetter `false` só na fonte recomendada: menor consumo vence.
 */
data class CompareRow(
    val label: String,
    val valueA: Int,
    val valueB: Int,
    val unit: String,
    val higherIsBetter: Boolean,
) {
    /** Denominador das barras — nunca zero. */
    val maxValue: Int get() = max(max(valueA, valueB), 1)
    val aWins: Boolean get() = if (higherIsBetter) valueA > valueB else valueA < valueB
    val bWins: Boolean get() = if (higherIsBetter) valueB > valueA else valueB < valueA

    /** A variação de A em relação a B, em %. `null` quando são iguais ou B é zero. */
    val deltaPercent: Int? get() =
        if (valueA != valueB && valueB > 0) jsRound((valueA - valueB).toDouble() / valueB * 100) else null

    /** `true` quando o delta favorece A (cor verde no original). */
    val deltaFavorsA: Boolean get() = deltaPercent?.let { if (higherIsBetter) it > 0 else it < 0 } ?: false
}

/**
 * "Comparar builds", modo "Jogo atual" — porta o corpo padrão do
 * `renderComp()` (`index.html:2652`): FPS médio, 1% low, máximo e fonte
 * recomendada, A contra B, no jogo/preset ativos.
 */
fun FpsCalculator.compareGame(buildA: BuildState, buildB: BuildState): List<CompareRow> {
    val ra = calc(buildA)
    val rb = calc(buildB)
    val pa = psu(buildA)
    val pb = psu(buildB)
    return listOf(
        CompareRow("FPS médio", ra.avg, rb.avg, "", higherIsBetter = true),
        CompareRow("1% low", ra.min, rb.min, "", higherIsBetter = true),
        CompareRow("FPS máximo", ra.max, rb.max, "", higherIsBetter = true),
        CompareRow("Fonte recomendada", pa.recommended, pb.recommended, "W", higherIsBetter = false),
    )
}

/** Uma linha do modo "Todos os jogos": o mesmo jogo, A e B com seus próprios hardwares. */
data class GameCompareEntry(val game: Game, val avgA: Int, val avgB: Int, val deltaPercent: Int)

data class GameCompareSummary(
    val entries: List<GameCompareEntry>,
    /** Em quantos jogos B supera A. */
    val bWinsCount: Int,
    /** Delta médio — arredondado depois de tirar a média, não a média dos deltas já arredondados. */
    val averageDeltaPercent: Int,
)

/**
 * "Comparar builds", modo "Todos os jogos" — porta o `renderCompAll()`
 * (`index.html:2631`).
 *
 * Cada build mantém seu próprio hardware; só o jogo varia junto, jogo a jogo.
 * O delta aqui é de B em relação a A — o espelho do [CompareRow.deltaPercent],
 * que é de A em relação a B.
 */
fun FpsCalculator.compareAllGames(buildA: BuildState, buildB: BuildState): GameCompareSummary {
    val entries = db.games.map { game ->
        val sa = db.normalize(buildA.copy(gameId = game.id))
        val sb = db.normalize(buildB.copy(gameId = game.id))
        val a = calc(sa).avg
        val b = calc(sb).avg
        val delta = if (a != 0) jsRound((b - a).toDouble() / a * 100) else 0
        GameCompareEntry(game, a, b, delta)
    }.sortedByDescending { it.deltaPercent }

    val bWins = entries.count { it.avgB > it.avgA }
    val averageDelta = if (entries.isEmpty()) 0 else {
        jsRound(entries.sumOf { it.deltaPercent }.toDouble() / entries.size)
    }
    return GameCompareSummary(entries, bWins, averageDelta)
}
