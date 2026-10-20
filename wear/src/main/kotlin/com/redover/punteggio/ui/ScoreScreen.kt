package com.redover.punteggio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.redover.punteggio.core.Match
import com.redover.punteggio.core.MatchState
import com.redover.punteggio.core.Player
import com.redover.punteggio.core.SetScore

private val UsColor = Color(0xFF1B5E20)
private val ThemColor = Color(0xFF0D47A1)
private val BallColor = Color(0xFFC6E832)

/**
 * Metà alta = avversari ("Loro"), metà bassa = noi.
 * Tocca una metà per assegnare il punto, usa il tasto al centro per annullare.
 */
@Composable
fun ScoreScreen(
    match: Match,
    onPoint: (Player) -> Unit,
    onUndo: () -> Unit,
    onNewMatch: () -> Unit,
) {
    val state = match.current
    val haptics = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerHalf(
                state = state,
                player = Player.THEM,
                label = "LORO",
                color = ThemColor,
                top = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPoint(Player.THEM)
                },
            )
            MiddleBar(state = state, canUndo = match.canUndo, onUndo = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onUndo()
            })
            PlayerHalf(
                state = state,
                player = Player.US,
                label = "NOI",
                color = UsColor,
                top = false,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPoint(Player.US)
                },
            )
        }

        if (state.isOver) {
            MatchOverOverlay(state = state, canUndo = match.canUndo, onUndo = onUndo, onNewMatch = onNewMatch)
        }
    }
}

@Composable
private fun PlayerHalf(
    state: MatchState,
    player: Player,
    label: String,
    color: Color,
    top: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color)
            .clickable(enabled = !state.isOver, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = if (top) 18.dp else 0.dp, bottom = if (top) 0.dp else 18.dp),
        ) {
            if (!top) PointText(state, player)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.server == player && !state.isOver) {
                    Box(Modifier.size(8.dp).background(BallColor, CircleShape))
                    Spacer(Modifier.width(4.dp))
                }
                Text(text = label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
            if (top) PointText(state, player)
        }
    }
}

@Composable
private fun PointText(state: MatchState, player: Player) {
    Text(
        text = state.pointLabel(player),
        fontSize = 46.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
}

@Composable
private fun MiddleBar(state: MatchState, canUndo: Boolean, onUndo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).background(Color.Black).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Set vinti e game del set corrente: Loro sopra, Noi sotto.
        ScoreColumn(title = "SET", them = state.setsWon(Player.THEM), us = state.setsWon(Player.US))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Text("↶", fontSize = 16.sp)
            }
            statusLabel(state)?.let {
                Text(it, fontSize = 9.sp, color = BallColor, textAlign = TextAlign.Center)
            }
        }
        ScoreColumn(title = "GAME", them = state.gamesThem, us = state.gamesUs)
    }
}

private fun statusLabel(state: MatchState): String? = when {
    state.isSuperTieBreak -> "SUPER TB"
    state.inTieBreak -> "TIE-BREAK"
    state.isGoldenPoint -> "GOLDEN"
    else -> null
}

@Composable
private fun ScoreColumn(title: String, them: Int, us: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$them", fontSize = 14.sp, color = Color.White)
        Text(title, fontSize = 8.sp, color = Color.Gray)
        Text("$us", fontSize = 14.sp, color = Color.White)
    }
}

@Composable
private fun MatchOverOverlay(state: MatchState, canUndo: Boolean, onUndo: () -> Unit, onNewMatch: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (state.winner == Player.US) "Avete vinto!" else "Hanno vinto loro",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BallColor,
            )
            Text(
                text = state.completedSets.joinToString("  ") { set -> setText(set) },
                fontSize = 13.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            Chip(
                onClick = onNewMatch,
                label = { Text("Nuova partita") },
                colors = ChipDefaults.primaryChipColors(),
            )
            if (canUndo) {
                Chip(
                    onClick = onUndo,
                    label = { Text("Annulla ultimo punto") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** "6-4", "7-6(5)" per il tie-break, "[10-8]" per il super tie-break. */
private fun setText(set: SetScore): String {
    val tbUs = set.tieBreakUs
    val tbThem = set.tieBreakThem
    return when {
        tbUs == null || tbThem == null -> "${set.us}-${set.them}"
        set.us + set.them == 1 -> "[$tbUs-$tbThem]"
        else -> "${set.us}-${set.them}(${minOf(tbUs, tbThem)})"
    }
}
