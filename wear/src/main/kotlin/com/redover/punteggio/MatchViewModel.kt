package com.redover.punteggio

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.redover.punteggio.core.Match
import com.redover.punteggio.core.MatchConfig
import com.redover.punteggio.core.Player
import com.redover.punteggio.core.Sport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiene la partita in corso e la salva a ogni punto, così non si perde
 * se l'orologio chiude l'app o si riavvia.
 */
class MatchViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("match", Context.MODE_PRIVATE)

    var lastConfig: MatchConfig = loadConfig() ?: MatchConfig.tennis()
        private set

    private val _match = MutableStateFlow(loadMatch())
    val match: StateFlow<Match?> = _match.asStateFlow()

    fun start(config: MatchConfig) {
        lastConfig = config
        update(Match.start(config))
    }

    fun point(player: Player) {
        _match.value?.let { update(it.point(player)) }
    }

    fun undo() {
        _match.value?.let { update(it.undo()) }
    }

    fun reset() {
        _match.value = null
        prefs.edit().remove(KEY_LOG).putBoolean(KEY_ACTIVE, false).apply()
    }

    private fun update(match: Match) {
        _match.value = match
        saveConfig(match.config)
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_LOG, match.pointLog.joinToString("") { if (it == Player.US) "U" else "T" })
            .apply()
    }

    private fun loadMatch(): Match? {
        if (!prefs.getBoolean(KEY_ACTIVE, false)) return null
        val config = loadConfig() ?: return null
        val log = prefs.getString(KEY_LOG, "").orEmpty().map { if (it == 'U') Player.US else Player.THEM }
        return Match.replay(config, log)
    }

    private fun saveConfig(config: MatchConfig) {
        prefs.edit()
            .putString(KEY_SPORT, config.sport.name)
            .putInt(KEY_SETS, config.setsToWin)
            .putBoolean(KEY_GOLDEN, config.goldenPoint)
            .putBoolean(KEY_SUPER_TB, config.superTieBreakFinalSet)
            .putString(KEY_SERVER, config.firstServer.name)
            .apply()
    }

    private fun loadConfig(): MatchConfig? {
        val sport = prefs.getString(KEY_SPORT, null) ?: return null
        return MatchConfig(
            sport = Sport.valueOf(sport),
            setsToWin = prefs.getInt(KEY_SETS, 2),
            goldenPoint = prefs.getBoolean(KEY_GOLDEN, false),
            superTieBreakFinalSet = prefs.getBoolean(KEY_SUPER_TB, false),
            firstServer = Player.valueOf(prefs.getString(KEY_SERVER, Player.US.name)!!),
        )
    }

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_LOG = "log"
        const val KEY_SPORT = "sport"
        const val KEY_SETS = "setsToWin"
        const val KEY_GOLDEN = "goldenPoint"
        const val KEY_SUPER_TB = "superTieBreak"
        const val KEY_SERVER = "firstServer"
    }
}
