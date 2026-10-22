package com.redover.punteggio.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.wear.compose.material.MaterialTheme
import com.redover.punteggio.core.Match
import com.redover.punteggio.core.MatchConfig
import com.redover.punteggio.core.Player.THEM
import com.redover.punteggio.core.Player.US
import org.junit.Rule
import org.junit.Test

/** Genera le immagini della schermata del punteggio, così si vede l'app senza orologio. */
class ScoreScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND)

    private fun snapshot(match: Match, ambient: Boolean = false) = paparazzi.snapshot {
        MaterialTheme {
            ScoreScreen(match = match, ambient = ambient, onAddPoint = {}, onRemovePoint = {}, onSetServer = {}, onSetTieBreak = {}, onReset = {})
        }
    }

    @Test
    fun inizioPartita() = snapshot(Match.start(MatchConfig.freePlay()))

    @Test
    fun partitaInCorso() {
        var m = Match.start(MatchConfig.freePlay())
        // Un set vinto, qualche game e un game in corso.
        repeat(6) { m = (1..4).fold(m) { acc, _ -> acc.point(US) } }
        repeat(3) { m = (1..4).fold(m) { acc, _ -> acc.point(THEM) } }
        listOf(US, THEM, US, US, US, US, THEM, THEM, THEM, THEM, US, US, US, US, US, THEM, US)
            .forEach { m = m.point(it) }
        snapshot(m)
    }

    @Test
    fun tieBreak() {
        var m = Match.start(MatchConfig.freePlay()).tieBreak(true)
        listOf(US, THEM, US, US, THEM).forEach { m = m.point(it) }
        snapshot(m)
    }

    @Test
    fun polsoAbbassato() {
        var m = Match.start(MatchConfig.freePlay())
        listOf(US, US, THEM).forEach { m = m.point(it) }
        snapshot(m, ambient = true)
    }

    @Test
    fun menu() = paparazzi.snapshot {
        MaterialTheme {
            MenuDialog(onReset = {}, onClose = {}, onDismiss = {})
        }
    }
}
