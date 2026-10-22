package com.redover.punteggio

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.MaterialTheme
import com.redover.punteggio.ui.ScoreScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Durante la partita lo schermo resta acceso, così il punteggio è sempre visibile.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Se l'orologio si blocca tra un punto e l'altro, il punteggio compare sopra il blocco
        // schermo: si segna il punto senza inserire il PIN. Il resto dell'orologio resta bloccato.
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        setContent {
            MaterialTheme {
                val vm: MatchViewModel = viewModel()
                val match by vm.match.collectAsStateWithLifecycle()
                ScoreScreen(
                    match = match,
                    onAddPoint = vm::addPoint,
                    onRemovePoint = vm::removePoint,
                    onSetServer = vm::setServer,
                    onSetTieBreak = vm::setTieBreak,
                    onReset = vm::reset,
                )
            }
        }
    }
}
