package com.redover.punteggio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.redover.punteggio.core.Match
import com.redover.punteggio.core.MatchState
import com.redover.punteggio.core.Player

private val UsColor = Color(0xFF1B5E20)
private val ThemColor = Color(0xFF0D47A1)
private val BallColor = Color(0xFFC6E832)

/**
 * Metà alta = avversari ("Loro"), metà bassa = noi.
 * In ogni metà: tocco a destra = +1 punto, tocco a sinistra = −1.
 * Scorrere verso l'alto dà il servizio a loro, verso il basso a noi.
 * Il pulsante al centro azzera la partita, dopo una conferma.
 */
@Composable
fun ScoreScreen(
    match: Match,
    onAddPoint: (Player) -> Unit,
    onRemovePoint: (Player) -> Unit,
    onSetServer: (Player) -> Unit,
    onReset: () -> Unit,
) {
    val state = match.current
    val haptics = LocalHapticFeedback.current
    var confirmReset by remember { mutableStateOf(false) }

    val add: (Player) -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onAddPoint(it)
    }
    val remove: (Player) -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onRemovePoint(it)
    }

    val swipeThreshold = with(LocalDensity.current) { 40.dp.toPx() }
    val setServer: (Player) -> Unit = {
        if (state.server != it) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onSetServer(it)
    }
    // Il gesto vive più a lungo di una singola composizione: usa sempre lo stato aggiornato.
    val currentSetServer by rememberUpdatedState(setServer)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var dragged = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            dragged += amount
                        },
                        onDragEnd = {
                            when {
                                dragged < -swipeThreshold -> currentSetServer(Player.THEM)
                                dragged > swipeThreshold -> currentSetServer(Player.US)
                            }
                        },
                    )
                },
        ) {
            PlayerHalf(
                state = state,
                player = Player.THEM,
                label = "LORO",
                color = ThemColor,
                top = true,
                modifier = Modifier.weight(1f),
                onAdd = { add(Player.THEM) },
                onRemove = { remove(Player.THEM) },
            )
            MiddleBar(state = state, onReset = { confirmReset = true })
            PlayerHalf(
                state = state,
                player = Player.US,
                label = "NOI",
                color = UsColor,
                top = false,
                modifier = Modifier.weight(1f),
                onAdd = { add(Player.US) },
                onRemove = { remove(Player.US) },
            )
        }

        if (confirmReset) {
            ResetDialog(
                onConfirm = {
                    confirmReset = false
                    onReset()
                },
                onDismiss = { confirmReset = false },
            )
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
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth().background(color),
        contentAlignment = Alignment.Center,
    ) {
        // Due zone di tocco: sinistra toglie, destra aggiunge.
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable(onClick = onRemove),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("−", fontSize = 22.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(start = 22.dp))
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable(onClick = onAdd),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("+", fontSize = 22.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(end = 22.dp))
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = if (top) 18.dp else 0.dp, bottom = if (top) 0.dp else 18.dp),
        ) {
            if (!top) PointText(state, player)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.server == player) {
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
private fun MiddleBar(state: MatchState, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.Black)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Set vinti e game del set corrente: Loro sopra, Noi sotto.
        ScoreColumn(title = "SET", them = state.setsWon(Player.THEM), us = state.setsWon(Player.US))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onReset,
                modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Text("↺", fontSize = 16.sp)
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
private fun ResetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            // Blocca i tocchi verso il punteggio sotto.
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Vuoi azzerare il punteggio?",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BallColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Chip(
                onClick = onConfirm,
                label = { Text("Sì, azzera") },
                colors = ChipDefaults.primaryChipColors(),
            )
            Chip(
                onClick = onDismiss,
                label = { Text("No") },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
