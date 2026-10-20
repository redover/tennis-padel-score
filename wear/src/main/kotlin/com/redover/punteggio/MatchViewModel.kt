package com.redover.punteggio

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.redover.punteggio.core.Match
import com.redover.punteggio.core.MatchConfig
import com.redover.punteggio.core.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiene la partita in corso e la salva a ogni punto, così non si perde
 * se l'orologio chiude l'app o torna al quadrante.
 */
class MatchViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("match", Context.MODE_PRIVATE)

    private val _match = MutableStateFlow(loadMatch())
    val match: StateFlow<Match> = _match.asStateFlow()

    fun addPoint(player: Player) = update(_match.value.point(player))

    fun removePoint(player: Player) = update(_match.value.removePointOf(player))

    fun setServer(player: Player) = update(_match.value.serve(player))

    fun reset() = update(Match.start(CONFIG))

    private fun update(match: Match) {
        _match.value = match
        prefs.edit()
            .putString(KEY_LOG, match.encodeEvents())
            .apply()
    }

    private fun loadMatch(): Match {
        return Match.decode(CONFIG, prefs.getString(KEY_LOG, "").orEmpty())
    }

    private companion object {
        val CONFIG = MatchConfig.freePlay()
        const val KEY_LOG = "log"
    }
}
