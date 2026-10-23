package com.redover.punteggio.core

/** Regole di tennis e padel: funzioni pure che calcolano lo stato dopo un punto. */
object ScoreEngine {

    fun pointWonBy(state: MatchState, player: Player): MatchState {
        if (state.isOver) return state
        val scored = if (player == Player.US) {
            state.copy(pointsUs = state.pointsUs + 1)
        } else {
            state.copy(pointsThem = state.pointsThem + 1)
        }
        return if (state.inTieBreak) afterTieBreakPoint(scored, player) else afterGamePoint(scored, player)
    }

    /** Cambia a mano chi serve. Nel tie-break sposta la rotazione in modo che serva [player] ora. */
    fun setServer(state: MatchState, player: Player): MatchState {
        if (state.isOver || state.server == player) return state
        if (!state.inTieBreak) return state.copy(server = player)
        val first = state.tieBreakFirstServer ?: state.server
        return state.copy(server = player, tieBreakFirstServer = first.other)
    }

    /**
     * Passa a mano dal game normale al tie-break e viceversa. Sono due conteggi separati:
     * il tie-break parte da 0-0 e, tornando indietro, il game normale è esattamente com'era.
     * Il tie-break manuale non ha limite di punti: si torna al game normale con lo swipe.
     */
    fun setTieBreak(state: MatchState, on: Boolean): MatchState {
        if (state.isOver || state.inTieBreak == on) return state
        return if (on) {
            val tb = state.savedTieBreak
            val firstServer = tb?.server ?: state.server
            val next = state.copy(
                inTieBreak = true,
                isSuperTieBreak = false,
                isManualTieBreak = true,
                pointsUs = tb?.us ?: 0,
                pointsThem = tb?.them ?: 0,
                tieBreakFirstServer = firstServer,
                savedGame = SavedScore(state.pointsUs, state.pointsThem, state.server),
                savedTieBreak = null,
            )
            next.copy(server = tieBreakServer(next))
        } else {
            val game = state.savedGame
            state.copy(
                inTieBreak = false,
                isSuperTieBreak = false,
                isManualTieBreak = false,
                pointsUs = game?.us ?: 0,
                pointsThem = game?.them ?: 0,
                server = game?.server ?: state.server,
                tieBreakFirstServer = null,
                savedTieBreak = SavedScore(state.pointsUs, state.pointsThem, state.tieBreakFirstServer ?: state.server),
                savedGame = null,
            )
        }
    }

    private fun afterGamePoint(state: MatchState, player: Player): MatchState {
        val mine = state.points(player)
        val theirs = state.points(player.other)
        val gameWon = if (state.config.goldenPoint) {
            mine >= 4
        } else {
            mine >= 4 && mine - theirs >= 2
        }
        return if (gameWon) gameWonBy(state, player) else state
    }

    private fun afterTieBreakPoint(state: MatchState, player: Player): MatchState {
        val target = if (state.isSuperTieBreak) state.config.superTieBreakPoints else state.config.tieBreakPoints
        val mine = state.points(player)
        val theirs = state.points(player.other)
        // Il tie-break aperto a mano è un contatore libero: non finisce mai da solo.
        if (!state.isManualTieBreak && mine >= target && mine - theirs >= 2) {
            return tieBreakWonBy(state, player)
        }
        return state.copy(server = tieBreakServer(state))
    }

    /** Nel tie-break chi inizia serve un punto, poi si alterna ogni due punti. */
    private fun tieBreakServer(state: MatchState): Player {
        val first = state.tieBreakFirstServer ?: state.server
        val played = state.pointsUs + state.pointsThem
        if (played == 0) return first
        return if (((played - 1) / 2) % 2 == 0) first.other else first
    }

    private fun gameWonBy(state: MatchState, player: Player): MatchState {
        val gamesUs = state.gamesUs + if (player == Player.US) 1 else 0
        val gamesThem = state.gamesThem + if (player == Player.THEM) 1 else 0
        val next = state.copy(
            gamesUs = gamesUs,
            gamesThem = gamesThem,
            pointsUs = 0,
            pointsThem = 0,
            server = state.server.other,
            savedTieBreak = null,
        )
        val target = state.config.gamesPerSet
        val mine = next.games(player)
        val theirs = next.games(player.other)
        return when {
            mine >= target && mine - theirs >= 2 -> setWonBy(next, player, tieBreak = null)
            gamesUs == target && gamesThem == target -> next.copy(
                inTieBreak = true,
                tieBreakFirstServer = next.server,
            )
            else -> next
        }
    }

    private fun tieBreakWonBy(state: MatchState, player: Player): MatchState {
        val tieBreak = state.pointsUs to state.pointsThem
        val firstServer = state.tieBreakFirstServer ?: state.server
        val withGame = if (state.isSuperTieBreak) {
            // Il super tie-break vale come set vinto 1-0.
            state.copy(
                gamesUs = if (player == Player.US) 1 else 0,
                gamesThem = if (player == Player.THEM) 1 else 0,
            )
        } else {
            state.copy(
                gamesUs = state.gamesUs + if (player == Player.US) 1 else 0,
                gamesThem = state.gamesThem + if (player == Player.THEM) 1 else 0,
            )
        }
        // Dopo il tie-break serve chi ha ricevuto il primo punto.
        return setWonBy(withGame.copy(server = firstServer.other), player, tieBreak)
    }

    private fun setWonBy(state: MatchState, player: Player, tieBreak: Pair<Int, Int>?): MatchState {
        val sets = state.completedSets + SetScore(
            us = state.gamesUs,
            them = state.gamesThem,
            tieBreakUs = tieBreak?.first,
            tieBreakThem = tieBreak?.second,
        )
        val base = state.copy(
            completedSets = sets,
            gamesUs = 0,
            gamesThem = 0,
            pointsUs = 0,
            pointsThem = 0,
            inTieBreak = false,
            isSuperTieBreak = false,
            tieBreakFirstServer = null,
            savedGame = null,
            savedTieBreak = null,
        )
        val toWin = state.config.setsToWin
        val wonUs = sets.count { it.winner == Player.US }
        val wonThem = sets.count { it.winner == Player.THEM }
        if (wonUs == toWin || wonThem == toWin) {
            return base.copy(winner = player)
        }
        val decidingSet = wonUs == toWin - 1 && wonThem == toWin - 1
        if (decidingSet && state.config.superTieBreakFinalSet) {
            return base.copy(
                inTieBreak = true,
                isSuperTieBreak = true,
                tieBreakFirstServer = base.server,
            )
        }
        return base
    }
}
