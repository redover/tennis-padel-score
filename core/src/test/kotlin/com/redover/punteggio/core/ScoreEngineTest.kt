package com.redover.punteggio.core

import com.redover.punteggio.core.Player.THEM
import com.redover.punteggio.core.Player.US
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreEngineTest {

    private fun Match.points(vararg players: Player): Match =
        players.fold(this) { m, p -> m.point(p) }

    private fun Match.repeat(n: Int, player: Player): Match =
        (1..n).fold(this) { m, _ -> m.point(player) }

    private fun Match.games(n: Int, player: Player): Match = repeat(n * 4, player)

    private fun labels(m: Match) = m.current.pointLabel(US) to m.current.pointLabel(THEM)

    @Test
    fun `punti base 0 15 30 40`() {
        var m = Match.start(MatchConfig.tennis())
        assertEquals("0" to "0", labels(m))
        m = m.point(US)
        assertEquals("15" to "0", labels(m))
        m = m.point(US)
        assertEquals("30" to "0", labels(m))
        m = m.point(THEM)
        assertEquals("30" to "15", labels(m))
        m = m.point(US)
        assertEquals("40" to "15", labels(m))
        m = m.point(US)
        assertEquals(1, m.current.gamesUs)
        assertEquals("0" to "0", labels(m))
    }

    @Test
    fun `parita e vantaggi nel tennis`() {
        var m = Match.start(MatchConfig.tennis()).points(US, US, US, THEM, THEM, THEM)
        assertTrue(m.current.isDeuce)
        assertEquals("40" to "40", labels(m))
        m = m.point(THEM)
        assertEquals("40" to "AD", labels(m))
        m = m.point(US)
        assertTrue(m.current.isDeuce)
        m = m.point(US).point(US)
        assertEquals(1, m.current.gamesUs)
        assertEquals(0, m.current.gamesThem)
    }

    @Test
    fun `golden point nel padel`() {
        var m = Match.start(MatchConfig.padel()).points(US, US, US, THEM, THEM, THEM)
        assertTrue(m.current.isGoldenPoint)
        m = m.point(THEM)
        assertEquals(1, m.current.gamesThem)
        assertEquals(0, m.current.pointsUs)
    }

    @Test
    fun `set vinto 6-4 e servizio alternato`() {
        var m = Match.start(MatchConfig.tennis())
        assertEquals(US, m.current.server)
        m = m.games(1, US)
        assertEquals(THEM, m.current.server)
        m = m.games(4, THEM).games(5, US)
        assertEquals(listOf(SetScore(6, 4)), m.current.completedSets)
        assertEquals(0, m.current.gamesUs)
    }

    @Test
    fun `sul 6-5 si continua fino al 7-5`() {
        val m = Match.start(MatchConfig.tennis()).games(5, US).games(5, THEM).games(1, US)
        assertEquals(6, m.current.gamesUs)
        assertTrue(m.current.completedSets.isEmpty())
        val done = m.games(1, US)
        assertEquals(listOf(SetScore(7, 5)), done.current.completedSets)
    }

    @Test
    fun `tie-break sul 6-6 con rotazione del servizio`() {
        var m = Match.start(MatchConfig.tennis()).games(5, US).games(5, THEM).games(1, US).games(1, THEM)
        assertTrue(m.current.inTieBreak)
        // 12 game giocati: serve di nuovo chi ha iniziato il set.
        val first = m.current.server
        assertEquals(US, first)
        m = m.point(US)
        assertEquals("1" to "0", labels(m))
        assertEquals(THEM, m.current.server)
        m = m.point(US)
        assertEquals(THEM, m.current.server)
        m = m.point(US)
        assertEquals(US, m.current.server)
        m = m.point(THEM).point(THEM).point(THEM).point(THEM).point(THEM)
        // 3-5
        m = m.repeat(4, US)
        // 7-5
        val set = m.current.completedSets.single()
        assertEquals(SetScore(7, 6, 7, 5), set)
        // Dopo il tie-break serve chi ha ricevuto il primo punto.
        assertEquals(THEM, m.current.server)
    }

    @Test
    fun `tie-break serve due punti di scarto`() {
        var m = Match.start(MatchConfig.tennis()).games(5, US).games(5, THEM).games(1, US).games(1, THEM)
        repeat(6) { m = m.points(US, THEM) }
        m = m.point(US)
        assertTrue(m.current.inTieBreak)
        m = m.point(US)
        assertEquals(SetScore(7, 6, 8, 6), m.current.completedSets.single())
    }

    @Test
    fun `partita due set su tre`() {
        var m = Match.start(MatchConfig.tennis()).games(6, US)
        assertNull(m.current.winner)
        m = m.games(6, THEM)
        m = m.games(6, US)
        assertEquals(US, m.current.winner)
        assertEquals(2, m.current.setsWon(US))
        val after = m.point(THEM)
        assertEquals(m, after)
    }

    @Test
    fun `super tie-break nel set decisivo`() {
        val config = MatchConfig.padel().copy(superTieBreakFinalSet = true)
        var m = Match.start(config).games(6, US).games(6, THEM)
        assertTrue(m.current.inTieBreak)
        assertTrue(m.current.isSuperTieBreak)
        m = m.repeat(9, THEM)
        assertTrue(m.current.inTieBreak)
        m = m.point(THEM)
        assertEquals(THEM, m.current.winner)
        assertEquals(SetScore(0, 1, 0, 10), m.current.completedSets.last())
    }

    @Test
    fun `annulla ripristina lo stato precedente anche dopo un set`() {
        val before = Match.start(MatchConfig.tennis()).games(5, US).points(US, US, US)
        val after = before.point(US)
        assertEquals(1, after.current.completedSets.size)
        val undone = after.undo()
        assertEquals(before.current, undone.current)
        assertTrue(undone.canUndo)
        assertFalse(Match.start(MatchConfig.tennis()).canUndo)
    }

    @Test
    fun `replay ricostruisce la partita dal log dei punti`() {
        val config = MatchConfig.padel().copy(superTieBreakFinalSet = true)
        val m = Match.start(config).games(6, US).games(3, THEM).points(US, THEM, THEM)
        val rebuilt = Match.replay(config, m.pointLog)
        assertEquals(m.current, rebuilt.current)
        assertEquals(m.undo().current, rebuilt.undo().current)
    }

    @Test
    fun `togliere un punto a chi non ha segnato per ultimo`() {
        val m = Match.start(MatchConfig.tennis()).points(US, US, THEM)
        assertEquals("30" to "15", labels(m))
        val fixed = m.removePointOf(US)
        assertEquals("15" to "15", labels(fixed))
        assertEquals(listOf(US, THEM), fixed.pointLog)
    }

    @Test
    fun `togliere il punto che ha chiuso il game riapre il game`() {
        val m = Match.start(MatchConfig.tennis()).points(US, US, US, US)
        assertEquals(1, m.current.gamesUs)
        val fixed = m.removePointOf(US)
        assertEquals(0, fixed.current.gamesUs)
        assertEquals("40" to "0", labels(fixed))
    }

    @Test
    fun `non si toglie nulla a chi non ha punti nel game`() {
        val m = Match.start(MatchConfig.tennis()).points(US, US, US, US, THEM)
        assertEquals(m, m.removePointOf(US))
        assertEquals(m, Match.start(MatchConfig.tennis()).let { it.removePointOf(US) }.let { if (it.pointLog.isEmpty()) m else it })
        val fresh = Match.start(MatchConfig.tennis())
        assertEquals(fresh, fresh.removePointOf(THEM))
    }

    @Test
    fun `partita libera non finisce mai`() {
        var m = Match.start(MatchConfig.freePlay())
        repeat(5) { m = m.games(6, US) }
        assertNull(m.current.winner)
        assertEquals(5, m.current.setsWon(US))
    }

    @Test
    fun `cambio manuale del servizio e rotazione successiva`() {
        var m = Match.start(MatchConfig.freePlay())
        assertEquals(US, m.current.server)
        m = m.serve(THEM)
        assertEquals(THEM, m.current.server)
        m = m.games(1, US)
        assertEquals(US, m.current.server)
        // Assegnare il servizio a chi serve già non cambia nulla.
        assertEquals(m, m.serve(US))
    }

    @Test
    fun `cambio servizio nel tie-break`() {
        var m = Match.start(MatchConfig.tennis()).games(5, US).games(5, THEM).games(1, US).games(1, THEM)
        assertTrue(m.current.inTieBreak)
        assertEquals(US, m.current.server)
        m = m.serve(THEM)
        assertEquals(THEM, m.current.server)
        m = m.point(US)
        assertEquals(US, m.current.server)
        m = m.point(US)
        assertEquals(US, m.current.server)
        m = m.point(US)
        assertEquals(THEM, m.current.server)
    }

    @Test
    fun `salvataggio e ripristino con cambi di servizio`() {
        val config = MatchConfig.freePlay()
        val m = Match.start(config).points(US, THEM).serve(THEM).points(US, US, US).removePointOf(THEM)
        val restored = Match.decode(config, m.encodeEvents())
        assertEquals(m.current, restored.current)
        assertEquals(m.events, restored.events)
    }

    @Test
    fun `tie-break manuale vinto chiude il set`() {
        var m = Match.start(MatchConfig.freePlay()).games(2, US).games(2, THEM)
        m = m.tieBreak(true)
        assertTrue(m.current.inTieBreak)
        m = m.point(US)
        assertEquals("1" to "0", labels(m))
        m = m.repeat(6, US)
        assertEquals(SetScore(3, 2, 7, 0), m.current.completedSets.single())
        assertFalse(m.current.inTieBreak)
    }

    @Test
    fun `tornare al punteggio normale conserva i punti`() {
        val m = Match.start(MatchConfig.freePlay()).points(US, US, THEM)
        val tb = m.tieBreak(true)
        assertEquals("2" to "1", labels(tb))
        val back = tb.tieBreak(false)
        assertEquals("30" to "15", labels(back))
        assertEquals(m.current, back.current)
        assertEquals(tb, tb.tieBreak(true))
    }

    @Test
    fun `il tie-break manuale viene salvato e ripristinato`() {
        val config = MatchConfig.freePlay()
        val m = Match.start(config).points(US, THEM).tieBreak(true).points(US, US)
        assertEquals(m.current, Match.decode(config, m.encodeEvents()).current)
    }
}
