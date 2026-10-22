package com.redover.punteggio

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material.MaterialTheme
import com.redover.punteggio.ui.ScoreScreen

class MainActivity : ComponentActivity() {

    /** Vero quando l'orologio è in modalità "sempre attivo" (polso abbassato). */
    private var ambient by mutableStateOf(false)

    // Con il supporto ambient l'app resta in primo piano quando abbassi il polso,
    // invece di lasciare il posto al quadrante.
    private val ambientObserver = AmbientLifecycleObserver(
        this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                ambient = true
            }

            override fun onExitAmbient() {
                ambient = false
            }

            override fun onUpdateAmbient() = Unit
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)
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
                    ambient = ambient,
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
