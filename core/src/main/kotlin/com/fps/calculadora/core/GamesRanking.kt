package com.fps.calculadora.core

import java.text.Collator

/** Uma linha do ranking — o jogo e o resultado da build atual nele. */
data class GameRankEntry(val game: Game, val result: CalcResult)

/** Como ordenar o ranking — o `gamesSort` do `index.html` (:2823). */
enum class GamesSort { FPS, NAME }

/**
 * "Seu PC em todos os jogos" — porta o `items` do `renderGames()`
 * (`index.html:2824`).
 *
 * Mantém toda a build atual (CPU, GPU, placa-mãe, RAM, resolução, preset, RT,
 * upscaler, Frame Gen, Hz) e varia só o jogo, renormalizando cada variação —
 * um jogo sem RT, por exemplo, pode exigir cair de Ray Tracing pra Off.
 */
fun FpsCalculator.rankAllGames(state: BuildState, sort: GamesSort): List<GameRankEntry> {
    val entries = db.games.map { game ->
        val s2 = db.normalize(state.copy(gameId = game.id))
        GameRankEntry(game, calc(s2))
    }
    return when (sort) {
        GamesSort.FPS -> entries.sortedByDescending { it.result.avg }
        GamesSort.NAME -> {
            val collator = Collator.getInstance()
            entries.sortedWith(Comparator { a, b -> collator.compare(a.game.name, b.game.name) })
        }
    }
}
