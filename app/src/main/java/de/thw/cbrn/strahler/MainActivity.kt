package de.thw.cbrn.strahler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import de.thw.cbrn.strahler.ui.StrahlerApp
import de.thw.cbrn.strahler.ui.theme.StrahlerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrahlerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StrahlerApp()
                }
            }
        }
    }
}
