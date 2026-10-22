package com.redover.punteggio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
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
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.redover.punteggio.core.Match
import com.redover.punteggio.core.MatchState
import com.redover.punteggio.core.Player
import kotlin.math.abs

private val UsColor = Color(0xFF1B5E20)
private val ThemColor = Color(0xFF0D47A1)
private val BallColor = Color(0xFFC6E832)

/**
 * Metà alta = avversari ("Loro"), metà bassa = noi.
 * In ogni metà: tocco a destra = +1 punto, tocco a sinistra = −1.
 * Scorrere verso l'alto dà il servizio a loro, verso il basso a noi.
 * Scorrere a destra trasforma il game in tie-break, a sinistra torna al punteggio normale.
 * Il pulsante al centro azzera la partita, dopo una conferma.
 */
@Composable
fun ScoreScreen(
    match: Match,
    ambient: Boolean = false,
    onAddPoint: (Player) -> Unit,
    onRemovePoint: (Player) -> Unit,
    onSetServer: (Player) -> Unit,
    onSetTieBreak: (Boolean) -> Unit,
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
    val setTieBreak: (Boolean) -> Unit = {
        if (state.inTieBreak != it) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onSetTieBreak(it)
    }
    val onSwipe: (Swipe?) -> Unit = {
        when (it) {
            Swipe.UP -> setServer(Player.THEM)
            Swipe.DOWN -> setServer(Player.US)
            Swipe.RIGHT -> setTieBreak(true)
            Swipe.LEFT -> setTieBreak(false)
            null -> Unit
        }
    }
    // Il gesto vive più a lungo di una singola composizione: usa sempre lo stato aggiornato.
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var dragged = Offset.Zero
                    detectDragGestures(
                        onDragStart = { dragged = Offset.Zero },
                        onDrag = { change, amount ->
                            change.consume()
                            dragged += amount
                        },
                        onDragEnd = { currentOnSwipe(swipeDirection(dragged, swipeThreshold)) },
                    )
                },
        ) {
            PlayerHalf(
                state = state,
                player = Player.THEM,
                color = if (ambient) Color.Black else ThemColor,
                ambient = ambient,
                top = true,
                modifier = Modifier.weight(1f),
                onAdd = { add(Player.THEM) },
                onRemove = { remove(Player.THEM) },
            )
            MiddleBar(state = state, ambient = ambient, onReset = { confirmReset = true })
            PlayerHalf(
                state = state,
                player = Player.US,
                color = if (ambient) Color.Black else UsColor,
                ambient = ambient,
                top = false,
                modifier = Modifier.weight(1f),
                onAdd = { add(Player.US) },
                onRemove = { remove(Player.US) },
            )
        }

        // Ora corrente, in alto lungo il bordo dello schermo.
        TimeText()

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

private enum class Swipe { UP, DOWN, LEFT, RIGHT }

/** Direzione prevalente dello swipe, o null se il movimento è troppo corto. */
private fun swipeDirection(dragged: Offset, threshold: Float): Swipe? = when {
    abs(dragged.x) >= abs(dragged.y) && dragged.x > threshold -> Swipe.RIGHT
    abs(dragged.x) >= abs(dragged.y) && dragged.x < -threshold -> Swipe.LEFT
    abs(dragged.y) > abs(dragged.x) && dragged.y > threshold -> Swipe.DOWN
    abs(dragged.y) > abs(dragged.x) && dragged.y < -threshold -> Swipe.UP
    else -> null
}

@Composable
private fun PlayerHalf(
    state: MatchState,
    player: Player,
    color: Color,
    ambient: Boolean,
    top: Boolean,
    modifier: Modifier,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth().background(color),
        // Il punteggio sta vicino alla barra centrale, lontano dai bordi curvi e dall'ora.
        contentAlignment = if (top) Alignment.BottomCenter else Alignment.TopCenter,
    ) {
        // Due zone di tocco: sinistra toglie, destra aggiunge.
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable(enabled = !ambient, onClick = onRemove),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (!ambient) Text("−", fontSize = 22.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(start = 22.dp))
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable(enabled = !ambient, onClick = onAdd),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (!ambient) Text("+", fontSize = 22.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(end = 22.dp))
            }
        }
        // Punti, con la pallina gialla accanto a chi serve. Blu = loro, verde = noi.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = if (top) 0.dp else 6.dp, bottom = if (top) 6.dp else 0.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(if (state.server == player) BallColor else Color.Transparent, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            PointText(state, player)
            // Spazio uguale a destra, così il numero resta centrato.
            Spacer(Modifier.width(18.dp))
        }
    }
}

@Composable
private fun PointText(state: MatchState, player: Player) {
    Text(
        text = state.pointLabel(player),
        fontSize = 40.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
}

@Composable
private fun MiddleBar(state: MatchState, ambient: Boolean, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.Black)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Set vinti e game del set corrente: Loro sopra, Noi sotto.
        ScoreColumn(title = "SET", them = state.setsWon(Player.THEM), us = state.setsWon(Player.US))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // A schermo attenuato niente pulsante, solo il punteggio.
            if (!ambient) Button(
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
