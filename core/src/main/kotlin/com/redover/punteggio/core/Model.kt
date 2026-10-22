package com.redover.punteggio.core

enum class Player {
    US, THEM;

    val other: Player get() = if (this == US) THEM else US
}

enum class Sport { TENNIS, PADEL }

/**
 * Regole della partita.
 *
 * @param setsToWin set necessari per vincere (1 = set unico, 2 = due su tre, 3 = tre su cinque).
 * @param gamesPerSet game per vincere un set (di solito 6).
 * @param goldenPoint sul 40-40 il punto successivo decide il game (niente vantaggi).
 * @param tieBreakPoints punti del tie-break normale (di solito 7).
 * @param superTieBreakFinalSet il set decisivo si gioca come super tie-break.
 * @param superTieBreakPoints punti del super tie-break (di solito 10).
 */
data class MatchConfig(
    val sport: Sport = Sport.TENNIS,
    val setsToWin: Int = 2,
    val gamesPerSet: Int = 6,
    val goldenPoint: Boolean = false,
    val tieBreakPoints: Int = 7,
    val superTieBreakFinalSet: Boolean = false,
    val superTieBreakPoints: Int = 10,
    val firstServer: Player = Player.US,
) {
    init {
        require(setsToWin >= 1) { "setsToWin deve essere almeno 1" }
        require(gamesPerSet >= 1) { "gamesPerSet deve essere almeno 1" }
    }

    companion object {
        fun tennis() = MatchConfig(sport = Sport.TENNIS)
        fun padel() = MatchConfig(sport = Sport.PADEL, goldenPoint = true)

        /** Partita libera: regole classiche (vantaggi, tie-break sul 6-6) e set senza limite. */
        fun freePlay() = MatchConfig(setsToWin = 99)
    }
}

/** Set concluso. I punti del tie-break sono valorizzati solo se il set è finito al tie-break. */
data class SetScore(
    val us: Int,
    val them: Int,
    val tieBreakUs: Int? = null,
    val tieBreakThem: Int? = null,
) {
    val winner: Player get() = if (us > them) Player.US else Player.THEM
}

/** Punti e servizio di un conteggio messo da parte (game normale o tie-break). */
data class SavedScore(val us: Int, val them: Int, val server: Player)

data class MatchState(
    val config: MatchConfig,
    val completedSets: List<SetScore> = emptyList(),
    val gamesUs: Int = 0,
    val gamesThem: Int = 0,
    /** Punti del game corrente, o del tie-break se [inTieBreak]. */
    val pointsUs: Int = 0,
    val pointsThem: Int = 0,
    val inTieBreak: Boolean = false,
    val isSuperTieBreak: Boolean = false,
    val server: Player = config.firstServer,
    /** Chi ha servito il primo punto del tie-break in corso. */
    val tieBreakFirstServer: Player? = null,
    val winner: Player? = null,
    /** Punteggio normale messo da parte mentre si guarda il tie-break, e viceversa. */
    val savedGame: SavedScore? = null,
    val savedTieBreak: SavedScore? = null,
) {
    val isOver: Boolean get() = winner != null

    fun setsWon(player: Player): Int = completedSets.count { it.winner == player }

    fun games(player: Player): Int = if (player == Player.US) gamesUs else gamesThem

    fun points(player: Player): Int = if (player == Player.US) pointsUs else pointsThem

    /** Testo del punteggio del game corrente per [player]: "0", "15", "30", "40", "AD" o i punti del tie-break. */
    fun pointLabel(player: Player): String {
        val mine = points(player)
        val theirs = points(player.other)
        if (inTieBreak) return mine.toString()
        if (mine >= 3 && theirs >= 3) {
            return when {
                mine > theirs -> "AD"
                else -> "40"
            }
        }
        return when (mine) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            else -> "40"
        }
    }

    /** Vero sul 40-40 quando vale il golden point: il prossimo punto chiude il game. */
    val isGoldenPoint: Boolean
        get() = config.goldenPoint && !inTieBreak && pointsUs == 3 && pointsThem == 3

    val isDeuce: Boolean
        get() = !config.goldenPoint && !inTieBreak && pointsUs >= 3 && pointsUs == pointsThem

    companion object {
        fun start(config: MatchConfig): MatchState {
            val initial = MatchState(config = config)
            // Con set unico e super tie-break, si parte direttamente dal super tie-break.
            return if (config.setsToWin == 1 && config.superTieBreakFinalSet) {
                initial.copy(
                    inTieBreak = true,
                    isSuperTieBreak = true,
                    tieBreakFirstServer = config.firstServer,
                )
            } else {
                initial
            }
        }
    }
}
