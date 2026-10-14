package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.BillVaultViewModel
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.localization.LocalAppStrings
import com.example.ui.localization.getStrings
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BillVaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val strings = getStrings(currentLanguage)

            CompositionLocalProvider(
                LocalAppStrings provides strings,
                LocalAppLanguage provides currentLanguage
            ) {
                MyApplicationTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Crossfade(targetState = isLoggedIn, label = "auth_crossfade") { loggedIn ->
                            if (loggedIn) {
                                HomeScreen(viewModel = viewModel)
                            } else {
                                AuthScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
