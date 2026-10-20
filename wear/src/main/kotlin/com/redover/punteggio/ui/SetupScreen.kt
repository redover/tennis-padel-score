package com.redover.punteggio.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import com.redover.punteggio.core.MatchConfig
import com.redover.punteggio.core.Player
import com.redover.punteggio.core.Sport

@Composable
fun SetupScreen(initial: MatchConfig, onStart: (MatchConfig) -> Unit) {
    var sport by rememberSaveable { mutableStateOf(initial.sport) }
    var setsToWin by rememberSaveable { mutableStateOf(initial.setsToWin) }
    var goldenPoint by rememberSaveable { mutableStateOf(initial.goldenPoint) }
    var superTieBreak by rememberSaveable { mutableStateOf(initial.superTieBreakFinalSet) }
    var firstServer by rememberSaveable { mutableStateOf(initial.firstServer) }
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 28.dp),
        ) {
            item {
                Text(
                    text = "Nuova partita",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        sport = if (sport == Sport.TENNIS) Sport.PADEL else Sport.TENNIS
                        // Nel padel il golden point è la regola più comune.
                        goldenPoint = sport == Sport.PADEL
                    },
                    label = { Text("Sport") },
                    secondaryLabel = { Text(if (sport == Sport.TENNIS) "Tennis" else "Padel") },
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { setsToWin = if (setsToWin >= 3) 1 else setsToWin + 1 },
                    label = { Text("Set") },
                    secondaryLabel = { Text(setsLabel(setsToWin)) },
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
            item {
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    checked = goldenPoint,
                    onCheckedChange = { goldenPoint = it },
                    label = { Text("Golden point") },
                    toggleControl = { Switch(checked = goldenPoint) },
                )
            }
            item {
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    checked = superTieBreak,
                    onCheckedChange = { superTieBreak = it },
                    label = { Text("Super tie-break") },
                    secondaryLabel = { Text("Set decisivo a 10") },
                    toggleControl = { Switch(checked = superTieBreak) },
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { firstServer = firstServer.other },
                    label = { Text("Serve per primo") },
                    secondaryLabel = { Text(if (firstServer == Player.US) "Noi" else "Loro") },
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onStart(
                            MatchConfig(
                                sport = sport,
                                setsToWin = setsToWin,
                                goldenPoint = goldenPoint,
                                superTieBreakFinalSet = superTieBreak,
                                firstServer = firstServer,
                            )
                        )
                    },
                    label = { Text("Inizia") },
                    colors = ChipDefaults.primaryChipColors(),
                )
            }
        }
    }
}

private fun setsLabel(setsToWin: Int): String = when (setsToWin) {
    1 -> "Set unico"
    2 -> "2 su 3"
    else -> "3 su 5"
}
