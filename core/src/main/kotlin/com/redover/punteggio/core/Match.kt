package com.redover.punteggio.core

/** Ciò che succede durante la partita: un punto vinto, un cambio manuale del servizio o del tie-break. */
sealed interface MatchEvent {
    data class Point(val player: Player) : MatchEvent
    data class Serve(val player: Player) : MatchEvent
    data class TieBreak(val on: Boolean) : MatchEvent
}

/**
 * Partita con cronologia, per poter annullare i punti. Immutabile.
 * [events] contiene tutto quello che è successo: basta salvare config ed eventi per ricostruire la partita.
 */
data class Match(
    val current: MatchState,
    private val history: List<MatchState> = emptyList(),
    val events: List<MatchEvent> = emptyList(),
) {
    val config: MatchConfig get() = current.config

    val canUndo: Boolean get() = history.isNotEmpty()

    /** Chi ha vinto ogni punto, in ordine. */
    val pointLog: List<Player> get() = events.filterIsInstance<MatchEvent.Point>().map { it.player }

    fun point(player: Player): Match = apply(MatchEvent.Point(player))

    /** Assegna il servizio a mano; da lì in poi la rotazione riparte da [player]. */
    fun serve(player: Player): Match = apply(MatchEvent.Serve(player))

    /** Trasforma a mano il game in corso in tie-break ([on] = true) o lo riporta a game normale. */
    fun tieBreak(on: Boolean): Match = apply(MatchEvent.TieBreak(on))

    private fun apply(event: MatchEvent): Match {
        if (current.isOver) return this
        val next = when (event) {
            is MatchEvent.Point -> ScoreEngine.pointWonBy(current, event.player)
            is MatchEvent.Serve -> ScoreEngine.setServer(current, event.player)
            is MatchEvent.TieBreak -> ScoreEngine.setTieBreak(current, event.on)
        }
        if (next == current) return this
        return Match(next, history + current, events + event)
    }

    fun undo(): Match {
        if (history.isEmpty()) return this
        return Match(history.last(), history.dropLast(1), events.dropLast(1))
    }

    /**
     * Toglie a [player] l'ultimo punto che ha vinto, ricalcolando il resto della partita.
     * Funziona se ha punti nel game in corso, oppure se il game è appena finito con un suo punto
     * (in quel caso riapre il game). Altrimenti non cambia nulla.
     */
    fun removePointOf(player: Player): Match {
        val index = events.indexOfLast { it == MatchEvent.Point(player) }
        if (index < 0) return this
        val lastPoint = events.indexOfLast { it is MatchEvent.Point }
        val gameJustStarted = current.pointsUs == 0 && current.pointsThem == 0
        val canRemove = current.points(player) > 0 || (gameJustStarted && index == lastPoint)
        if (!canRemove) return this
        if (index == events.lastIndex) return undo()
        return replayEvents(config, events.filterIndexed { i, _ -> i != index })
    }

    /** Eventi in forma compatta per salvarli: U/T = punto noi/loro, u/t = servizio a noi/loro, B/b = tie-break sì/no. */
    fun encodeEvents(): String = events.joinToString("") {
        when (it) {
            is MatchEvent.Point -> if (it.player == Player.US) "U" else "T"
            is MatchEvent.Serve -> if (it.player == Player.US) "u" else "t"
            is MatchEvent.TieBreak -> if (it.on) "B" else "b"
        }
    }

    companion object {
        fun start(config: MatchConfig) = Match(MatchState.start(config))

        fun replay(config: MatchConfig, log: List<Player>): Match =
            replayEvents(config, log.map { MatchEvent.Point(it) })

        fun replayEvents(config: MatchConfig, events: List<MatchEvent>): Match =
            events.fold(start(config)) { m, e -> m.apply(e) }

        fun decode(config: MatchConfig, encoded: String): Match =
            replayEvents(config, encoded.mapNotNull {
                when (it) {
                    'U' -> MatchEvent.Point(Player.US)
                    'T' -> MatchEvent.Point(Player.THEM)
                    'u' -> MatchEvent.Serve(Player.US)
                    't' -> MatchEvent.Serve(Player.THEM)
                    'B' -> MatchEvent.TieBreak(true)
                    'b' -> MatchEvent.TieBreak(false)
                    else -> null
                }
            })
    }
}
