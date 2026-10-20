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
import com.redover.punteggio.ui.SetupScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Durante la partita lo schermo resta acceso, così il punteggio è sempre visibile.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MaterialTheme {
                val vm: MatchViewModel = viewModel()
                val match by vm.match.collectAsStateWithLifecycle()
                val current = match
                if (current == null) {
                    SetupScreen(initial = vm.lastConfig, onStart = vm::start)
                } else {
                    ScoreScreen(
                        match = current,
                        onPoint = vm::point,
                        onUndo = vm::undo,
                        onNewMatch = vm::reset,
                    )
                }
            }
        }
    }
}
