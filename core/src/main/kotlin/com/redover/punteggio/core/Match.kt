package com.redover.punteggio.core

/**
 * Partita con cronologia, per poter annullare i punti. Immutabile.
 * [pointLog] contiene chi ha vinto ogni punto: basta salvare config e log per ricostruire la partita.
 */
data class Match(
    val current: MatchState,
    private val history: List<MatchState> = emptyList(),
    val pointLog: List<Player> = emptyList(),
) {
    val config: MatchConfig get() = current.config

    val canUndo: Boolean get() = history.isNotEmpty()

    fun point(player: Player): Match {
        if (current.isOver) return this
        return Match(ScoreEngine.pointWonBy(current, player), history + current, pointLog + player)
    }

    fun undo(): Match {
        if (history.isEmpty()) return this
        return Match(history.last(), history.dropLast(1), pointLog.dropLast(1))
    }

    companion object {
        fun start(config: MatchConfig) = Match(MatchState.start(config))

        fun replay(config: MatchConfig, log: List<Player>): Match =
            log.fold(start(config)) { m, p -> m.point(p) }
    }
}
